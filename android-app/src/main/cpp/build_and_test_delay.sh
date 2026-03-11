#!/bin/bash

# Build and test script for LSP Plugins Delay implementations
# Tests both Delay and DynamicDelay classes on Android NDK

set -e

echo "=== Building Delay Test Program ==="
echo ""

# Configuration
BUILD_DIR="build_delay_test"
TEST_PROGRAM="test_dsp_delay"
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

# Build for arm64-v8a
echo "Building for arm64-v8a..."
TOOLCHAIN="$ANDROID_NDK/toolchains/llvm/prebuilt/$HOST_TAG"
CXX="$TOOLCHAIN/bin/aarch64-linux-android30-clang++"

$CXX \
    -std=c++17 \
    -O2 \
    -I../lsp-plugins/include \
    -I../lsp-plugins/include/core \
    -I../lsp-plugins/include/dsp \
    -DANDROID \
    -DARCH_AARCH64 \
    ../test_dsp_delay.cpp \
    ../lsp-plugins/src/core/util/Delay.cpp \
    ../lsp-plugins/src/core/util/DynamicDelay.cpp \
    ../lsp-plugins/src/core/alloc.cpp \
    ../lsp-plugins/src/core/debug.cpp \
    ../lsp-plugins/src/dsp/dsp.cpp \
    ../lsp-plugins/src/dsp/native.cpp \
    ../lsp-plugins/src/dsp/bits.cpp \
    -o ${TEST_PROGRAM}_arm64 \
    -llog -lm

echo "✓ Built for arm64-v8a"

# Build for x86_64 (for emulator testing)
echo ""
echo "Building for x86_64..."
CXX="$TOOLCHAIN/bin/x86_64-linux-android30-clang++"

$CXX \
    -std=c++17 \
    -O2 \
    -I../lsp-plugins/include \
    -I../lsp-plugins/include/core \
    -I../lsp-plugins/include/dsp \
    -DANDROID \
    -DARCH_X86_64 \
    ../test_dsp_delay.cpp \
    ../lsp-plugins/src/core/util/Delay.cpp \
    ../lsp-plugins/src/core/util/DynamicDelay.cpp \
    ../lsp-plugins/src/core/alloc.cpp \
    ../lsp-plugins/src/core/debug.cpp \
    ../lsp-plugins/src/dsp/dsp.cpp \
    ../lsp-plugins/src/dsp/native.cpp \
    ../lsp-plugins/src/dsp/bits.cpp \
    -o ${TEST_PROGRAM}_x86_64 \
    -llog -lm

echo "✓ Built for x86_64"

echo ""
echo "=== Build Complete ==="
echo ""
echo "Test binaries created:"
echo "  - ${TEST_PROGRAM}_arm64 (for arm64-v8a devices)"
echo "  - ${TEST_PROGRAM}_x86_64 (for x86_64 emulators)"
echo ""
echo "To run tests on device/emulator:"
echo "  adb push ${TEST_PROGRAM}_arm64 /data/local/tmp/"
echo "  adb shell /data/local/tmp/${TEST_PROGRAM}_arm64"
echo ""
echo "Or for emulator:"
echo "  adb push ${TEST_PROGRAM}_x86_64 /data/local/tmp/"
echo "  adb shell /data/local/tmp/${TEST_PROGRAM}_x86_64"

cd ..
