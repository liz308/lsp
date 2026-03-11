#!/bin/bash

# JNI Bridge Architecture Verification Script
# 
# This script verifies that the JNI bridge works correctly across all
# supported Android architectures: arm64-v8a, x86_64, and armeabi-v7a.
#
# Usage:
#   ./verify_jni_bridge_architectures.sh [architecture]
#
# Arguments:
#   architecture - Optional. Specific architecture to test (arm64-v8a, x86_64, armeabi-v7a)
#                  If not specified, tests all available architectures.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if adb is available
check_adb() {
    if ! command -v adb &> /dev/null; then
        log_error "adb not found. Please install Android SDK platform-tools."
        exit 1
    fi
    
    # Check if device is connected
    if ! adb devices | grep -q "device$"; then
        log_error "No Android device connected. Please connect a device or start an emulator."
        exit 1
    fi
    
    log_success "adb found and device connected"
}

# Get device architecture
get_device_architecture() {
    local abi=$(adb shell getprop ro.product.cpu.abi | tr -d '\r')
    echo "$abi"
}

# Get all supported ABIs on device
get_supported_abis() {
    local abis=$(adb shell getprop ro.product.cpu.abilist | tr -d '\r')
    echo "$abis"
}

# Build the project for specific architecture
build_for_architecture() {
    local arch=$1
    log_info "Building for architecture: $arch"
    
    cd "$PROJECT_ROOT"
    
    # Build the specific ABI
    ./gradlew :android-app:assembleDebug -Pandroid.injected.abi="$arch"
    
    if [ $? -eq 0 ]; then
        log_success "Build successful for $arch"
        return 0
    else
        log_error "Build failed for $arch"
        return 1
    fi
}

# Run instrumented tests
run_instrumented_tests() {
    local arch=$1
    log_info "Running instrumented tests for architecture: $arch"
    
    cd "$PROJECT_ROOT"
    
    # Run the JNI bridge architecture tests
    ./gradlew :android-app:connectedDebugAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.class=com.example.lspandroid.JniBridgeArchitectureTest
    
    if [ $? -eq 0 ]; then
        log_success "Tests passed for $arch"
        return 0
    else
        log_error "Tests failed for $arch"
        return 1
    fi
}

# Extract test results from logcat
extract_test_results() {
    log_info "Extracting test results from logcat..."
    
    # Clear logcat
    adb logcat -c
    
    # Wait a moment for tests to run
    sleep 2
    
    # Extract JNI bridge test logs
    adb logcat -d -s JNIBridgeArchTest:* | grep -E "\[PASS\]|\[FAIL\]|Architecture:"
}

# Verify library loading
verify_library_loading() {
    local arch=$1
    log_info "Verifying library loading for $arch"
    
    # Install the APK
    cd "$PROJECT_ROOT"
    ./gradlew :android-app:installDebug -Pandroid.injected.abi="$arch"
    
    if [ $? -ne 0 ]; then
        log_error "Failed to install APK for $arch"
        return 1
    fi
    
    # Check if the library exists in the APK
    local apk_path="android-app/build/outputs/apk/debug/android-app-debug.apk"
    if [ -f "$apk_path" ]; then
        local lib_path="lib/$arch/liblsp_audio_engine.so"
        if unzip -l "$apk_path" | grep -q "$lib_path"; then
            log_success "Library found in APK: $lib_path"
            return 0
        else
            log_error "Library not found in APK: $lib_path"
            return 1
        fi
    else
        log_error "APK not found: $apk_path"
        return 1
    fi
}

# Test specific architecture
test_architecture() {
    local arch=$1
    local device_arch=$2
    
    echo ""
    log_info "=========================================="
    log_info "Testing Architecture: $arch"
    log_info "Device Architecture: $device_arch"
    log_info "=========================================="
    
    # Check if architecture is supported by device
    local supported_abis=$(get_supported_abis)
    if [[ ! "$supported_abis" =~ "$arch" ]]; then
        log_warning "Architecture $arch not supported by device (supports: $supported_abis)"
        return 2
    fi
    
    # Build for architecture
    if ! build_for_architecture "$arch"; then
        return 1
    fi
    
    # Verify library loading
    if ! verify_library_loading "$arch"; then
        return 1
    fi
    
    # Run instrumented tests
    if ! run_instrumented_tests "$arch"; then
        return 1
    fi
    
    # Extract and display results
    extract_test_results
    
    log_success "All tests passed for $arch"
    return 0
}

# Main function
main() {
    local target_arch=$1
    
    log_info "JNI Bridge Architecture Verification"
    log_info "======================================"
    
    # Check prerequisites
    check_adb
    
    # Get device information
    local device_arch=$(get_device_architecture)
    local supported_abis=$(get_supported_abis)
    
    log_info "Device primary ABI: $device_arch"
    log_info "Device supported ABIs: $supported_abis"
    
    # Determine which architectures to test
    local architectures=()
    if [ -n "$target_arch" ]; then
        architectures=("$target_arch")
        log_info "Testing specific architecture: $target_arch"
    else
        # Test all supported architectures
        if [[ "$supported_abis" =~ "arm64-v8a" ]]; then
            architectures+=("arm64-v8a")
        fi
        if [[ "$supported_abis" =~ "x86_64" ]]; then
            architectures+=("x86_64")
        fi
        if [[ "$supported_abis" =~ "armeabi-v7a" ]]; then
            architectures+=("armeabi-v7a")
        fi
        
        if [ ${#architectures[@]} -eq 0 ]; then
            log_error "No supported architectures found on device"
            exit 1
        fi
        
        log_info "Testing all supported architectures: ${architectures[*]}"
    fi
    
    # Test each architecture
    local total=0
    local passed=0
    local failed=0
    local skipped=0
    
    for arch in "${architectures[@]}"; do
        total=$((total + 1))
        test_architecture "$arch" "$device_arch"
        local result=$?
        
        if [ $result -eq 0 ]; then
            passed=$((passed + 1))
        elif [ $result -eq 2 ]; then
            skipped=$((skipped + 1))
        else
            failed=$((failed + 1))
        fi
    done
    
    # Print summary
    echo ""
    log_info "=========================================="
    log_info "Test Summary"
    log_info "=========================================="
    log_info "Total architectures tested: $total"
    log_success "Passed: $passed"
    if [ $failed -gt 0 ]; then
        log_error "Failed: $failed"
    fi
    if [ $skipped -gt 0 ]; then
        log_warning "Skipped: $skipped"
    fi
    
    if [ $failed -gt 0 ]; then
        log_error "Some tests failed"
        exit 1
    else
        log_success "All tests passed!"
        exit 0
    fi
}

# Run main function
main "$@"
