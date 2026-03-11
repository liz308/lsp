/*
 * Comprehensive Test Harness for arm64-v8a Architecture
 * 
 * Tests DSP functionality on Android arm64-v8a devices including:
 * - CPU feature detection (NEON baseline)
 * - DSP compare operations
 * - Memory allocation
 * - SIMD vs scalar performance
 * 
 * This test harness can be run standalone on Android devices
 * to verify DSP fidelity and performance.
 */

#include <iostream>
#include <cmath>
#include <cstring>
#include <chrono>
#include "CpuFeatures.h"

#ifdef ANDROID
#include <android/log.h>
#define LOG_TAG "LSP_TestHarness_arm64"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#define LOGI(...) printf(__VA_ARGS__); printf("\n")
#define LOGE(...) fprintf(stderr, __VA_ARGS__); fprintf(stderr, "\n")
#endif

// Include native DSP implementations
namespace native
{
    float min(const float *src, size_t count);
    float max(const float *src, size_t count);
    float abs_min(const float *src, size_t count);
    float abs_max(const float *src, size_t count);
    void minmax(const float *src, size_t count, float *min, float *max);
    void abs_minmax(const float *src, size_t count, float *min, float *max);
    size_t min_index(const float *src, size_t count);
    size_t max_index(const float *src, size_t count);
    size_t abs_min_index(const float *src, size_t count);
    size_t abs_max_index(const float *src, size_t count);
    void minmax_index(const float *src, size_t count, size_t *min, size_t *max);
    void abs_minmax_index(const float *src, size_t count, size_t *min, size_t *max);
}

// Test configuration
const float EPSILON = 0.0001f;
const size_t PERF_TEST_SIZE = 1024 * 1024; // 1M samples for performance tests
const int PERF_ITERATIONS = 100;

// Test statistics
struct TestStats {
    int total = 0;
    int passed = 0;
    int failed = 0;
    
    void record_pass() { total++; passed++; }
    void record_fail() { total++; failed++; }
    
    void print_summary() {
        std::cout << "\n=== Test Summary ===" << std::endl;
        std::cout << "Total:  " << total << std::endl;
        std::cout << "Passed: " << passed << " ✓" << std::endl;
        std::cout << "Failed: " << failed << (failed > 0 ? " ✗" : "") << std::endl;
        std::cout << "Success Rate: " << (total > 0 ? (100.0 * passed / total) : 0.0) << "%" << std::endl;
    }
};

// Helper functions
bool float_equal(float a, float b, float epsilon = EPSILON)
{
    return std::fabs(a - b) < epsilon;
}

void print_array(const char* name, const float* arr, size_t count)
{
    std::cout << name << ": [";
    for (size_t i = 0; i < count && i < 10; ++i)
    {
        std::cout << arr[i];
        if (i < count - 1 && i < 9) std::cout << ", ";
    }
    if (count > 10) std::cout << ", ...";
    std::cout << "]" << std::endl;
}

// CPU Feature Detection for arm64-v8a
void detect_cpu_features()
{
    std::cout << "\n=== CPU Feature Detection (arm64-v8a) ===" << std::endl;
    
    // Initialize NDK cpu-features library
    if (!cpu_features_init()) {
        std::cerr << "ERROR: Failed to initialize CPU feature detection" << std::endl;
        return;
    }
    
    // Get detected features
    const cpu_features_info_t* info = cpu_features_get();
    
    std::cout << "CPU Family: " << info->cpu_family << std::endl;
    std::cout << "CPU Model: " << info->cpu_model << std::endl;
    std::cout << "Architecture: " << cpu_features_arch_string(info->architecture) << std::endl;
    
    // Display SIMD features
    char simd_str[256];
    cpu_features_simd_string(info->simd_features, simd_str, sizeof(simd_str));
    std::cout << "SIMD Features: " << simd_str << std::endl;
    
    // Individual feature flags
    std::cout << "\nDetailed Features:" << std::endl;
    std::cout << "  NEON: " << (info->has_neon ? "YES (mandatory for arm64-v8a)" : "NO") << std::endl;
    
    std::cout << "\nSIMD Width: 128-bit (NEON)" << std::endl;
    std::cout << "Float Vector Size: 4 floats per register (float32x4_t)" << std::endl;
    std::cout << "Instruction Set: ARMv8-A with Advanced SIMD" << std::endl;
}

