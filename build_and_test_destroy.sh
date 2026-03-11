#!/bin/bash

# Build and test script for plugin_destroy() function
# This script compiles the test and runs it to verify the implementation

set -e

echo "Building plugin_destroy() test..."

# Create build directory
mkdir -p build_test_destroy

# Compile the bridge implementation and test
g++ -std=c++17 -O2 -Wall -Wextra \
    dsp-core/lsp_android_bridge.cpp \
    dsp-core/tests/test_plugin_destroy.cpp \
    -o build_test_destroy/test_plugin_destroy \
    -I.

echo "Build successful!"
echo ""
echo "Running tests..."
echo ""

# Run the test
./build_test_destroy/test_plugin_destroy

echo ""
echo "Test execution completed!"
