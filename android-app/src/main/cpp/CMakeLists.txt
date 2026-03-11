cmake_minimum_required(VERSION 3.22)

project(lsp_audio_engine 
    VERSION 1.0.0
    DESCRIPTION "LSP Plugins Android Port"
    LANGUAGES C CXX
)

# Android NDK pthread configuration
if(ANDROID)
    set(CMAKE_THREAD_LIBS_INIT "")
    set(CMAKE_HAVE_THREADS_LIBRARY TRUE)
    set(CMAKE_USE_PTHREADS_INIT TRUE)
endif()

# Compiler standards
set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Build configuration
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

# Compiler optimizations (common flags)
add_compile_options(
    -funroll-loops
    -fomit-frame-pointer
    $<$<COMPILE_LANGUAGE:CXX>:-fno-exceptions>
    $<$<COMPILE_LANGUAGE:CXX>:-fno-rtti>
    $<$<CONFIG:Debug>:-g>
    $<$<CONFIG:Debug>:-O0>
    $<$<CONFIG:Debug>:-DDEBUG>
    $<$<CONFIG:Release>:-DNDEBUG>
)

# Android-specific configurations
if(ANDROID)
    # Configure ANDROID_PLATFORM per architecture
    # API 21 (Lollipop) for arm64-v8a and x86_64
    # API 16 (Jelly Bean) for armeabi-v7a
    if(ANDROID_ABI STREQUAL "arm64-v8a")
        set(ANDROID_PLATFORM "android-21")
        # arm64-v8a: Maximum performance optimization
        add_compile_options(-march=armv8-a+simd -O3)
        add_compile_definitions(__ARM_NEON=1 USE_NEON=1)
    elseif(ANDROID_ABI STREQUAL "x86_64")
        set(ANDROID_PLATFORM "android-21")
        # x86_64: SSE2 baseline optimization
        add_compile_options(-msse2 -O3)
        add_compile_definitions(__SSE2__=1)
    elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
        set(ANDROID_PLATFORM "android-16")
        # armeabi-v7a: Size optimization for legacy devices (-Oz minimizes binary size)
        add_compile_options(-march=armv7-a -mfloat-abi=softfp -mfpu=neon -Oz)
        add_compile_definitions(USE_NEON=1)
    else()
        # Default optimization for other architectures
        add_compile_options(-O3)
    endif()

    # Position-independent code for all architectures
    add_compile_options(-fPIC)
    
    # Enable function and data sections for unused code removal
    # Each function/data item goes into its own section
    add_compile_options(-ffunction-sections -fdata-sections)
    
    # Remove unused code sections per architecture at link time
    # This works in conjunction with -ffunction-sections and -fdata-sections
    add_link_options(-Wl,--gc-sections)
    
    # Note: -ffast-math is disabled because LSP plugins code prohibits it
    # The upstream code has checks that prevent compilation with -ffast-math

    add_compile_definitions(
        ANDROID_PLATFORM=${ANDROID_PLATFORM}
        ANDROID_ABI=${ANDROID_ABI}
        CMAKE_ANDROID_ARCH_ABI=${ANDROID_ABI}
    )
endif()

# Path configurations - simplified for now
# set(DSP_CORE_ROOT "${CMAKE_SOURCE_DIR}/../../../dsp-core")
# set(OBOE_ROOT "${CMAKE_SOURCE_DIR}/../../../external/oboe")
# set(LSP_PLUGINS_ROOT "${CMAKE_SOURCE_DIR}/../../../external/lsp-plugins")

# Add subdirectories for dependencies - disabled for now
# add_subdirectory(${DSP_CORE_ROOT} ${CMAKE_CURRENT_BINARY_DIR}/dsp-core-build)
# add_subdirectory(${OBOE_ROOT} ${CMAKE_CURRENT_BINARY_DIR}/oboe-build)

# LSP Plugins ported source files - minimal set that compiles
set(LSP_PLUGINS_SOURCES
    lsp_audio_engine_stub.cpp
    jni_bridge_minimal.cpp
    CpuFeatures.cpp
    SimdOps.cpp
    lsp-plugins/src/core/alloc.cpp
    lsp-plugins/src/dsp/compare.cpp
)

# JNI Bridge Architecture Test Sources (optional, for testing)
# Enabled since lsp_android_bridge.h is implemented
set(JNI_BRIDGE_TEST_SOURCES
    test_jni_bridge_architectures.cpp
    ../../../../dsp-core/lsp_android_bridge.cpp
)
set_source_files_properties(test_jni_bridge_architectures.cpp ../../../../dsp-core/lsp_android_bridge.cpp PROPERTIES COMPILE_FLAGS "-fexceptions")

# Create the main library
add_library(
    lsp_audio_engine
    SHARED
    ${LSP_PLUGINS_SOURCES}
    ${JNI_BRIDGE_TEST_SOURCES}
)

# Set library properties
set_target_properties(lsp_audio_engine PROPERTIES
    VERSION ${PROJECT_VERSION}
    SOVERSION 1
    OUTPUT_NAME "lsp_audio_engine"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
)

# Include directories
target_include_directories(lsp_audio_engine
    PUBLIC
        $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
        $<INSTALL_INTERFACE:include>
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/lsp-plugins/include
)

