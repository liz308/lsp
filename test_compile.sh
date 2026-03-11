#!/bin/bash
# Simple test script to verify native test harness compilation

echo "Testing compilation of native test harness..."

# Create a temporary build directory
mkdir -p build_test
cd build_test

# Try to compile with CMake
cmake ../dsp-core -DCMAKE_BUILD_TYPE=Debug

if [ $? -eq 0 ]; then
    echo "CMake configuration successful"
    
    # Try to build
    cmake --build . --target lsp_native_test_harness
    
    if [ $? -eq 0 ]; then
        echo "Build successful!"
        
        # Check if the executable was created
        if [ -f "lsp_native_test_harness" ]; then
            echo "Executable created: build_test/lsp_native_test_harness"
            echo "Test compilation successful!"
        else
            echo "Error: Executable not found"
            exit 1
        fi
    else
        echo "Build failed"
        exit 1
    fi
else
    echo "CMake configuration failed"
    exit 1
fi