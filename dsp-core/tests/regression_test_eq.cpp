#include <cmath>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <algorithm>
#include <iomanip>

#include "../lsp_android_bridge.h"

namespace {

// Test configuration
struct TestConfig {
    int sample_rate;
    int buffer_size;
    float duration_seconds;
    std::string signal_type;
    float signal_freq;
    std::map<int, float> parameters;
};

// Test result
struct TestResult {
    bool passed;
    float max_deviation;
    int deviation_frame;
    std::string deviation_details;
    std::vector<float> android_output;
    std::vector<float> reference_output;
};

// Generate test signal
std::vector<float> generate_signal(const TestConfig& config) {
    std::size_t total_frames = static_cast<std::size_t>(config.sample_rate * config.duration_seconds);
    std::vector<float> signal(total_frames);
    
    if (config.signal_type == "sine") {
        const float two_pi_f_over_fs = 2.0f * static_cast<float>(M_PI) * config.signal_freq / static_cast<float>(config.sample_rate);
        for (std::size_t n = 0; n < total_frames; ++n) {
            signal[n] = 0.5f * std::sinf(two_pi_f_over_fs * static_cast<float>(n));
        }
    } else if (config.signal_type == "noise") {
        // White noise
        for (std::size_t n = 0; n < total_frames; ++n) {
            signal[n] = (static_cast<float>(rand()) / RAND_MAX) * 2.0f - 1.0f;
        }
    } else if (config.signal_type == "sweep") {
        // Frequency sweep from 20Hz to 20kHz
        const float start_freq = 20.0f;
        const float end_freq = 20000.0f;
        const float duration = config.duration_seconds;
        
        for (std::size_t n = 0; n < total_frames; ++n) {
            float t = static_cast<float>(n) / config.sample_rate;
            float freq = start_freq + (end_freq - start_freq) * (t / duration);
            float phase = 2.0f * static_cast<float>(M_PI) * start_freq * t + 
                          static_cast<float>(M_PI) * (end_freq - start_freq) * t * t / duration;
            signal[n] = 0.5f * std::sinf(phase);
        }
    } else {
        // Default to silence
        std::fill(signal.begin(), signal.end(), 0.0f);
    }
    
    return signal;
}

// Process signal through Android plugin
std::vector<float> process_android(const std::vector<float>& input, const TestConfig& config) {
    std::vector<float> output(input.size());
    
    // Create plugin
    lsp_android_plugin_handle plugin = lsp_android_create_parametric_eq();
    if (!plugin) {
        std::cerr << "ERROR: Failed to create parametric EQ plugin\n";
        return output;
    }
    
    // Set parameters
    for (const auto& [index, value] : config.parameters) {
        lsp_android_set_param(plugin, index, value);
    }
    
    // Process in chunks of buffer_size
    std::size_t total_frames = input.size();
    for (std::size_t offset = 0; offset < total_frames; offset += config.buffer_size) {
        std::size_t frames_to_process = std::min(static_cast<std::size_t>(config.buffer_size), total_frames - offset);
        lsp_android_process(plugin, 
                           input.data() + offset, 
                           output.data() + offset, 
                           static_cast<int32_t>(frames_to_process));
    }
    
    // Destroy plugin
    lsp_android_destroy_plugin(plugin);
    
    return output;
}

// Generate reference output (stub implementation - would be replaced with Linux reference)
std::vector<float> generate_reference(const std::vector<float>& input, const TestConfig& config) {
    std::vector<float> output(input.size());
    
    // For the stub implementation, reference is just passthrough (unity gain)
    // In real implementation, this would call the Linux LSP plugin
    float gain_linear = 1.0f; // Unity gain
    
    // If parameter 0 (Gain) is set, apply it
    auto it = config.parameters.find(0);
    if (it != config.parameters.end()) {
        // Convert dB to linear: 10^(dB/20)
        gain_linear = std::pow(10.0f, it->second / 20.0f);
    }
    
    for (std::size_t i = 0; i < input.size(); ++i) {
        output[i] = input[i] * gain_linear;
    }
    
    return output;
}

// Compare outputs with tolerance
TestResult compare_outputs(const std::vector<float>& android_output,
                          const std::vector<float>& reference_output,
                          const TestConfig& config) {
    TestResult result;
    result.passed = true;
    result.max_deviation = 0.0f;
    result.deviation_frame = -1;
    
    if (android_output.size() != reference_output.size()) {
        result.passed = false;
        result.deviation_details = "Output size mismatch: Android=" + 
                                  std::to_string(android_output.size()) + 
                                  ", Reference=" + std::to_string(reference_output.size());
        return result;
    }
    
    // Tolerance thresholds
    const float absolute_tolerance = 1e-6f;  // For near-zero values
    const float relative_tolerance = 1e-4f;  // 0.01% relative error
    
    for (std::size_t i = 0; i < android_output.size(); ++i) {
        float android_val = android_output[i];
        float reference_val = reference_output[i];
        float diff = std::abs(android_val - reference_val);
        
        // Calculate relative error (avoid division by zero)
        float rel_error = 0.0f;
        if (std::abs(reference_val) > absolute_tolerance) {
            rel_error = diff / std::abs(reference_val);
        }
        
        // Check if error exceeds tolerance
        if (diff > absolute_tolerance && rel_error > relative_tolerance) {
            if (diff > result.max_deviation) {
                result.max_deviation = diff;
                result.deviation_frame = static_cast<int>(i);
            }
            result.passed = false;
        }
    }
    
    if (!result.passed) {
        result.deviation_details = "Max deviation: " + std::to_string(result.max_deviation) +
                                  " at frame " + std::to_string(result.deviation_frame);
    }
    
    result.android_output = android_output;
    result.reference_output = reference_output;
    
    return result;
}

// Run a single test configuration
TestResult run_test(const TestConfig& config) {
    std::cout << "Running test: " << config.sample_rate << "Hz, buffer=" 
              << config.buffer_size << ", signal=" << config.signal_type;
    if (config.signal_type == "sine") {
        std::cout << "(" << config.signal_freq << "Hz)";
    }
    if (!config.parameters.empty()) {
        std::cout << ", params:";
        for (const auto& [index, value] : config.parameters) {
            std::cout << " " << index << "=" << value;
        }
    }
    std::cout << "\n";
    
    // Generate test signal
    std::vector<float> input = generate_signal(config);
    
    // Process through Android plugin
    std::vector<float> android_output = process_android(input, config);
    
    // Generate reference output
    std::vector<float> reference_output = generate_reference(input, config);
    
    // Compare results
    return compare_outputs(android_output, reference_output, config);
}

// Print test result
void print_result(const TestResult& result, int test_num) {
    std::cout << "Test " << test_num << ": ";
    if (result.passed) {
        std::cout << "PASS";
    } else {
        std::cout << "FAIL - " << result.deviation_details;
    }
    std::cout << "\n";
    
    if (!result.passed && result.deviation_frame >= 0) {
        std::size_t frame = static_cast<std::size_t>(result.deviation_frame);
        std::cout << "  Frame " << frame << ": Android=" << std::setprecision(8) 
                  << result.android_output[frame] << ", Reference=" 
                  << result.reference_output[frame] << ", Diff=" 
                  << std::abs(result.android_output[frame] - result.reference_output[frame]) << "\n";
    }
}

} // namespace