# Add cpufeatures include path for Android builds
if(ANDROID)
    add_library(cpufeatures STATIC ${ANDROID_NDK}/sources/android/cpufeatures/cpu-features.c)
    target_include_directories(cpufeatures PUBLIC ${ANDROID_NDK}/sources/android/cpufeatures)
    target_include_directories(lsp_audio_engine PRIVATE ${ANDROID_NDK}/sources/android/cpufeatures)
endif()

# Preprocessor definitions
target_compile_definitions(lsp_audio_engine
    PRIVATE
        LSP_AUDIO_ENGINE_VERSION="${PROJECT_VERSION}"
        AUDIO_ENGINE_SAMPLE_RATE=48000
        AUDIO_ENGINE_FRAMES_PER_BURST=192
)

# Link libraries
target_link_libraries(
    lsp_audio_engine
    PRIVATE
        log          # Android log library for all architectures
        android      # Android native API
        OpenSLES     # OpenSL ES audio
        atomic       # Atomic operations
        cpufeatures  # NDK CPU feature detection
)

# Thread support
find_package(Threads REQUIRED)
target_link_libraries(lsp_audio_engine PRIVATE Threads::Threads)

# Enable warnings in debug builds
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(lsp_audio_engine PRIVATE
        -Wall
        -Wextra
        -Wpedantic
        -Wno-unused-parameter
        -Wno-missing-field-initializers
    )
endif()

# ============================================================================
# Native Test Harness Executables (Per-Architecture)
# ============================================================================
# These are standalone executables for testing DSP code without the full app
# Each architecture gets its own test harness binary

# Common test harness sources
set(TEST_HARNESS_COMMON_SOURCES
    lsp-plugins/src/dsp/compare.cpp
    lsp-plugins/src/core/alloc.cpp
    lsp-plugins/src/core/debug.cpp
)

# Test harness for x86_64
if(ANDROID_ABI STREQUAL "x86_64")
    add_executable(test-harness-x86_64
        test_harness_x86_64.cpp
        CpuFeatures.cpp
        ${TEST_HARNESS_COMMON_SOURCES}
    )
    
    target_include_directories(test-harness-x86_64 PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/lsp-plugins/include
        ${CMAKE_CURRENT_SOURCE_DIR}/lsp-plugins/include/core
        ${CMAKE_CURRENT_SOURCE_DIR}/lsp-plugins/include/dsp
        ${ANDROID_NDK}/sources/android/cpufeatures
    )
    
    target_compile_definitions(test-harness-x86_64 PRIVATE
        ANDROID
        ARCH_X86_64
        __SSE2__=1
    )
    
    target_link_libraries(test-harness-x86_64 PRIVATE
        log
        m
        cpufeatures
    )
    
    message(STATUS "Configured test-harness-x86_64 executable")
endif()

# Test harness for arm64-v8a
if(ANDROID_ABI STREQUAL "arm64-v8a")
    add_executable(test-harness-arm64-v8a
        test_harness_arm64.cpp
        CpuFeatures.cpp
        ${TEST_HARNESS_COMMON_SOURCES}
    )
    
    target_include_directories(test-harness-arm64-v8a PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/lsp-plugins/include
        ${CMAKE_CURRENT_SOURCE_DIR}/lsp-plugins/include/core
        ${CMAKE_CURRENT_SOURCE_DIR}/lsp-plugins/include/dsp
        ${ANDROID_NDK}/sources/android/cpufeatures
    )
    
    target_compile_definitions(test-harness-arm64-v8a PRIVATE
        ANDROID
        ARCH_ARM64
        __ARM_NEON=1
        USE_NEON=1
    )
    
    target_link_libraries(test-harness-arm64-v8a PRIVATE
        log
        m
        cpufeatures
    )
    
    message(STATUS "Configured test-harness-arm64-v8a executable")
endif()

# Test harness for armeabi-v7a (optional, if supported)
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    add_executable(test-harness-armeabi-v7a
        test_harness_arm64.cpp  # Reuse arm64 test harness with different flags
        CpuFeatures.cpp
        ${TEST_HARNESS_COMMON_SOURCES}
    )
    
    target_include_directories(test-harness-armeabi-v7a PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/lsp-plugins/include
        ${CMAKE_CURRENT_SOURCE_DIR}/lsp-plugins/include/core
        ${CMAKE_CURRENT_SOURCE_DIR}/lsp-plugins/include/dsp
        ${ANDROID_NDK}/sources/android/cpufeatures
    )
    
    target_compile_definitions(test-harness-armeabi-v7a PRIVATE
        ANDROID
        ARCH_ARMV7
        USE_NEON=1
    )
    
    target_link_libraries(test-harness-armeabi-v7a PRIVATE
        log
        m
        cpufeatures
    )
    
    message(STATUS "Configured test-harness-armeabi-v7a executable")
endif()

message(STATUS "LSP Audio Engine Configuration:")
message(STATUS "  Version: ${PROJECT_VERSION}")
message(STATUS "  Build Type: ${CMAKE_BUILD_TYPE}")
message(STATUS "  Android ABI: ${ANDROID_ABI}")
