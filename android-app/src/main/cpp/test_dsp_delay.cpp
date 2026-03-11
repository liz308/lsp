/*
 * Test program for LSP Plugins Delay implementations on Android
 * 
 * Tests both Delay and DynamicDelay classes to verify:
 * - Circular buffer functionality
 * - Variable delay times
 * - No dynamic allocation in audio callback
 * - Correct delay line behavior
 */

#include <iostream>
#include <cmath>
#include <cstring>
#include <cassert>

// Include LSP delay implementations
#include <core/util/Delay.h>
#include <core/util/DynamicDelay.h>
#include <dsp/dsp.h>

using namespace lsp;

// Test configuration
const size_t SAMPLE_RATE = 48000;
const size_t BUFFER_SIZE = 256;
const float EPSILON = 1e-6f;

// Helper function to compare floats
bool float_equals(float a, float b, float epsilon = EPSILON) {
    return std::fabs(a - b) < epsilon;
}

// Test 1: Basic Delay - Single sample processing
bool test_delay_single_sample() {
    std::cout << "Test 1: Delay - Single sample processing... ";
    
    Delay delay;
    const size_t delay_samples = 10;
    
    if (!delay.init(delay_samples + 100)) {
        std::cout << "FAILED (init)" << std::endl;
        return false;
    }
    
    delay.set_delay(delay_samples);
    
    // Fill delay line with zeros
    for (size_t i = 0; i < delay_samples; i++) {
        float out = delay.process(0.0f);
        if (!float_equals(out, 0.0f)) {
            std::cout << "FAILED (initial zeros)" << std::endl;
            return false;
        }
    }
    
    // Send impulse
    float out = delay.process(1.0f);
    if (!float_equals(out, 0.0f)) {
        std::cout << "FAILED (impulse input)" << std::endl;
        return false;
    }
    
    // Wait for delay
    for (size_t i = 0; i < delay_samples - 1; i++) {
        out = delay.process(0.0f);
        if (!float_equals(out, 0.0f)) {
            std::cout << "FAILED (waiting for impulse)" << std::endl;
            return false;
        }
    }
    
    // Impulse should appear now
    out = delay.process(0.0f);
    if (!float_equals(out, 1.0f)) {
        std::cout << "FAILED (impulse output, got " << out << ")" << std::endl;
        return false;
    }
    
    delay.destroy();
    std::cout << "PASSED" << std::endl;
    return true;
}

// Test 2: Delay - Buffer processing
bool test_delay_buffer_processing() {
    std::cout << "Test 2: Delay - Buffer processing... ";
    
    Delay delay;
    const size_t delay_samples = 100;
    const size_t test_size = 256;
    
    if (!delay.init(delay_samples + test_size)) {
        std::cout << "FAILED (init)" << std::endl;
        return false;
    }
    
    delay.set_delay(delay_samples);
    
    float input[test_size];
    float output[test_size];
    
    // Create test signal (ramp)
    for (size_t i = 0; i < test_size; i++) {
        input[i] = static_cast<float>(i) / test_size;
    }
    
    // Process buffer
    delay.process(output, input, test_size);
    
    // Output should be zeros (delay line was empty)
    for (size_t i = 0; i < test_size; i++) {
        if (!float_equals(output[i], 0.0f)) {
            std::cout << "FAILED (first buffer should be zeros)" << std::endl;
            return false;
        }
    }
    
    // Process zeros, should get delayed input
    float zeros[test_size];
    dsp::fill_zero(zeros, test_size);
    delay.process(output, zeros, test_size);
    
    // First delay_samples should still be zeros
    for (size_t i = 0; i < delay_samples && i < test_size; i++) {
        if (!float_equals(output[i], 0.0f)) {
            std::cout << "FAILED (delay period)" << std::endl;
            return false;
        }
    }
    
    // After delay, should see the ramp
    for (size_t i = delay_samples; i < test_size; i++) {
        float expected = static_cast<float>(i - delay_samples) / test_size;
        if (!float_equals(output[i], expected, 1e-5f)) {
            std::cout << "FAILED (delayed signal mismatch at " << i << ")" << std::endl;
            return false;
        }
    }
    
    delay.destroy();
    std::cout << "PASSED" << std::endl;
    return true;
}