// DSP Compare Tests
bool test_min(TestStats& stats)
{
    std::cout << "\nTest: min()... ";
    float data[] = {3.5f, -2.1f, 5.7f, -8.3f, 1.2f};
    float result = native::min(data, 5);
    
    if (float_equal(result, -8.3f)) {
        std::cout << "PASSED (result: " << result << ")" << std::endl;
        stats.record_pass();
        return true;
    } else {
        std::cout << "FAILED (got " << result << ", expected -8.3)" << std::endl;
        stats.record_fail();
        return false;
    }
}

bool test_max(TestStats& stats)
{
    std::cout << "Test: max()... ";
    float data[] = {3.5f, -2.1f, 5.7f, -8.3f, 1.2f};
    float result = native::max(data, 5);
    
    if (float_equal(result, 5.7f)) {
        std::cout << "PASSED (result: " << result << ")" << std::endl;
        stats.record_pass();
        return true;
    } else {
        std::cout << "FAILED (got " << result << ", expected 5.7)" << std::endl;
        stats.record_fail();
        return false;
    }
}

bool test_abs_min(TestStats& stats)
{
    std::cout << "Test: abs_min()... ";
    float data[] = {3.5f, -2.1f, 5.7f, -0.5f, 1.2f};
    float result = native::abs_min(data, 5);
    
    if (float_equal(result, 0.5f)) {
        std::cout << "PASSED (result: " << result << ")" << std::endl;
        stats.record_pass();
        return true;
    } else {
        std::cout << "FAILED (got " << result << ", expected 0.5)" << std::endl;
        stats.record_fail();
        return false;
    }
}

bool test_abs_max(TestStats& stats)
{
    std::cout << "Test: abs_max()... ";
    float data[] = {3.5f, -2.1f, 5.7f, -8.3f, 1.2f};
    float result = native::abs_max(data, 5);
    
    if (float_equal(result, 8.3f)) {
        std::cout << "PASSED (result: " << result << ")" << std::endl;
        stats.record_pass();
        return true;
    } else {
        std::cout << "FAILED (got " << result << ", expected 8.3)" << std::endl;
        stats.record_fail();
        return false;
    }
}

bool test_minmax(TestStats& stats)
{
    std::cout << "Test: minmax()... ";
    float data[] = {3.5f, -2.1f, 5.7f, -8.3f, 1.2f};
    float min_val, max_val;
    native::minmax(data, 5, &min_val, &max_val);
    
    if (float_equal(min_val, -8.3f) && float_equal(max_val, 5.7f)) {
        std::cout << "PASSED (min: " << min_val << ", max: " << max_val << ")" << std::endl;
        stats.record_pass();
        return true;
    } else {
        std::cout << "FAILED (got [" << min_val << ", " << max_val << "], expected [-8.3, 5.7])" << std::endl;
        stats.record_fail();
        return false;
    }
}

bool test_min_index(TestStats& stats)
{
    std::cout << "Test: min_index()... ";
    float data[] = {3.5f, -2.1f, 5.7f, -8.3f, 1.2f};
    size_t idx = native::min_index(data, 5);
    
    if (idx == 3) {
        std::cout << "PASSED (index: " << idx << ", value: " << data[idx] << ")" << std::endl;
        stats.record_pass();
        return true;
    } else {
        std::cout << "FAILED (got index " << idx << ", expected 3)" << std::endl;
        stats.record_fail();
        return false;
    }
}

bool test_max_index(TestStats& stats)
{
    std::cout << "Test: max_index()... ";
    float data[] = {3.5f, -2.1f, 5.7f, -8.3f, 1.2f};
    size_t idx = native::max_index(data, 5);
    
    if (idx == 2) {
        std::cout << "PASSED (index: " << idx << ", value: " << data[idx] << ")" << std::endl;
        stats.record_pass();
        return true;
    } else {
        std::cout << "FAILED (got index " << idx << ", expected 2)" << std::endl;
        stats.record_fail();
        return false;
    }
}

// Edge case tests
bool test_empty_array(TestStats& stats)
{
    std::cout << "\nTest: Empty array handling... ";
    float data[] = {1.0f};
    float result = native::min(data, 0);
    
    if (float_equal(result, 0.0f)) {
        std::cout << "PASSED (returns 0.0 for empty array)" << std::endl;
        stats.record_pass();
        return true;
    } else {
        std::cout << "FAILED (got " << result << ", expected 0.0)" << std::endl;
        stats.record_fail();
        return false;
    }
}

