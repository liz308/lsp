#!/bin/bash
# Verification script for -Oz optimization on armeabi-v7a
# This script verifies that the CMakeLists.txt correctly configures -Oz for armeabi-v7a

set -e

echo "=== Verifying -Oz Optimization Configuration for armeabi-v7a ==="
echo ""

# Check if CMakeLists.txt exists
if [ ! -f "CMakeLists.txt" ]; then
    echo "ERROR: CMakeLists.txt not found in current directory"
    exit 1
fi

# Check for -Oz flag in armeabi-v7a section
echo "Checking for -Oz flag in armeabi-v7a configuration..."
if grep -A 2 "armeabi-v7a" CMakeLists.txt | grep -q "\-Oz"; then
    echo "✓ -Oz flag found in armeabi-v7a configuration"
else
    echo "✗ -Oz flag NOT found in armeabi-v7a configuration"
    exit 1
fi

# Verify the complete flag set for armeabi-v7a
echo ""
echo "armeabi-v7a compiler flags:"
grep -A 2 "armeabi-v7a" CMakeLists.txt | grep "add_compile_options"

echo ""
echo "=== Verification Complete ==="
echo ""
echo "The -Oz optimization flag is correctly configured for armeabi-v7a."
echo "This minimizes binary size for legacy devices while maintaining NEON support."
echo ""
echo "Expected flags for armeabi-v7a:"
echo "  -march=armv7-a    : Target ARMv7-A architecture"
echo "  -mfloat-abi=softfp: Use software floating-point ABI"
echo "  -mfpu=neon        : Enable NEON SIMD instructions"
echo "  -Oz               : Optimize for size (smaller than -Os)"