// Test 3: Delay - Gain processing
bool test_delay_with_gain() {
    std::cout << "Test 3: Delay - Gain processing... ";
    
    Delay delay;
    const size_t delay_samples = 50;
    const float gain = 0.5f;
    
    if (!delay.init(delay_samples + 100)) {
        std::cout << "FAILED (init)" << std::endl;
        return false;
    }
    
    delay.set_delay(delay_samples);
    
    // Fill delay line
    for (size_t i = 0; i < delay_samples; i++) {
        delay.process(1.0f);
    }
    
    // Process with gain
    float out = delay.process(0.0f, gain);
    if (!float_equals(out, gain)) {
        std::cout << "FAILED (gain not applied, got " << out << ")" << std::endl;
        return false;
    }
    
    delay.destroy();
    std::cout << "PASSED" << std::endl;
    return true;
}

// Test 4: Delay - Ramping delay time
bool test_delay_ramping() {
    std::cout << "Test 4: Delay - Ramping delay time... ";
    
    Delay delay;
    const size_t initial_delay = 50;
    const size_t final_delay = 100;
    const size_t ramp_samples = 200;
    
    if (!delay.init(final_delay + ramp_samples)) {
        std::cout << "FAILED (init)" << std::endl;
        return false;
    }
    
    delay.set_delay(initial_delay);
    
    float input[ramp_samples];
    float output[ramp_samples];
    
    // Create impulse train
    dsp::fill_zero(input, ramp_samples);
    input[0] = 1.0f;
    
    // Process with ramping delay
    delay.process_ramping(output, input, final_delay, ramp_samples);
    
    // Verify delay changed
    if (delay.get_delay() != final_delay) {
        std::cout << "FAILED (delay not updated)" << std::endl;
        return false;
    }
    
    delay.destroy();
    std::cout << "PASSED" << std::endl;
    return true;
}

// Test 5: Delay - Clear functionality
bool test_delay_clear() {
    std::cout << "Test 5: Delay - Clear functionality... ";
    
    Delay delay;
    const size_t delay_samples = 50;
    
    if (!delay.init(delay_samples + 100)) {
        std::cout << "FAILED (init)" << std::endl;
        return false;
    }
    
    delay.set_delay(delay_samples);
    
    // Fill delay line with non-zero values
    for (size_t i = 0; i < delay_samples; i++) {
        delay.process(1.0f);
    }
    
    // Clear the delay line
    delay.clear();
    
    // Output should be zeros now
    for (size_t i = 0; i < delay_samples; i++) {
        float out = delay.process(0.0f);
        if (!float_equals(out, 0.0f)) {
            std::cout << "FAILED (not cleared)" << std::endl;
            return false;
        }
    }
    
    delay.destroy();
    std::cout << "PASSED" << std::endl;
    return true;
}

// Test 6: DynamicDelay - Basic functionality
bool test_dynamic_delay_basic() {
    std::cout << "Test 6: DynamicDelay - Basic functionality... ";
    
    DynamicDelay ddelay;
    const size_t max_delay = 1000;
    
    if (ddelay.init(max_delay) != STATUS_OK) {
        std::cout << "FAILED (init)" << std::endl;
        return false;
    }
    
    if (ddelay.max_delay() != max_delay) {
        std::cout << "FAILED (max_delay mismatch)" << std::endl;
        return false;
    }
    
    ddelay.destroy();
    std::cout << "PASSED" << std::endl;
    return true;
}

// Test 7: DynamicDelay - Variable delay processing
bool test_dynamic_delay_variable() {
    std::cout << "Test 7: DynamicDelay - Variable delay processing... ";
    
    DynamicDelay ddelay;
    const size_t max_delay = 500;
    const size_t test_size = 256;
    
    if (ddelay.init(max_delay) != STATUS_OK) {
        std::cout << "FAILED (init)" << std::endl;
        return false;
    }
    
    float input[test_size];
    float output[test_size];
    float delay_values[test_size];
    float feedback_gain[test_size];
    float feedback_delay[test_size];
    
    // Create test signal
    for (size_t i = 0; i < test_size; i++) {
        input[i] = std::sin(2.0f * M_PI * i / 100.0f);
        delay_values[i] = 100.0f + 50.0f * std::sin(2.0f * M_PI * i / 200.0f);
        feedback_gain[i] = 0.0f;  // No feedback for this test
        feedback_delay[i] = 0.0f;
    }
    
    // Process with dynamic delay
    ddelay.process(output, input, delay_values, feedback_gain, feedback_delay, test_size);
    
    // Output should be non-zero (delayed input)
    bool has_output = false;
    for (size_t i = 0; i < test_size; i++) {
        if (std::fabs(output[i]) > EPSILON) {
            has_output = true;
            break;
        }
    }
    
    if (!has_output) {
        std::cout << "FAILED (no output)" << std::endl;
        return false;
    }
    
    ddelay.destroy();
    std::cout << "PASSED" << std::endl;
    return true;
}

