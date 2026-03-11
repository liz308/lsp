#include <cmath>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>
#include <map>

#include "../lsp_android_bridge.h"

namespace {

// Simple 16-bit PCM WAV writer for test purposes.
void write_wav_mono_16bit(
        const char*        path,
        const float*       samples,
        std::size_t        num_samples,
        int                sample_rate) {
    const uint32_t byte_rate   = static_cast<uint32_t>(sample_rate * 2); // mono, 16-bit
    const uint16_t block_align = 2;
    const uint16_t bits_per_sample = 16;
    const uint32_t data_size  = static_cast<uint32_t>(num_samples * 2);
    const uint32_t riff_size  = 36 + data_size;

    std::ofstream out(path, std::ios::binary);
    if (!out) {
        std::cerr << "Failed to open WAV file for writing: " << path << "\n";
        return;
    }

    // RIFF header
    out.write("RIFF", 4);
    out.write(reinterpret_cast<const char*>(&riff_size), 4);
    out.write("WAVE", 4);

    // fmt chunk
    out.write("fmt ", 4);
    uint32_t fmt_size = 16;
    uint16_t audio_format = 1; // PCM
    uint16_t num_channels = 1;
    out.write(reinterpret_cast<const char*>(&fmt_size), 4);
    out.write(reinterpret_cast<const char*>(&audio_format), 2);
    out.write(reinterpret_cast<const char*>(&num_channels), 2);
    out.write(reinterpret_cast<const char*>(&sample_rate), 4);
    out.write(reinterpret_cast<const char*>(&byte_rate), 4);
    out.write(reinterpret_cast<const char*>(&block_align), 2);
    out.write(reinterpret_cast<const char*>(&bits_per_sample), 2);

    // data chunk
    out.write("data", 4);
    out.write(reinterpret_cast<const char*>(&data_size), 4);

    for (std::size_t i = 0; i < num_samples; ++i) {
        float s = samples[i];
        if (s > 1.0f) s = 1.0f;
        if (s < -1.0f) s = -1.0f;
        int16_t v = static_cast<int16_t>(std::lrintf(s * 32767.0f));
        out.write(reinterpret_cast<const char*>(&v), 2);
    }

    out.close();
}

// Generate test signals
enum class SignalType {
    SINE,
    NOISE,
    SWEEP
};

std::vector<float> generate_test_signal(
        SignalType type,
        std::size_t num_frames,
        int sample_rate,
        float freq = 1000.0f) {
    
    std::vector<float> signal(num_frames);
    
    switch (type) {
        case SignalType::SINE: {
            // Generate a sine wave
            const float two_pi_f_over_fs = 2.0f * static_cast<float>(M_PI) * freq / static_cast<float>(sample_rate);
            for (std::size_t n = 0; n < num_frames; ++n) {
                signal[n] = 0.5f * std::sinf(two_pi_f_over_fs * static_cast<float>(n));
            }
            break;
        }
        
        case SignalType::NOISE: {
            // Generate white noise
            for (std::size_t n = 0; n < num_frames; ++n) {
                signal[n] = (static_cast<float>(rand()) / RAND_MAX) * 2.0f - 1.0f;
            }
            break;
        }
        
        case SignalType::SWEEP: {
            // Generate a frequency sweep from 20Hz to 20kHz
            const float start_freq = 20.0f;
            const float end_freq = 20000.0f;
            const float duration = static_cast<float>(num_frames) / sample_rate;
            
            for (std::size_t n = 0; n < num_frames; ++n) {
                float t = static_cast<float>(n) / sample_rate;
                float freq = start_freq + (end_freq - start_freq) * (t / duration);
                float phase = 2.0f * static_cast<float>(M_PI) * start_freq * t + 
                              static_cast<float>(M_PI) * (end_freq - start_freq) * t * t / duration;
                signal[n] = 0.5f * std::sinf(phase);
            }
            break;
        }
    }
    
    return signal;
}

void print_usage(const char* program_name) {
    std::cout << "Usage: " << program_name << " [options]\n";
    std::cout << "Options:\n";
    std::cout << "  --output FILE          Output WAV file (default: test_output.wav)\n";
    std::cout << "  --duration SECONDS     Test duration in seconds (default: 2.0)\n";
    std::cout << "  --sample-rate RATE     Sample rate in Hz (default: 48000)\n";
    std::cout << "  --signal TYPE          Test signal type: sine, noise, sweep (default: sine)\n";
    std::cout << "  --freq FREQ            Frequency for sine wave in Hz (default: 1000)\n";
    std::cout << "  --param INDEX=VALUE    Set plugin parameter (e.g., --param 0=12.0)\n";
    std::cout << "  --list-params          List available plugin parameters\n";
    std::cout << "  --help                 Show this help message\n";
}

bool parse_float(const char* str, float& value) {
    char* endptr;
    value = strtof(str, &endptr);
    return endptr != str && *endptr == '\0';
}

bool parse_int(const char* str, int& value) {
    char* endptr;
    value = static_cast<int>(strtol(str, &endptr, 10));
    return endptr != str && *endptr == '\0';
}

} // namespace

