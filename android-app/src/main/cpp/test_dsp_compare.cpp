/*
 * Test program for DSP compare functions
 * Tests all sample comparison operations
 */

#include <iostream>
#include <cmath>
#include <cstring>

// Include the native implementations directly
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

// Test helpers
bool float_equal(float a, float b, float epsilon = 0.0001f)
{
    return std::fabs(a - b) < epsilon;
}

void print_array(const char* name, const float* arr, size_t count)
{
    std::cout << name << ": [";
    for (size_t i = 0; i < count; ++i)
    {
        std::cout << arr[i];
        if (i < count - 1) std::cout << ", ";
    }
    std::cout << "]" << std::endl;
}

int main()
{
    std::cout << "=== DSP Compare Functions Test ===" << std::endl << std::endl;

    // Test data with mixed positive and negative values
    float test_data[] = {3.5f, -2.1f, 5.7f, -8.3f, 1.2f, -0.5f, 4.8f, -6.2f};
    size_t count = sizeof(test_data) / sizeof(test_data[0]);
    
    print_array("Test data", test_data, count);
    std::cout << std::endl;

    // Test 1: min()
    {
        float result = native::min(test_data, count);
        float expected = -8.3f;
        if (float_equal(result, expected))
            std::cout << "✓ PASS: min() = " << result << std::endl;
        else
            std::cout << "✗ FAIL: min() = " << result << ", expected " << expected << std::endl;
    }

    // Test 2: max()
    {
        float result = native::max(test_data, count);
        float expected = 5.7f;
        if (float_equal(result, expected))
            std::cout << "✓ PASS: max() = " << result << std::endl;
        else
            std::cout << "✗ FAIL: max() = " << result << ", expected " << expected << std::endl;
    }

    // Test 3: abs_min()
    {
        float result = native::abs_min(test_data, count);
        float expected = 0.5f;
        if (float_equal(result, expected))
            std::cout << "✓ PASS: abs_min() = " << result << std::endl;
        else
            std::cout << "✗ FAIL: abs_min() = " << result << ", expected " << expected << std::endl;
    }

    // Test 4: abs_max()
    {
        float result = native::abs_max(test_data, count);
        float expected = 8.3f;
        if (float_equal(result, expected))
            std::cout << "✓ PASS: abs_max() = " << result << std::endl;
        else
            std::cout << "✗ FAIL: abs_max() = " << result << ", expected " << expected << std::endl;
    }

    // Test 5: minmax()
    {
        float min_val, max_val;
        native::minmax(test_data, count, &min_val, &max_val);
        if (float_equal(min_val, -8.3f) && float_equal(max_val, 5.7f))
            std::cout << "✓ PASS: minmax() = [" << min_val << ", " << max_val << "]" << std::endl;
        else
            std::cout << "✗ FAIL: minmax() = [" << min_val << ", " << max_val << "], expected [-8.3, 5.7]" << std::endl;
    }

    // Test 6: abs_minmax()
    {
        float min_val, max_val;
        native::abs_minmax(test_data, count, &min_val, &max_val);
        if (float_equal(min_val, 0.5f) && float_equal(max_val, 8.3f))
            std::cout << "✓ PASS: abs_minmax() = [" << min_val << ", " << max_val << "]" << std::endl;
        else
            std::cout << "✗ FAIL: abs_minmax() = [" << min_val << ", " << max_val << "], expected [0.5, 8.3]" << std::endl;
    }

    // Test 7: min_index()
    {
        size_t idx = native::min_index(test_data, count);
        size_t expected = 3; // -8.3 is at index 3
        if (idx == expected)
            std::cout << "✓ PASS: min_index() = " << idx << " (value: " << test_data[idx] << ")" << std::endl;
        else
            std::cout << "✗ FAIL: min_index() = " << idx << ", expected " << expected << std::endl;
    }

    // Test 8: max_index()
    {
        size_t idx = native::max_index(test_data, count);
        size_t expected = 2; // 5.7 is at index 2
        if (idx == expected)
            std::cout << "✓ PASS: max_index() = " << idx << " (value: " << test_data[idx] << ")" << std::endl;
        else
            std::cout << "✗ FAIL: max_index() = " << idx << ", expected " << expected << std::endl;
    }

    // Test 9: abs_min_index()
    {
        size_t idx = native::abs_min_index(test_data, count);
        size_t expected = 5; // -0.5 (abs = 0.5) is at index 5
        if (idx == expected)
            std::cout << "✓ PASS: abs_min_index() = " << idx << " (value: " << test_data[idx] << ")" << std::endl;
        else
            std::cout << "✗ FAIL: abs_min_index() = " << idx << ", expected " << expected << std::endl;
    }

    // Test 10: abs_max_index()
    {
        size_t idx = native::abs_max_index(test_data, count);
        size_t expected = 3; // -8.3 (abs = 8.3) is at index 3
        if (idx == expected)
            std::cout << "✓ PASS: abs_max_index() = " << idx << " (value: " << test_data[idx] << ")" << std::endl;
        else
            std::cout << "✗ FAIL: abs_max_index() = " << idx << ", expected " << expected << std::endl;
    }

    // Test 11: minmax_index()
    {
        size_t min_idx, max_idx;
        native::minmax_index(test_data, count, &min_idx, &max_idx);
        if (min_idx == 3 && max_idx == 2)
            std::cout << "✓ PASS: minmax_index() = [" << min_idx << ", " << max_idx << "]" << std::endl;
        else
            std::cout << "✗ FAIL: minmax_index() = [" << min_idx << ", " << max_idx << "], expected [3, 2]" << std::endl;
    }

    // Test 12: abs_minmax_index()
    {
        size_t min_idx, max_idx;
        native::abs_minmax_index(test_data, count, &min_idx, &max_idx);
        if (min_idx == 5 && max_idx == 3)
            std::cout << "✓ PASS: abs_minmax_index() = [" << min_idx << ", " << max_idx << "]" << std::endl;
        else
            std::cout << "✗ FAIL: abs_minmax_index() = [" << min_idx << ", " << max_idx << "], expected [5, 3]" << std::endl;
    }

    std::cout << std::endl;

    // Edge case tests
    std::cout << "=== Edge Case Tests ===" << std::endl << std::endl;

    // Test with empty array (count = 0)
    {
        float result = native::min(test_data, 0);
        if (float_equal(result, 0.0f))
            std::cout << "✓ PASS: min() with count=0 returns 0.0" << std::endl;
        else
            std::cout << "✗ FAIL: min() with count=0 = " << result << std::endl;
    }

    // Test with single element
    {
        float single[] = {42.0f};
        float result = native::min(single, 1);
        if (float_equal(result, 42.0f))
            std::cout << "✓ PASS: min() with single element = " << result << std::endl;
        else
            std::cout << "✗ FAIL: min() with single element = " << result << std::endl;
    }

    // Test with all same values
    {
        float same[] = {3.14f, 3.14f, 3.14f, 3.14f};
        float min_val, max_val;
        native::minmax(same, 4, &min_val, &max_val);
        if (float_equal(min_val, 3.14f) && float_equal(max_val, 3.14f))
            std::cout << "✓ PASS: minmax() with identical values = [" << min_val << ", " << max_val << "]" << std::endl;
        else
            std::cout << "✗ FAIL: minmax() with identical values" << std::endl;
    }

    // Test with all negative values
    {
        float negatives[] = {-1.0f, -5.0f, -2.0f, -10.0f};
        float result = native::max(negatives, 4);
        if (float_equal(result, -1.0f))
            std::cout << "✓ PASS: max() with all negatives = " << result << std::endl;
        else
            std::cout << "✗ FAIL: max() with all negatives = " << result << std::endl;
    }

    std::cout << std::endl << "=== All Tests Complete ===" << std::endl;

    return 0;
}
