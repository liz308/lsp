#!/bin/bash

# Build and test DSP array operations
# This script compiles the test program and runs it

set -e

echo "=== Building DSP Array Operations Test ==="

# Create build directory
BUILD_DIR="build_test_array"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

# Compiler settings
CXX="g++"
CXXFLAGS="-std=c++17 -O2 -Wall -Wextra"
INCLUDES="-I android-app/src/main/cpp/lsp-plugins/include -I android-app/src/main/cpp"

# Source files
SOURCES=(
    "android-app/src/main/cpp/lsp-plugins/src/dsp/dsp.cpp"
    "android-app/src/main/cpp/lsp-plugins/src/dsp/native.cpp"
    "android-app/src/main/cpp/test_dsp_array.cpp"
)

# Output binary
OUTPUT="$BUILD_DIR/test_dsp_array"

echo "Compiling..."
$CXX $CXXFLAGS $INCLUDES "${SOURCES[@]}" -o "$OUTPUT" -lm

echo "Build successful!"
echo ""
echo "=== Running Tests ==="
echo ""

"$OUTPUT"

echo ""
echo "=== Test Complete ==="
