#!/bin/bash

# Build all test harness executables for all supported architectures
# This script builds separate test binaries for x86_64, arm64-v8a, and armeabi-v7a

set -e

echo "========================================"
echo "Building All Test Harness Executables"
echo "========================================"
echo ""

# Track build results
BUILDS_SUCCEEDED=0
BUILDS_FAILED=0
FAILED_ARCHS=""

# Build x86_64 test harness
echo ">>> Building x86_64 test harness..."
if ./build_test_harness_x86_64.sh; then
    echo "✓ x86_64 build succeeded"
    BUILDS_SUCCEEDED=$((BUILDS_SUCCEEDED + 1))
else
    echo "✗ x86_64 build failed"
    BUILDS_FAILED=$((BUILDS_FAILED + 1))
    FAILED_ARCHS="$FAILED_ARCHS x86_64"
fi
echo ""

# Build arm64-v8a test harness
echo ">>> Building arm64-v8a test harness..."
if ./build_test_harness_arm64.sh; then
    echo "✓ arm64-v8a build succeeded"
    BUILDS_SUCCEEDED=$((BUILDS_SUCCEEDED + 1))
else
    echo "✗ arm64-v8a build failed"
    BUILDS_FAILED=$((BUILDS_FAILED + 1))
    FAILED_ARCHS="$FAILED_ARCHS arm64-v8a"
fi
echo ""

# Build armeabi-v7a test harness (optional)
echo ">>> Building armeabi-v7a test harness (optional)..."
if ./build_test_harness_armeabi_v7a.sh; then
    echo "✓ armeabi-v7a build succeeded"
    BUILDS_SUCCEEDED=$((BUILDS_SUCCEEDED + 1))
else
    echo "✗ armeabi-v7a build failed"
    BUILDS_FAILED=$((BUILDS_FAILED + 1))
    FAILED_ARCHS="$FAILED_ARCHS armeabi-v7a"
fi
echo ""

# Print summary
echo "========================================"
echo "Build Summary"
echo "========================================"
echo "Succeeded: $BUILDS_SUCCEEDED"
echo "Failed: $BUILDS_FAILED"

if [ $BUILDS_FAILED -gt 0 ]; then
    echo ""
    echo "Failed architectures:$FAILED_ARCHS"
    echo ""
    echo "✗ Some builds failed"
    exit 1
else
    echo ""
    echo "✓ All test harness builds succeeded!"
    echo ""
    echo "Test binaries created:"
    echo "  - build_test_harness_x86_64/test-harness-x86_64"
    echo "  - build_test_harness_arm64/test-harness-arm64-v8a"
    echo "  - build_test_harness_armeabi_v7a/test-harness-armeabi-v7a"
    echo ""
    echo "Next steps:"
    echo "  1. Deploy to devices/emulators using adb push"
    echo "  2. Run verification script: ./verify_test_harnesses.sh"
    echo ""
    exit 0
fi