int main() {
    std::cout << "========================================\n";
    std::cout << "Parametric EQ Plugin DSP Fidelity Test\n";
    std::cout << "========================================\n\n";
    
    // Test configurations based on requirements
    std::vector<TestConfig> test_configs;
    
    // Test different sample rates (Requirement 13.4)
    std::vector<int> sample_rates = {44100, 48000, 96000};
    
    // Test different buffer sizes (Requirement 13.5)
    std::vector<int> buffer_sizes = {64, 128, 256, 512};
    
    // Test different signal types
    std::vector<std::pair<std::string, float>> signal_types = {
        {"sine", 1000.0f},
        {"sine", 100.0f},
        {"sine", 10000.0f},
        {"noise", 0.0f},
        {"sweep", 0.0f}
    };
    
    // Test different parameter settings
    std::vector<std::map<int, float>> parameter_sets = {
        {},  // Default parameters
        {{0, 6.0f}},   // +6dB gain
        {{0, -12.0f}}, // -12dB gain
        {{0, 24.0f}},  // +24dB gain (max)
        {{0, -24.0f}}  // -24dB gain (min)
    };
    
    // Generate test configurations
    for (int sr : sample_rates) {
        for (int bs : buffer_sizes) {
            for (const auto& signal : signal_types) {
                for (const auto& params : parameter_sets) {
                    TestConfig config;
                    config.sample_rate = sr;
                    config.buffer_size = bs;
                    config.duration_seconds = 0.1f; // Short duration for quick tests
                    config.signal_type = signal.first;
                    config.signal_freq = signal.second;
                    config.parameters = params;
                    
                    // Skip some combinations to keep test count reasonable
                    if (bs > 256 && sr == 96000) continue; // Very large buffers at high SR
                    if (signal.first == "sweep" && sr < 48000) continue; // Sweep needs higher SR
                    
                    test_configs.push_back(config);
                }
            }
        }
    }
    
    // Run all tests
    std::vector<TestResult> results;
    int test_num = 1;
    int passed_count = 0;
    int failed_count = 0;
    
    for (const auto& config : test_configs) {
        TestResult result = run_test(config);
        results.push_back(result);
        print_result(result, test_num);
        
        if (result.passed) {
            passed_count++;
        } else {
            failed_count++;
        }
        
        test_num++;
    }
    
    // Summary report
    std::cout << "\n========================================\n";
    std::cout << "DSP Fidelity Test Summary\n";
    std::cout << "========================================\n";
    std::cout << "Total tests: " << test_configs.size() << "\n";
    std::cout << "Passed: " << passed_count << "\n";
    std::cout << "Failed: " << failed_count << "\n";
    std::cout << "Pass rate: " << std::fixed << std::setprecision(1) 
              << (100.0f * passed_count / test_configs.size()) << "%\n\n";
    
    // Detailed failure analysis
    if (failed_count > 0) {
        std::cout << "Failure Analysis:\n";
        std::cout << "-----------------\n";
        
        // Group failures by type
        std::map<std::string, int> failure_types;
        for (const auto& result : results) {
            if (!result.passed) {
                std::string type = "General deviation";
                if (result.max_deviation > 0.1f) {
                    type = "Large deviation (>0.1)";
                } else if (result.max_deviation > 0.01f) {
                    type = "Moderate deviation (>0.01)";
                } else {
                    type = "Small deviation (<0.01)";
                }
                failure_types[type]++;
            }
        }
        
        for (const auto& [type, count] : failure_types) {
            std::cout << "  " << type << ": " << count << " tests\n";
        }
    }
    
    // Overall status
    std::cout << "\nOverall DSP Fidelity Status: ";
    if (failed_count == 0) {
        std::cout << "PASS - All tests within tolerance\n";
        return 0;
    } else {
        std::cout << "FAIL - " << failed_count << " tests exceeded tolerance\n";
        
        // Check if failures are expected (stub implementation)
        std::cout << "\nNote: Current implementation is a stub (passthrough).\n";
        std::cout << "Failures are expected when testing non-zero gain parameters.\n";
        std::cout << "When real LSP plugin is integrated, tests should pass.\n";
        return 1;
    }
}