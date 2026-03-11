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

