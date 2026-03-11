#!/bin/bash
echo "Testing simple compilation of native test harness..."

# Create build directory
mkdir -p build_simple
cd build_simple

# Try to compile directly with g++
echo "Compiling native_test_harness.cpp..."
g++ -std=c++17 -I../dsp-core -O3 -ffast-math -fno-exceptions \
    ../dsp-core/tests/native_test_harness.cpp \
    ../dsp-core/lsp_android_bridge.cpp \
    -o lsp_native_test_harness \
    -lm

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo "Executable created: build_simple/lsp_native_test_harness"
    
    # Test the executable
    echo ""
    echo "Testing executable with --help flag:"
    ./lsp_native_test_harness --help
    
    echo ""
    echo "Testing executable with --list-params flag:"
    ./lsp_native_test_harness --list-params
    
    echo ""
    echo "Testing basic functionality:"
    ./lsp_native_test_harness --output test_output.wav --duration 0.1
    
    if [ -f "test_output.wav" ]; then
        echo "WAV file created: test_output.wav"
        echo "Test successful!"
    else
        echo "Error: WAV file not created"
        exit 1
    fi
else
    echo "Compilation failed"
    exit 1
fi