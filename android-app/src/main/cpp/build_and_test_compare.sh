#!/bin/bash

# Build and test script for DSP compare functions
# Tests sample comparison operations (min, max, abs_min, abs_max, etc.)

set -e

echo "=== Building DSP Compare Test ==="
echo

# Compiler settings
CXX=${CXX:-g++}
CXXFLAGS="-std=c++17 -O2 -Wall -Wextra"

# Include paths
INCLUDES="-I./lsp-plugins/include"

# Source files
SOURCES="test_dsp_compare.cpp lsp-plugins/src/dsp/compare.cpp"

# Output binary
OUTPUT="test_dsp_compare"

echo "Compiler: $CXX"
echo "Flags: $CXXFLAGS"
echo "Includes: $INCLUDES"
echo "Sources: $SOURCES"
echo "Output: $OUTPUT"
echo

# Compile
echo "Compiling..."
$CXX $CXXFLAGS $INCLUDES $SOURCES -o $OUTPUT -lm

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
    echo
    
    # Run tests
    echo "=== Running Tests ==="
    echo
    ./$OUTPUT
    
    if [ $? -eq 0 ]; then
        echo
        echo "✓ All tests passed!"
    else
        echo
        echo "✗ Some tests failed"
        exit 1
    fi
else
    echo "✗ Compilation failed"
    exit 1
fi