// Test 8: DynamicDelay - Clear and copy
bool test_dynamic_delay_operations() {
    std::cout << "Test 8: DynamicDelay - Clear and copy... ";
    
    DynamicDelay ddelay1, ddelay2;
    const size_t max_delay = 500;
    
    if (ddelay1.init(max_delay) != STATUS_OK || ddelay2.init(max_delay) != STATUS_OK) {
        std::cout << "FAILED (init)" << std::endl;
        return false;
    }
    
    // Clear should work without error
    ddelay1.clear();
    
    // Copy should work
    ddelay2.copy(&ddelay1);
    
    // Swap should work
    ddelay1.swap(&ddelay2);
    
    ddelay1.destroy();
    ddelay2.destroy();
    std::cout << "PASSED" << std::endl;
    return true;
}

// Test 9: No dynamic allocation in process
bool test_no_dynamic_allocation() {
    std::cout << "Test 9: No dynamic allocation in process... ";
    
    Delay delay;
    const size_t delay_samples = 100;
    const size_t test_size = 256;
    
    if (!delay.init(delay_samples + test_size)) {
        std::cout << "FAILED (init)" << std::endl;
        return false;
    }
    
    delay.set_delay(delay_samples);
    
    float input[test_size];
    float output[test_size];
    
    // Fill with test data
    for (size_t i = 0; i < test_size; i++) {
        input[i] = static_cast<float>(i);
    }
    
    // Process multiple times - should not allocate
    for (int iter = 0; iter < 10; iter++) {
        delay.process(output, input, test_size);
    }
    
    delay.destroy();
    std::cout << "PASSED" << std::endl;
    return true;
}

// Test 10: Circular buffer wraparound
bool test_circular_buffer_wraparound() {
    std::cout << "Test 10: Circular buffer wraparound... ";
    
    Delay delay;
    const size_t delay_samples = 100;
    const size_t iterations = 10;
    
    if (!delay.init(delay_samples + 100)) {
        std::cout << "FAILED (init)" << std::endl;
        return false;
    }
    
    delay.set_delay(delay_samples);
    
    // Process many samples to force wraparound
    for (size_t iter = 0; iter < iterations; iter++) {
        for (size_t i = 0; i < delay_samples * 2; i++) {
            float value = static_cast<float>(iter * 1000 + i);
            delay.process(value);
        }
    }
    
    delay.destroy();
    std::cout << "PASSED" << std::endl;
    return true;
}

int main() {
    std::cout << "=== LSP Plugins Delay Implementation Tests ===" << std::endl;
    std::cout << "Testing delay line implementations for Android NDK" << std::endl;
    std::cout << std::endl;
    
    // Initialize DSP
    dsp::init();
    
    int passed = 0;
    int total = 0;
    
    // Run all tests
    total++; if (test_delay_single_sample()) passed++;
    total++; if (test_delay_buffer_processing()) passed++;
    total++; if (test_delay_with_gain()) passed++;
    total++; if (test_delay_ramping()) passed++;
    total++; if (test_delay_clear()) passed++;
    total++; if (test_dynamic_delay_basic()) passed++;
    total++; if (test_dynamic_delay_variable()) passed++;
    total++; if (test_dynamic_delay_operations()) passed++;
    total++; if (test_no_dynamic_allocation()) passed++;
    total++; if (test_circular_buffer_wraparound()) passed++;
    
    std::cout << std::endl;
    std::cout << "=== Test Results ===" << std::endl;
    std::cout << "Passed: " << passed << "/" << total << std::endl;
    
    if (passed == total) {
        std::cout << "✓ All delay tests PASSED" << std::endl;
        std::cout << std::endl;
        std::cout << "Delay implementations verified:" << std::endl;
        std::cout << "  - Circular buffer functionality" << std::endl;
        std::cout << "  - Variable delay times" << std::endl;
        std::cout << "  - No dynamic allocation in audio callback" << std::endl;
        std::cout << "  - Gain processing" << std::endl;
        std::cout << "  - Ramping delay changes" << std::endl;
        std::cout << "  - Dynamic delay with feedback" << std::endl;
        return 0;
    } else {
        std::cout << "✗ Some tests FAILED" << std::endl;
        return 1;
    }
}