int main(int argc, char* argv[]) {
    // Default configuration
    std::string output_file = "test_output.wav";
    float duration_seconds = 2.0f;
    int sample_rate = 48000;
    SignalType signal_type = SignalType::SINE;
    float sine_freq = 1000.0f;
    std::map<int, float> param_values;
    bool list_params = false;
    
    // Parse command line arguments
    for (int i = 1; i < argc; ++i) {
        if (strcmp(argv[i], "--help") == 0) {
            print_usage(argv[0]);
            return 0;
        } else if (strcmp(argv[i], "--output") == 0) {
            if (i + 1 < argc) {
                output_file = argv[++i];
            } else {
                std::cerr << "Error: --output requires a filename\n";
                return 1;
            }
        } else if (strcmp(argv[i], "--duration") == 0) {
            if (i + 1 < argc) {
                if (!parse_float(argv[++i], duration_seconds) || duration_seconds <= 0) {
                    std::cerr << "Error: Invalid duration value\n";
                    return 1;
                }
            } else {
                std::cerr << "Error: --duration requires a value\n";
                return 1;
            }
        } else if (strcmp(argv[i], "--sample-rate") == 0) {
            if (i + 1 < argc) {
                if (!parse_int(argv[++i], sample_rate) || sample_rate <= 0) {
                    std::cerr << "Error: Invalid sample rate value\n";
                    return 1;
                }
            } else {
                std::cerr << "Error: --sample-rate requires a value\n";
                return 1;
            }
        } else if (strcmp(argv[i], "--signal") == 0) {
            if (i + 1 < argc) {
                std::string type = argv[++i];
                if (type == "sine") {
                    signal_type = SignalType::SINE;
                } else if (type == "noise") {
                    signal_type = SignalType::NOISE;
                } else if (type == "sweep") {
                    signal_type = SignalType::SWEEP;
                } else {
                    std::cerr << "Error: Invalid signal type. Must be sine, noise, or sweep\n";
                    return 1;
                }
            } else {
                std::cerr << "Error: --signal requires a type\n";
                return 1;
            }
        } else if (strcmp(argv[i], "--freq") == 0) {
            if (i + 1 < argc) {
                if (!parse_float(argv[++i], sine_freq) || sine_freq <= 0) {
                    std::cerr << "Error: Invalid frequency value\n";
                    return 1;
                }
            } else {
                std::cerr << "Error: --freq requires a value\n";
                return 1;
            }
        } else if (strcmp(argv[i], "--param") == 0) {
            if (i + 1 < argc) {
                std::string param_str = argv[++i];
                size_t equals_pos = param_str.find('=');
                if (equals_pos == std::string::npos) {
                    std::cerr << "Error: Parameter must be in format INDEX=VALUE\n";
                    return 1;
                }
                
                std::string index_str = param_str.substr(0, equals_pos);
                std::string value_str = param_str.substr(equals_pos + 1);
                
                int index;
                float value;
                
                if (!parse_int(index_str.c_str(), index) || index < 0) {
                    std::cerr << "Error: Invalid parameter index\n";
                    return 1;
                }
                
                if (!parse_float(value_str.c_str(), value)) {
                    std::cerr << "Error: Invalid parameter value\n";
                    return 1;
                }
                
                param_values[index] = value;
            } else {
                std::cerr << "Error: --param requires INDEX=VALUE\n";
                return 1;
            }
        } else if (strcmp(argv[i], "--list-params") == 0) {
            list_params = true;
        } else {
            std::cerr << "Error: Unknown option: " << argv[i] << "\n";
            print_usage(argv[0]);
            return 1;
        }
    }
    
    // Instantiate the parametric EQ plugin via the bridge.
    lsp_android_plugin_handle plugin = lsp_android_create_parametric_eq();
    if (!plugin) {
        std::cerr << "Failed to create parametric EQ plugin instance.\n";
        return 1;
    }
    
    // List parameters if requested
    if (list_params) {
        int port_count = lsp_android_get_port_count(plugin);
        std::cout << "Plugin has " << port_count << " parameters:\n";
        
        std::vector<lsp_android_port_descriptor> descriptors(port_count);
        lsp_android_get_port_descriptors(plugin, descriptors.data(), port_count);
        
        for (int i = 0; i < port_count; ++i) {
            const auto& desc = descriptors[i];
            std::cout << "  [" << i << "] " << desc.name 
                      << " (" << desc.unit << "): "
                      << desc.min_value << " to " << desc.max_value
                      << " (default: " << desc.default_value << ")\n";
        }
        
        lsp_android_destroy_plugin(plugin);
        return 0;
    }
    
    // Apply parameter values
    for (const auto& [index, value] : param_values) {
        lsp_android_set_param(plugin, index, value);
        std::cout << "Set parameter " << index << " to " << value << "\n";
    }
    
    // Generate test signal
    const std::size_t num_frames = static_cast<std::size_t>(sample_rate * duration_seconds);
    std::vector<float> input = generate_test_signal(signal_type, num_frames, sample_rate, sine_freq);
    std::vector<float> output(num_frames);
    
    // Process audio
    lsp_android_process(plugin, input.data(), output.data(), static_cast<int32_t>(num_frames));
    
    // Write output to WAV file
    write_wav_mono_16bit(output_file.c_str(), output.data(), output.size(), sample_rate);
    
    std::cout << "Test completed successfully:\n";
    std::cout << "  Output file: " << output_file << "\n";
    std::cout << "  Duration: " << duration_seconds << " seconds\n";
    std::cout << "  Sample rate: " << sample_rate << " Hz\n";
    std::cout << "  Frames processed: " << num_frames << "\n";
    
    lsp_android_destroy_plugin(plugin);
    return 0;
}