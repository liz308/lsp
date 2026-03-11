#!/bin/bash

# Build script for armeabi-v7a test harness
# Creates a comprehensive test executable for Android armeabi-v7a devices (legacy)
# Tests DSP functionality including compare, delay, and other operations

set -e

echo "=== Building armeabi-v7a Test Harness ==="
echo ""

# Configuration
BUILD_DIR="build_test_harness_armeabi_v7a"
TEST_PROGRAM="test-harness-armeabi-v7a"
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

# Toolchain setup for armeabi-v7a
echo "Configuring armeabi-v7a toolchain..."
TOOLCHAIN="$ANDROID_NDK/toolchains/llvm/prebuilt/$HOST_TAG"
CXX="$TOOLCHAIN/bin/armv7a-linux-androideabi21-clang++"

# Compiler flags for armeabi-v7a
# Using -Oz for size optimization on legacy devices
CXXFLAGS="-std=c++17 -Oz -march=armv7-a -mfloat-abi=softfp -mfpu=neon -ffast-math"
CXXFLAGS="$CXXFLAGS -Wall -Wextra -Wno-unused-parameter"
CXXFLAGS="$CXXFLAGS -DANDROID -DARCH_ARMV7 -DUSE_NEON=1"

# Include paths
INCLUDES="-I../lsp-plugins/include"
INCLUDES="$INCLUDES -I../lsp-plugins/include/core"
INCLUDES="$INCLUDES -I../lsp-plugins/include/dsp"
INCLUDES="$INCLUDES -I.."

# Source files for comprehensive test harness
# Reusing arm64 test harness source with different compilation flags
SOURCES="../test_harness_arm64.cpp"
SOURCES="$SOURCES ../lsp-plugins/src/dsp/compare.cpp"
SOURCES="$SOURCES ../lsp-plugins/src/core/alloc.cpp"
SOURCES="$SOURCES ../lsp-plugins/src/core/debug.cpp"

# Link libraries
LIBS="-llog -lm"

echo ""
echo "Build configuration:"
echo "  Compiler: $CXX"
echo "  Flags: $CXXFLAGS"
echo "  Architecture: armeabi-v7a (NEON with runtime detection)"
echo "  Optimization: -Oz (size optimized for legacy devices)"
echo "  Output: $TEST_PROGRAM"
echo ""

# Compile
echo "Compiling armeabi-v7a test harness..."
$CXX $CXXFLAGS $INCLUDES $SOURCES -o $TEST_PROGRAM $LIBS

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
    echo ""
    echo "=== Build Complete ===" 
    echo ""
    echo "Test binary created: $BUILD_DIR/$TEST_PROGRAM"
    echo ""
    echo "To run on armeabi-v7a device:"
    echo "  1. Connect an armeabi-v7a Android device via USB (legacy device)"
    echo "  2. Push the binary:"
    echo "     adb push $BUILD_DIR/$TEST_PROGRAM /data/local/tmp/"
    echo "  3. Make it executable:"
    echo "     adb shell chmod +x /data/local/tmp/$TEST_PROGRAM"
    echo "  4. Run the tests:"
    echo "     adb shell /data/local/tmp/$TEST_PROGRAM"
    echo ""
    echo "CPU features detected at runtime:"
    echo "  - NEON (if available on device)"
    echo "  - VFPv4 fallback for scalar operations"
    echo ""
    echo "Note: This is a legacy architecture for older devices (API 16-20)"
    echo "      Modern devices should use arm64-v8a"
    echo ""
else
    echo "✗ Compilation failed"
    exit 1
fi

cd ..
