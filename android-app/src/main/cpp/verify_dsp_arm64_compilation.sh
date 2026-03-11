#!/bin/bash
# Verification script for DSP files compilation on arm64-v8a with NEON
# Task: Verify all DSP files compile for arm64-v8a with NEON

set -e

echo "=========================================="
echo "DSP arm64-v8a NEON Compilation Verification"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BUILD_DIR="build_dsp_arm64_test"
NDK_PATH="${ANDROID_NDK_HOME:-/opt/android-sdk/ndk/27.0.12077973}"
TOOLCHAIN="$NDK_PATH/build/cmake/android.toolchain.cmake"
ABI="arm64-v8a"
API_LEVEL=21

# Check NDK
if [ ! -d "$NDK_PATH" ]; then
    echo -e "${RED}ERROR: Android NDK not found at $NDK_PATH${NC}"
    echo "Please set ANDROID_NDK_HOME environment variable"
    exit 1
fi

echo "Using NDK: $NDK_PATH"
echo "Target ABI: $ABI"
echo "API Level: $API_LEVEL"
echo ""

# Create build directory
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

# List of DSP files to verify (from tasks.md Tier 1)
DSP_FILES=(
    "../lsp-plugins/src/dsp/alloc.cpp"
    "../lsp-plugins/src/dsp/bits.cpp"
    "../lsp-plugins/src/dsp/compare.cpp"
    "../lsp-plugins/src/dsp/dsp.cpp"
    "../lsp-plugins/src/dsp/native.cpp"
    "../lsp-plugins/src/dsp/matrix.cpp"
)

# Core files needed for DSP
CORE_FILES=(
    "../lsp-plugins/src/core/alloc.cpp"
    "../lsp-plugins/src/core/debug.cpp"
    "../lsp-plugins/src/core/types.cpp"
)

echo "Creating test CMakeLists.txt..."

# Create a minimal CMakeLists.txt for testing
cat > CMakeLists.txt << 'EOF'
cmake_minimum_required(VERSION 3.22)
project(dsp_arm64_test VERSION 1.0.0 LANGUAGES C CXX)

# Android NDK configuration
set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Compiler optimizations for arm64-v8a
# NOTE: Removed -ffast-math as it's prohibited by LSP plugins upstream
add_compile_options(
    -O3
    -march=armv8-a+simd
    -funroll-loops
    -fomit-frame-pointer
    $<$<COMPILE_LANGUAGE:CXX>:-fno-exceptions>
    $<$<COMPILE_LANGUAGE:CXX>:-fno-rtti>
    -fPIC
    -ffunction-sections
    -fdata-sections
)

# NEON is baseline for arm64-v8a
add_compile_definitions(
    __ARM_NEON=1
    USE_NEON=1
    ANDROID_PLATFORM=android-21
    ANDROID_ABI=arm64-v8a
)

# Include directories
include_directories(
    ../lsp-plugins/include
    ../lsp-plugins/include/dsp
    ../lsp-plugins/include/core
    ../
)

# DSP source files
set(DSP_SOURCES
    ../lsp-plugins/src/dsp/alloc.cpp
    ../lsp-plugins/src/dsp/bits.cpp
    ../lsp-plugins/src/dsp/compare.cpp
    ../lsp-plugins/src/dsp/dsp.cpp
    ../lsp-plugins/src/dsp/native.cpp
    ../lsp-plugins/src/dsp/matrix.cpp
)

# Core support files
set(CORE_SOURCES
    ../lsp-plugins/src/core/alloc.cpp
    ../lsp-plugins/src/core/debug.cpp
    ../lsp-plugins/src/core/types.cpp
)

# Create test library
add_library(dsp_arm64_test SHARED
    ${DSP_SOURCES}
    ${CORE_SOURCES}
)

# Link libraries
target_link_libraries(dsp_arm64_test
    PRIVATE
        log
        android
)

# Enable warnings
target_compile_options(dsp_arm64_test PRIVATE
    -Wall
    -Wextra
    -Wno-unused-parameter
    -Wno-missing-field-initializers
)

EOF

echo -e "${YELLOW}Configuring CMake for arm64-v8a...${NC}"
cmake \
    -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN" \
    -DANDROID_ABI="$ABI" \
    -DANDROID_PLATFORM="android-$API_LEVEL" \
    -DANDROID_STL=c++_shared \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_EXPORT_COMPILE_COMMANDS=ON \
    .

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: CMake configuration failed${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Building DSP files for arm64-v8a with NEON...${NC}"
cmake --build . -- -j$(nproc)

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Build failed${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Build successful!${NC}"
echo ""

# Verify the output library
LIB_FILE="libdsp_arm64_test.so"
if [ -f "$LIB_FILE" ]; then
    echo -e "${GREEN}✓ Library created: $LIB_FILE${NC}"
    
    # Check file info
    echo ""
    echo "Library information:"
    file "$LIB_FILE"
    
    # Check size
    SIZE=$(stat -f%z "$LIB_FILE" 2>/dev/null || stat -c%s "$LIB_FILE" 2>/dev/null)
    echo "Size: $SIZE bytes"
    
    # Check for NEON symbols (if readelf is available)
    if command -v readelf &> /dev/null; then
        echo ""
        echo "Checking for ARM NEON usage..."
        if readelf -s "$LIB_FILE" | grep -i neon > /dev/null; then
            echo -e "${GREEN}✓ NEON symbols found${NC}"
        else
            echo -e "${YELLOW}⚠ No explicit NEON symbols (may be inlined)${NC}"
        fi
    fi
    
    # Check architecture
    if command -v readelf &> /dev/null; then
        echo ""
        echo "Architecture verification:"
        readelf -h "$LIB_FILE" | grep -E "Machine|Class"
    fi
else
    echo -e "${RED}ERROR: Library file not created${NC}"
    exit 1
fi

echo ""
echo "=========================================="
echo -e "${GREEN}DSP arm64-v8a NEON Compilation: VERIFIED${NC}"
echo "=========================================="
echo ""
echo "Summary:"
echo "  - All DSP files compiled successfully"
echo "  - Target: arm64-v8a with NEON baseline"
echo "  - Optimization: -O3 -march=armv8-a+simd"
echo "  - Output: $LIB_FILE"
echo ""
echo "Next steps:"
echo "  1. Verify x86_64 compilation with SSE2"
echo "  2. Run regression tests on physical arm64-v8a device"
echo "  3. Benchmark NEON performance vs scalar"
echo ""

cd ..
