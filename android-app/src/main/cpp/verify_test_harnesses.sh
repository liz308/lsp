#!/bin/bash

# Verification script for test harness executables
# Checks that all architecture-specific test harnesses are built correctly

set -e

echo "========================================"
echo "Test Harness Verification"
echo "========================================"
echo ""

# Track verification results
CHECKS_PASSED=0
CHECKS_FAILED=0
FAILED_CHECKS=""

# Function to check if a file exists and is executable
check_binary() {
    local binary_path=$1
    local arch_name=$2
    
    echo "Checking $arch_name test harness..."
    
    if [ ! -f "$binary_path" ]; then
        echo "  ✗ Binary not found: $binary_path"
        CHECKS_FAILED=$((CHECKS_FAILED + 1))
        FAILED_CHECKS="$FAILED_CHECKS $arch_name"
        return 1
    fi
    
    if [ ! -x "$binary_path" ]; then
        echo "  ✗ Binary not executable: $binary_path"
        CHECKS_FAILED=$((CHECKS_FAILED + 1))
        FAILED_CHECKS="$FAILED_CHECKS $arch_name"
        return 1
    fi
    
    # Check file size (should be > 100KB for a real binary)
    local file_size=$(stat -c%s "$binary_path" 2>/dev/null || stat -f%z "$binary_path" 2>/dev/null)
    if [ "$file_size" -lt 102400 ]; then
        echo "  ⚠ Warning: Binary seems small ($file_size bytes)"
    fi
    
    echo "  ✓ Binary exists and is executable"
    echo "  Size: $file_size bytes"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
    return 0
}

# Check x86_64 test harness
check_binary "build_test_harness_x86_64/test-harness-x86_64" "x86_64"
echo ""

# Check arm64-v8a test harness
check_binary "build_test_harness_arm64/test-harness-arm64-v8a" "arm64-v8a"
echo ""

# Check armeabi-v7a test harness (optional)
check_binary "build_test_harness_armeabi_v7a/test-harness-armeabi-v7a" "armeabi-v7a"
echo ""

# Print summary
echo "========================================"
echo "Verification Summary"
echo "========================================"
echo "Checks passed: $CHECKS_PASSED"
echo "Checks failed: $CHECKS_FAILED"

if [ $CHECKS_FAILED -gt 0 ]; then
    echo ""
    echo "Failed checks:$FAILED_CHECKS"
    echo ""
    echo "✗ Verification failed"
    echo ""
    echo "To rebuild failed architectures:"
    for arch in $FAILED_CHECKS; do
        echo "  ./build_test_harness_${arch}.sh"
    done
    echo ""
    exit 1
else
    echo ""
    echo "✓ All test harness binaries verified!"
    echo ""
    echo "Deployment instructions:"
    echo ""
    echo "For x86_64 emulator:"
    echo "  adb push build_test_harness_x86_64/test-harness-x86_64 /data/local/tmp/"
    echo "  adb shell chmod +x /data/local/tmp/test-harness-x86_64"
    echo "  adb shell /data/local/tmp/test-harness-x86_64"
    echo ""
    echo "For arm64-v8a device:"
    echo "  adb push build_test_harness_arm64/test-harness-arm64-v8a /data/local/tmp/"
    echo "  adb shell chmod +x /data/local/tmp/test-harness-arm64-v8a"
    echo "  adb shell /data/local/tmp/test-harness-arm64-v8a"
    echo ""
    echo "For armeabi-v7a device (legacy):"
    echo "  adb push build_test_harness_armeabi_v7a/test-harness-armeabi-v7a /data/local/tmp/"
    echo "  adb shell chmod +x /data/local/tmp/test-harness-armeabi-v7a"
    echo "  adb shell /data/local/tmp/test-harness-armeabi-v7a"
    echo ""
    exit 0
fi
