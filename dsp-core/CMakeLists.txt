cmake_minimum_required(VERSION 3.22)

project(lsp_android_dsp_core
    VERSION 0.1.0
    LANGUAGES C CXX)

# ------------------------------------------------------------------------------
# Android NDK configuration
# ------------------------------------------------------------------------------
if(NOT ANDROID)
    message(WARNING "This CMake configuration is intended to be used from an Android NDK/Gradle build.")
endif()

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Optimisations suitable for real-time audio DSP.
add_compile_options(
    -O3
)

# We do not allow C++ exceptions to cross the C/JNI boundary. The DSP core
# itself may still use exceptions internally where upstream does so, but the
# bridge interfaces are compiled with exceptions disabled.
add_compile_options(
    $<$<COMPILE_LANGUAGE:CXX>:-fno-exceptions>
)

# ------------------------------------------------------------------------------
# LSP plugin sources
#
# NOTE: The actual lsp-plugins source tree is present under external/lsp-plugins.
# We do NOT modify DSP algorithm files here; only build glue and portability
# macros are allowed.
# ------------------------------------------------------------------------------

set(LSP_PLUGINS_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../external/lsp-plugins")

if(NOT EXISTS "${LSP_PLUGINS_ROOT}/CMakeLists.txt")
    message(FATAL_ERROR "lsp-plugins source not found at ${LSP_PLUGINS_ROOT}. Clone https://github.com/lsp-plugins/lsp-plugins.git into external/lsp-plugins.")
endif()
# Ported LSP plugins core source files - minimal set
set(LSP_CORE_SOURCES
)

# Bridge header and any Android-specific glue lives here.
set(BRIDGE_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/lsp_android_bridge_minimal.cpp
)

add_library(lsp_android_dsp_core SHARED
    ${LSP_CORE_SOURCES}
    ${BRIDGE_SOURCES}
)

target_include_directories(lsp_android_dsp_core
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${LSP_PLUGINS_ROOT}/include
)

target_compile_definitions(lsp_android_dsp_core
    PUBLIC
        LSP_ANDROID_BUILD=1
        # Add any portability macros here to suppress Linux-only headers in
        # host UI/audio code while keeping DSP code intact.
)

# ------------------------------------------------------------------------------
# Test harness executables (native, no JNI)
# ------------------------------------------------------------------------------

# Test harness executables disabled for now
# add_executable(lsp_eq_test_harness ...)
# add_executable(lsp_native_test_harness ...)
