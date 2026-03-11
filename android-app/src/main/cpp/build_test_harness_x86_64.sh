#!/bin/bash

# Build script for x86_64 test harness
# Creates a comprehensive test executable for Android x86_64 emulators
# Tests DSP functionality including compare, delay, and other operations

set -e

echo "=== Building x86_64 Test Harness ==="
echo ""

# Configuration
BUILD_DIR="build_test_harness_x86_64"
TEST_PROGRAM="test-harness-x86_64"
ANDROID_NDK="${ANDROID_NDK_HOME:-$ANDROID_HOME/ndk-bundle}"

# Check for NDK
if [ ! -d "$ANDROID_NDK" ]; then
    echo "Error: Android NDK not found at $ANDROID_NDK"
    echo "Please set ANDROID_NDK_HOME environment variable"
    exit 1
fi

# Clean previous build
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

# Detect host OS for toolchain
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    HOST_TAG="linux-x86_64"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    HOST_TAG="darwin-x86_64"
else
    echo "Unsupported OS: $OSTYPE"
    exit 1
fi

# Toolchain setup for x86_64
echo "Configuring x86_64 toolchain..."
TOOLCHAIN="$ANDROID_NDK/toolchains/llvm/prebuilt/$HOST_TAG"
CXX="$TOOLCHAIN/bin/x86_64-linux-android30-clang++"

# Compiler flags for x86_64
CXXFLAGS="-std=c++17 -O3 -msse2 -ffast-math"
CXXFLAGS="$CXXFLAGS -Wall -Wextra -Wno-unused-parameter"
CXXFLAGS="$CXXFLAGS -DANDROID -DARCH_X86_64 -D__SSE2__=1"

# Include paths
INCLUDES="-I../lsp-plugins/include"
INCLUDES="$INCLUDES -I../lsp-plugins/include/core"
INCLUDES="$INCLUDES -I../lsp-plugins/include/dsp"
INCLUDES="$INCLUDES -I.."

# Source files for comprehensive test harness
SOURCES="../test_harness_x86_64.cpp"
SOURCES="$SOURCES ../lsp-plugins/src/dsp/compare.cpp"
SOURCES="$SOURCES ../lsp-plugins/src/core/alloc.cpp"
SOURCES="$SOURCES ../lsp-plugins/src/core/debug.cpp"

# Link libraries
LIBS="-llog -lm"

echo ""
echo "Build configuration:"
echo "  Compiler: $CXX"
echo "  Flags: $CXXFLAGS"
echo "  Architecture: x86_64 (SSE2 baseline)"
echo "  Output: $TEST_PROGRAM"
echo ""

# Compile
echo "Compiling x86_64 test harness..."
$CXX $CXXFLAGS $INCLUDES $SOURCES -o $TEST_PROGRAM $LIBS

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
    echo ""
    echo "=== Build Complete ===" 
    echo ""
    echo "Test binary created: $BUILD_DIR/$TEST_PROGRAM"
    echo ""
    echo "To run on x86_64 emulator:"
    echo "  1. Start an x86_64 Android emulator"
    echo "  2. Push the binary:"
    echo "     adb push $BUILD_DIR/$TEST_PROGRAM /data/local/tmp/"
    echo "  3. Make it executable:"
    echo "     adb shell chmod +x /data/local/tmp/$TEST_PROGRAM"
    echo "  4. Run the tests:"
    echo "     adb shell /data/local/tmp/$TEST_PROGRAM"
    echo ""
    echo "CPU features detected at runtime:"
    echo "  - SSE2 (baseline for x86_64)"
    echo "  - SSE4.1 (if available)"
    echo "  - AVX (if available)"
    echo ""
else
    echo "✗ Compilation failed"
    exit 1
fi

cd ..

