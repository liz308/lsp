#!/bin/bash
# Verification script for -O3 optimization on arm64-v8a

echo "=== Verifying -O3 Optimization for arm64-v8a ==="
echo ""

# Check CMakeLists.txt for arm64-v8a optimization flags
echo "1. Checking CMakeLists.txt configuration:"
grep -A 3 "arm64-v8a" CMakeLists.txt | grep -E "(O3|march)"

echo ""
echo "2. Checking build.gradle.kts for CMake arguments:"
grep -A 10 "externalNativeBuild" ../../build.gradle.kts | grep -E "(O3|CMAKE_BUILD_TYPE)"

echo ""
echo "3. Expected configuration for arm64-v8a:"
echo "   - Optimization level: -O3 (maximum performance)"
echo "   - Architecture flags: -march=armv8-a+simd"
echo "   - Fast math: -ffast-math"
echo "   - NEON enabled: __ARM_NEON=1"

echo ""
echo "=== Verification Complete ==="
