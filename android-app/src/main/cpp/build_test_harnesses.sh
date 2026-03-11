#!/bin/bash
# Build script for architecture-specific test harness executables
# This script builds test harness binaries for each supported Android architecture

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="${SCRIPT_DIR}/build_test_harnesses"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Building Test Harness Executables"
echo "=========================================="

# Check for Android NDK
if [ -z "$ANDROID_NDK" ]; then
    if [ -z "$ANDROID_NDK_HOME" ]; then
        echo -e "${RED}Error: ANDROID_NDK or ANDROID_NDK_HOME environment variable not set${NC}"
        echo "Please set one of these variables to your NDK installation path"
        exit 1
    fi
    ANDROID_NDK="$ANDROID_NDK_HOME"
fi

echo -e "${BLUE}Using Android NDK: ${ANDROID_NDK}${NC}"

# Function to build for a specific architecture
build_for_abi() {
    local ABI=$1
    local BUILD_SUBDIR="${BUILD_DIR}/${ABI}"
    
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Building for ${ABI}${NC}"
    echo -e "${BLUE}========================================${NC}"
    
    # Create build directory
    mkdir -p "${BUILD_SUBDIR}"
    cd "${BUILD_SUBDIR}"
    
    # Configure with CMake
    echo -e "${BLUE}Configuring CMake for ${ABI}...${NC}"
    cmake "${SCRIPT_DIR}" \
        -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK}/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI="${ABI}" \
        -DANDROID_PLATFORM=android-21 \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_ANDROID_NDK="${ANDROID_NDK}"
    
    # Build
    echo -e "${BLUE}Building for ${ABI}...${NC}"
    cmake --build . --config Release
    
    # Check if test harness was built
    if [ "${ABI}" = "x86_64" ]; then
        TEST_HARNESS="test-harness-x86_64"
    elif [ "${ABI}" = "arm64-v8a" ]; then
        TEST_HARNESS="test-harness-arm64-v8a"
    elif [ "${ABI}" = "armeabi-v7a" ]; then
        TEST_HARNESS="test-harness-armeabi-v7a"
    fi
    
    if [ -f "${TEST_HARNESS}" ]; then
        echo -e "${GREEN}✓ Successfully built ${TEST_HARNESS}${NC}"
        
        # Show file info
        ls -lh "${TEST_HARNESS}"
        file "${TEST_HARNESS}"
        
        # Copy to output directory
        OUTPUT_DIR="${SCRIPT_DIR}/test_harness_binaries/${ABI}"
        mkdir -p "${OUTPUT_DIR}"
        cp "${TEST_HARNESS}" "${OUTPUT_DIR}/"
        echo -e "${GREEN}✓ Copied to ${OUTPUT_DIR}/${TEST_HARNESS}${NC}"
    else
        echo -e "${RED}✗ Failed to build ${TEST_HARNESS}${NC}"
        return 1
    fi
    
    cd "${SCRIPT_DIR}"
}

# Build for each architecture
echo ""
echo "Building test harnesses for all architectures..."
echo ""

# Build for x86_64 (emulator)
build_for_abi "x86_64"

# Build for arm64-v8a (primary physical devices)
build_for_abi "arm64-v8a"

# Build for armeabi-v7a (legacy devices) - optional
# Uncomment if you want to support 32-bit ARM devices
# build_for_abi "armeabi-v7a"

echo ""
echo "=========================================="
echo -e "${GREEN}✓ All test harnesses built successfully${NC}"
echo "=========================================="
echo ""
echo "Test harness binaries are located in:"
echo "  ${SCRIPT_DIR}/test_harness_binaries/"
echo ""
echo "To deploy and run on a device:"
echo "  adb push test_harness_binaries/arm64-v8a/test-harness-arm64-v8a /data/local/tmp/"
echo "  adb shell chmod +x /data/local/tmp/test-harness-arm64-v8a"
echo "  adb shell /data/local/tmp/test-harness-arm64-v8a"
echo ""
echo "To run on an emulator:"
echo "  adb push test_harness_binaries/x86_64/test-harness-x86_64 /data/local/tmp/"
echo "  adb shell chmod +x /data/local/tmp/test-harness-x86_64"
echo "  adb shell /data/local/tmp/test-harness-x86_64"
echo ""
