/*
 * Test program for DSP array operations (copy functions)
 * Tests the ported array operations from lsp-plugins
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <dsp/dsp.h>

#define TEST_SIZE 16
#define EPSILON 0.0001f

// Helper function to print array
void print_array(const char *name, const float *arr, size_t count)
{
    printf("%s: [", name);
    for (size_t i = 0; i < count; i++)
    {
        printf("%.2f", arr[i]);
        if (i < count - 1)
            printf(", ");
    }
    printf("]\n");
}

// Helper function to compare arrays
bool arrays_equal(const float *a, const float *b, size_t count)
{
    for (size_t i = 0; i < count; i++)
    {
        if (fabsf(a[i] - b[i]) > EPSILON)
            return false;
    }
    return true;
}

int main()
{
    printf("=== DSP Array Operations Test ===\n\n");

    // Initialize DSP
    dsp::init();
    printf("DSP initialized\n\n");

    // Test 1: copy
    printf("Test 1: copy()\n");
    {
        float src[TEST_SIZE];
        float dst[TEST_SIZE];
        
        // Initialize source
        for (size_t i = 0; i < TEST_SIZE; i++)
            src[i] = (float)i;
        
        // Clear destination
        memset(dst, 0, sizeof(dst));
        
        // Copy
        dsp::copy(dst, src, TEST_SIZE);
        
        print_array("src", src, TEST_SIZE);
        print_array("dst", dst, TEST_SIZE);
        
        if (arrays_equal(src, dst, TEST_SIZE))
            printf("✓ PASS: copy() works correctly\n\n");
        else
            printf("✗ FAIL: copy() produced incorrect results\n\n");
    }

    // Test 2: move (non-overlapping)
    printf("Test 2: move() - non-overlapping\n");
    {
        float src[TEST_SIZE];
        float dst[TEST_SIZE];
        
        for (size_t i = 0; i < TEST_SIZE; i++)
            src[i] = (float)(i * 2);
        
        memset(dst, 0, sizeof(dst));
        dsp::move(dst, src, TEST_SIZE);
        
        print_array("src", src, TEST_SIZE);
        print_array("dst", dst, TEST_SIZE);
        
        if (arrays_equal(src, dst, TEST_SIZE))
            printf("✓ PASS: move() works correctly\n\n");
        else
            printf("✗ FAIL: move() produced incorrect results\n\n");
    }

    // Test 3: fill
    printf("Test 3: fill()\n");
    {
        float dst[TEST_SIZE];
        float value = 3.14f;
        
        dsp::fill(dst, value, TEST_SIZE);
        
        print_array("dst", dst, TEST_SIZE);
        
        bool pass = true;
        for (size_t i = 0; i < TEST_SIZE; i++)
        {
            if (fabsf(dst[i] - value) > EPSILON)
            {
                pass = false;
                break;
            }
        }
        
        if (pass)
            printf("✓ PASS: fill() works correctly\n\n");
        else
            printf("✗ FAIL: fill() produced incorrect results\n\n");
    }

    // Test 4: fill_zero
    printf("Test 4: fill_zero()\n");
    {
        float dst[TEST_SIZE];
        
        // Initialize with non-zero values
        for (size_t i = 0; i < TEST_SIZE; i++)
            dst[i] = (float)i + 1.0f;
        
        dsp::fill_zero(dst, TEST_SIZE);
        
        print_array("dst", dst, TEST_SIZE);
        
        bool pass = true;
        for (size_t i = 0; i < TEST_SIZE; i++)
        {
            if (fabsf(dst[i]) > EPSILON)
            {
                pass = false;
                break;
            }
        }
        
        if (pass)
            printf("✓ PASS: fill_zero() works correctly\n\n");
        else
            printf("✗ FAIL: fill_zero() produced incorrect results\n\n");
    }

    // Test 5: fill_one
    printf("Test 5: fill_one()\n");
    {
        float dst[TEST_SIZE];
        
        dsp::fill_one(dst, TEST_SIZE);
        
        print_array("dst", dst, TEST_SIZE);
        
        bool pass = true;
        for (size_t i = 0; i < TEST_SIZE; i++)
        {
            if (fabsf(dst[i] - 1.0f) > EPSILON)
            {
                pass = false;
                break;
            }
        }
        
        if (pass)
            printf("✓ PASS: fill_one() works correctly\n\n");
        else
            printf("✗ FAIL: fill_one() produced incorrect results\n\n");
    }

    // Test 6: fill_minus_one
    printf("Test 6: fill_minus_one()\n");
    {
        float dst[TEST_SIZE];
        
        dsp::fill_minus_one(dst, TEST_SIZE);
        
        print_array("dst", dst, TEST_SIZE);
        
        bool pass = true;
        for (size_t i = 0; i < TEST_SIZE; i++)
        {
            if (fabsf(dst[i] - (-1.0f)) > EPSILON)
            {
                pass = false;
                break;
            }
        }
        
        if (pass)
            printf("✓ PASS: fill_minus_one() works correctly\n\n");
        else
            printf("✗ FAIL: fill_minus_one() produced incorrect results\n\n");
    }

    // Test 7: reverse1 (in-place)
    printf("Test 7: reverse1() - in-place\n");
    {
        float dst[TEST_SIZE];
        float expected[TEST_SIZE];
        
        // Initialize
        for (size_t i = 0; i < TEST_SIZE; i++)
        {
            dst[i] = (float)i;
            expected[TEST_SIZE - 1 - i] = (float)i;
        }
        
        print_array("before", dst, TEST_SIZE);
        
        dsp::reverse1(dst, TEST_SIZE);
        
        print_array("after", dst, TEST_SIZE);
        print_array("expected", expected, TEST_SIZE);
        
        if (arrays_equal(dst, expected, TEST_SIZE))
            printf("✓ PASS: reverse1() works correctly\n\n");
        else
            printf("✗ FAIL: reverse1() produced incorrect results\n\n");
    }

    // Test 8: reverse2 (separate buffers)
    printf("Test 8: reverse2() - separate buffers\n");
    {
        float src[TEST_SIZE];
        float dst[TEST_SIZE];
        float expected[TEST_SIZE];
        
        // Initialize
        for (size_t i = 0; i < TEST_SIZE; i++)
        {
            src[i] = (float)(i * 2);
            expected[TEST_SIZE - 1 - i] = (float)(i * 2);
        }
        
        memset(dst, 0, sizeof(dst));
        
        print_array("src", src, TEST_SIZE);
        
        dsp::reverse2(dst, src, TEST_SIZE);
        
        print_array("dst", dst, TEST_SIZE);
        print_array("expected", expected, TEST_SIZE);
        
        if (arrays_equal(dst, expected, TEST_SIZE))
            printf("✓ PASS: reverse2() works correctly\n\n");
        else
            printf("✗ FAIL: reverse2() produced incorrect results\n\n");
    }

    printf("=== All tests completed ===\n");
    
    return 0;
}