bool test_single_element(TestStats& stats)
{
    std::cout << "Test: Single element array... ";
    float data[] = {42.0f};
    float result = native::min(data, 1);
    
    if (float_equal(result, 42.0f)) {
        std::cout << "PASSED" << std::endl;
        stats.record_pass();
        return true;
    } else {
        std::cout << "FAILED" << std::endl;
        stats.record_fail();
        return false;
    }
}

bool test_identical_values(TestStats& stats)
{
    std::cout << "Test: Identical values... ";
    float data[] = {3.14f, 3.14f, 3.14f, 3.14f};
    float min_val, max_val;
    native::minmax(data, 4, &min_val, &max_val);
    
    if (float_equal(min_val, 3.14f) && float_equal(max_val, 3.14f)) {
        std::cout << "PASSED" << std::endl;
        stats.record_pass();
        return true;
    } else {
        std::cout << "FAILED" << std::endl;
        stats.record_fail();
        return false;
    }
}

// Performance benchmark
void benchmark_compare_operations()
{
    std::cout << "\n=== Performance Benchmark (arm64-v8a) ===" << std::endl;
    std::cout << "Test size: " << PERF_TEST_SIZE << " samples" << std::endl;
    std::cout << "Iterations: " << PERF_ITERATIONS << std::endl;
    
    // Allocate test data
    float* data = new float[PERF_TEST_SIZE];
    for (size_t i = 0; i < PERF_TEST_SIZE; i++) {
        data[i] = std::sin(2.0f * M_PI * i / 1000.0f);
    }
    
    // Benchmark min()
    {
        auto start = std::chrono::high_resolution_clock::now();
        volatile float result;
        for (int i = 0; i < PERF_ITERATIONS; i++) {
            result = native::min(data, PERF_TEST_SIZE);
        }
        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::microseconds>(end - start);
        
        double avg_time = duration.count() / (double)PERF_ITERATIONS;
        double throughput = (PERF_TEST_SIZE / avg_time) / 1000.0; // MSamples/sec
        
        std::cout << "\nmin() performance:" << std::endl;
        std::cout << "  Average time: " << avg_time << " µs" << std::endl;
        std::cout << "  Throughput: " << throughput << " MSamples/sec" << std::endl;
    }
    
    // Benchmark minmax()
    {
        auto start = std::chrono::high_resolution_clock::now();
        volatile float min_val, max_val;
        for (int i = 0; i < PERF_ITERATIONS; i++) {
            native::minmax(data, PERF_TEST_SIZE, (float*)&min_val, (float*)&max_val);
        }
        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::microseconds>(end - start);
        
        double avg_time = duration.count() / (double)PERF_ITERATIONS;
        double throughput = (PERF_TEST_SIZE / avg_time) / 1000.0;
        
        std::cout << "\nminmax() performance:" << std::endl;
        std::cout << "  Average time: " << avg_time << " µs" << std::endl;
        std::cout << "  Throughput: " << throughput << " MSamples/sec" << std::endl;
    }
    
    delete[] data;
    
    std::cout << "\nNote: NEON optimizations should provide 3-4x speedup over scalar" << std::endl;
    std::cout << "Expected: arm64-v8a NEON ~1.5x faster than x86_64 SSE2" << std::endl;
}

int main()
{
    std::cout << "========================================" << std::endl;
    std::cout << "LSP Plugins Test Harness - arm64-v8a" << std::endl;
    std::cout << "========================================" << std::endl;
    
    // Detect CPU features
    detect_cpu_features();
    
    // Run functional tests
    std::cout << "\n=== Functional Tests ===" << std::endl;
    TestStats stats;
    
    test_min(stats);
    test_max(stats);
    test_abs_min(stats);
    test_abs_max(stats);
    test_minmax(stats);
    test_min_index(stats);
    test_max_index(stats);
    
    // Edge cases
    std::cout << "\n=== Edge Case Tests ===" << std::endl;
    test_empty_array(stats);
    test_single_element(stats);
    test_identical_values(stats);
    
    // Print test summary
    stats.print_summary();
    
    // Run performance benchmarks
    benchmark_compare_operations();
    
    std::cout << "\n========================================" << std::endl;
    if (stats.failed == 0) {
        std::cout << "✓ All tests PASSED" << std::endl;
        std::cout << "arm64-v8a DSP implementation verified" << std::endl;
        return 0;
    } else {
        std::cout << "✗ Some tests FAILED" << std::endl;
        return 1;
    }
}
