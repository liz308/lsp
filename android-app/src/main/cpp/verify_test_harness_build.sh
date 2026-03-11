#!/bin/bash
# Verification script for test harness build configuration
# This script checks that test harness executables are properly configured in CMakeLists.txt

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CMAKE_FILE="${SCRIPT_DIR}/CMakeLists.txt"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "=========================================="
echo "Test Harness Build Configuration Verification"
echo "=========================================="

# Check if CMakeLists.txt exists
if [ ! -f "${CMAKE_FILE}" ]; then
    echo -e "${RED}✗ CMakeLists.txt not found${NC}"
    exit 1
fi
echo -e "${GREEN}✓ CMakeLists.txt found${NC}"

# Check for test harness source files
echo ""
echo "Checking test harness source files..."

if [ -f "${SCRIPT_DIR}/test_harness_x86_64.cpp" ]; then
    echo -e "${GREEN}✓ test_harness_x86_64.cpp exists${NC}"
else
    echo -e "${RED}✗ test_harness_x86_64.cpp not found${NC}"
    exit 1
fi

if [ -f "${SCRIPT_DIR}/test_harness_arm64.cpp" ]; then
    echo -e "${GREEN}✓ test_harness_arm64.cpp exists${NC}"
else
    echo -e "${RED}✗ test_harness_arm64.cpp not found${NC}"
    exit 1
fi

# Check CMakeLists.txt for test harness configuration
echo ""
echo "Checking CMakeLists.txt configuration..."

if grep -q "add_executable(test-harness-x86_64" "${CMAKE_FILE}"; then
    echo -e "${GREEN}✓ test-harness-x86_64 executable configured${NC}"
else
    echo -e "${RED}✗ test-harness-x86_64 not configured in CMakeLists.txt${NC}"
    exit 1
fi

if grep -q "add_executable(test-harness-arm64-v8a" "${CMAKE_FILE}"; then
    echo -e "${GREEN}✓ test-harness-arm64-v8a executable configured${NC}"
else
    echo -e "${RED}✗ test-harness-arm64-v8a not configured in CMakeLists.txt${NC}"
    exit 1
fi

if grep -q "add_executable(test-harness-armeabi-v7a" "${CMAKE_FILE}"; then
    echo -e "${GREEN}✓ test-harness-armeabi-v7a executable configured (optional)${NC}"
else
    echo -e "${YELLOW}⚠ test-harness-armeabi-v7a not configured (optional)${NC}"
fi

# Check for required DSP source files
echo ""
echo "Checking DSP source dependencies..."

if [ -f "${SCRIPT_DIR}/lsp-plugins/src/dsp/compare.cpp" ]; then
    echo -e "${GREEN}✓ lsp-plugins/src/dsp/compare.cpp exists${NC}"
else
    echo -e "${RED}✗ lsp-plugins/src/dsp/compare.cpp not found${NC}"
    exit 1
fi

if [ -f "${SCRIPT_DIR}/lsp-plugins/src/core/alloc.cpp" ]; then
    echo -e "${GREEN}✓ lsp-plugins/src/core/alloc.cpp exists${NC}"
else
    echo -e "${RED}✗ lsp-plugins/src/core/alloc.cpp not found${NC}"
    exit 1
fi

# Check for architecture-specific configurations
echo ""
echo "Checking architecture-specific configurations..."

if grep -q "ANDROID_ABI STREQUAL \"x86_64\"" "${CMAKE_FILE}"; then
    echo -e "${GREEN}✓ x86_64 architecture configuration found${NC}"
else
    echo -e "${RED}✗ x86_64 architecture configuration missing${NC}"
    exit 1
fi

if grep -q "ANDROID_ABI STREQUAL \"arm64-v8a\"" "${CMAKE_FILE}"; then
    echo -e "${GREEN}✓ arm64-v8a architecture configuration found${NC}"
else
    echo -e "${RED}✗ arm64-v8a architecture configuration missing${NC}"
    exit 1
fi

# Check for SIMD preprocessor definitions
echo ""
echo "Checking SIMD preprocessor definitions..."

if grep -q "__SSE2__=1" "${CMAKE_FILE}"; then
    echo -e "${GREEN}✓ SSE2 definition for x86_64 found${NC}"
else
    echo -e "${YELLOW}⚠ SSE2 definition for x86_64 not found${NC}"
fi

if grep -q "__ARM_NEON=1" "${CMAKE_FILE}"; then
    echo -e "${GREEN}✓ NEON definition for arm64-v8a found${NC}"
else
    echo -e "${YELLOW}⚠ NEON definition for arm64-v8a not found${NC}"
fi

# Check for build script
echo ""
echo "Checking build scripts..."

if [ -f "${SCRIPT_DIR}/build_test_harnesses.sh" ]; then
    echo -e "${GREEN}✓ build_test_harnesses.sh exists${NC}"
    if [ -x "${SCRIPT_DIR}/build_test_harnesses.sh" ]; then
        echo -e "${GREEN}✓ build_test_harnesses.sh is executable${NC}"
    else
        echo -e "${YELLOW}⚠ build_test_harnesses.sh is not executable (run: chmod +x build_test_harnesses.sh)${NC}"
    fi
else
    echo -e "${YELLOW}⚠ build_test_harnesses.sh not found${NC}"
fi

# Check for documentation
echo ""
echo "Checking documentation..."

if [ -f "${SCRIPT_DIR}/README_TEST_HARNESS_BUILD.md" ]; then
    echo -e "${GREEN}✓ README_TEST_HARNESS_BUILD.md exists${NC}"
else
    echo -e "${YELLOW}⚠ README_TEST_HARNESS_BUILD.md not found${NC}"
fi

# Summary
echo ""
echo "=========================================="
echo -e "${GREEN}✓ Test harness build configuration verified${NC}"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Set ANDROID_NDK environment variable:"
echo "     export ANDROID_NDK=/path/to/android-ndk"
echo ""
echo "  2. Build test harnesses:"
echo "     cd ${SCRIPT_DIR}"
echo "     ./build_test_harnesses.sh"
echo ""
echo "  3. Deploy to device/emulator:"
echo "     adb push test_harness_binaries/arm64-v8a/test-harness-arm64-v8a /data/local/tmp/"
echo "     adb shell chmod +x /data/local/tmp/test-harness-arm64-v8a"
echo "     adb shell /data/local/tmp/test-harness-arm64-v8a"
echo ""
echo "See README_TEST_HARNESS_BUILD.md for detailed instructions."
echo ""
