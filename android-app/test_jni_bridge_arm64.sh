#!/bin/bash

# JNI Bridge Test Script for arm64-v8a Physical Devices
# This script automates testing of the JNI bridge on physical Android devices

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PACKAGE_NAME="com.example.lspandroid.debug"
ACTIVITY_NAME="com.example.lspandroid.MainActivity"
APK_PATH="build/outputs/apk/debug"
TEST_TIMEOUT=300  # 5 minutes

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}JNI Bridge Test for arm64-v8a Devices${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to print section headers
print_section() {
    echo ""
    echo -e "${BLUE}>>> $1${NC}"
    echo ""
}

# Function to print success messages
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print error messages
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Function to print warning messages
print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Check if adb is available
if ! command -v adb &> /dev/null; then
    print_error "adb not found. Please install Android SDK platform tools."
    exit 1
fi

print_success "adb found"

# Check for connected devices
print_section "Checking for connected devices"
DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    print_error "No devices connected. Please connect an arm64-v8a device via USB."
    exit 1
fi

if [ "$DEVICES" -gt 1 ]; then
    print_warning "Multiple devices connected. Using first device."
    print_warning "To specify a device, use: export ANDROID_SERIAL=<device_id>"
fi

print_success "Found $DEVICES device(s)"

# Get device information
print_section "Device Information"
DEVICE_MODEL=$(adb shell getprop ro.product.model | tr -d '\r')
DEVICE_MANUFACTURER=$(adb shell getprop ro.product.manufacturer | tr -d '\r')
ANDROID_VERSION=$(adb shell getprop ro.build.version.release | tr -d '\r')
SDK_VERSION=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
CPU_ABI=$(adb shell getprop ro.product.cpu.abi | tr -d '\r')
CPU_ABI2=$(adb shell getprop ro.product.cpu.abilist | tr -d '\r')

echo "Device: $DEVICE_MANUFACTURER $DEVICE_MODEL"
echo "Android Version: $ANDROID_VERSION (API $SDK_VERSION)"
echo "Primary ABI: $CPU_ABI"
echo "Supported ABIs: $CPU_ABI2"

# Verify arm64-v8a support
if [[ ! "$CPU_ABI2" =~ "arm64-v8a" ]]; then
    print_error "Device does not support arm64-v8a architecture"
    print_error "This test requires an arm64-v8a device"
    exit 1
fi

if [ "$CPU_ABI" != "arm64-v8a" ]; then
    print_warning "Primary ABI is $CPU_ABI, not arm64-v8a"
    print_warning "Test will still run but may use non-arm64 libraries"
fi

print_success "Device supports arm64-v8a"

# Build the APK
print_section "Building APK"
cd "$(dirname "$0")/.."

if [ ! -f "gradlew" ]; then
    print_error "gradlew not found. Please run this script from the android-app directory."
    exit 1
fi

print_warning "Building debug APK with arm64-v8a ABI..."
./gradlew assembleDebug -Pandroid.injected.abi=arm64-v8a

if [ $? -ne 0 ]; then
    print_error "Build failed"
    exit 1
fi

print_success "Build completed"

# Find the APK
APK_FILE=$(find "$APK_PATH" -name "*arm64-v8a*.apk" | head -n 1)

if [ -z "$APK_FILE" ]; then
    # Fallback to universal APK
    APK_FILE=$(find "$APK_PATH" -name "*.apk" | head -n 1)
fi

if [ -z "$APK_FILE" ] || [ ! -f "$APK_FILE" ]; then
    print_error "APK not found in $APK_PATH"
    exit 1
fi

print_success "Found APK: $APK_FILE"

# Verify APK contains arm64-v8a library
print_section "Verifying APK Contents"
if command -v unzip &> /dev/null; then
    if unzip -l "$APK_FILE" | grep -q "lib/arm64-v8a/liblsp_audio_engine.so"; then
        print_success "APK contains arm64-v8a native library"
    else
        print_warning "arm64-v8a library not found in APK"
        echo "APK contents:"
        unzip -l "$APK_FILE" | grep "lib/"
    fi
fi

# Uninstall existing app
print_section "Preparing Device"
print_warning "Uninstalling existing app..."
adb uninstall "$PACKAGE_NAME" 2>/dev/null || true

# Install the APK
print_warning "Installing APK..."
adb install -r "$APK_FILE"

if [ $? -ne 0 ]; then
    print_error "Installation failed"
    exit 1
fi

print_success "APK installed successfully"

# Grant permissions
print_section "Granting Permissions"
adb shell pm grant "$PACKAGE_NAME" android.permission.RECORD_AUDIO 2>/dev/null || print_warning "Could not grant RECORD_AUDIO permission"
adb shell pm grant "$PACKAGE_NAME" android.permission.MODIFY_AUDIO_SETTINGS 2>/dev/null || print_warning "Could not grant MODIFY_AUDIO_SETTINGS permission"
print_success "Permissions granted"

# Clear logcat
print_section "Preparing Logcat"
adb logcat -c
print_success "Logcat cleared"

# Start the activity
print_section "Launching Application"
adb shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME"

if [ $? -ne 0 ]; then
    print_error "Failed to start activity"
    exit 1
fi

print_success "Activity started"

# Wait for app to initialize
sleep 2

# Monitor logcat for test results
print_section "Monitoring Test Execution"
echo "Watching logcat for JNI bridge activity..."
echo "Press Ctrl+C to stop monitoring"
echo ""

# Create a temporary file for logcat output
LOGCAT_FILE=$(mktemp)
trap "rm -f $LOGCAT_FILE" EXIT

# Start logcat in background
adb logcat -s MainActivity:* AudioEngineJNI:* > "$LOGCAT_FILE" &
LOGCAT_PID=$!

# Monitor for specific log messages
echo "Monitoring for:"
echo "  - Library loading"
echo "  - JNI method calls"
echo "  - Engine initialization"
echo "  - Parameter updates"
echo "  - Errors and exceptions"
echo ""

# Wait and display relevant logs
sleep 5

# Check for library loading
if grep -q "Successfully loaded lsp_audio_engine library" "$LOGCAT_FILE"; then
    print_success "Native library loaded successfully"
else
    if grep -q "Failed to load lsp_audio_engine library" "$LOGCAT_FILE"; then
        print_error "Failed to load native library"
        grep "Failed to load" "$LOGCAT_FILE"
    else
        print_warning "Library loading status unknown"
    fi
fi

# Check for device info logging
if grep -q "Device Information" "$LOGCAT_FILE"; then
    print_success "Device information logged"
    grep -A 10 "Device Information" "$LOGCAT_FILE" | head -n 11
fi

# Display recent logs
echo ""
echo "Recent log output:"
echo "---"
tail -n 50 "$LOGCAT_FILE"
echo "---"

# Keep monitoring
echo ""
echo "Continuing to monitor logcat..."
echo "The app is now running on the device."
echo "Please interact with the app to run tests."
echo ""
echo "To view full logs, run:"
echo "  adb logcat -s MainActivity:* AudioEngineJNI:*"
echo ""
echo "To stop monitoring, press Ctrl+C"

# Continue monitoring until interrupted
tail -f "$LOGCAT_FILE" &
TAIL_PID=$!

# Wait for user interrupt
wait $TAIL_PID 2>/dev/null

# Cleanup
kill $LOGCAT_PID 2>/dev/null || true
kill $TAIL_PID 2>/dev/null || true

print_section "Test Complete"
print_success "JNI bridge test execution finished"
echo ""
echo "To run tests again:"
echo "  1. Tap 'Run All Tests' button in the app"
echo "  2. Or re-run this script: ./test_jni_bridge_arm64.sh"
echo ""
