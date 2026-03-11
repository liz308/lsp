# JNI Bridge Testing on arm64-v8a Physical Devices

## Overview

This document describes the comprehensive test harness for validating the JNI bridge implementation on arm64-v8a physical Android devices. The test suite ensures that all JNI methods work correctly, parameters are passed safely across the JNI boundary, and no C++ exceptions leak into Java/Kotlin code.

## Test Requirements

### Hardware Requirements
- Physical Android device with arm64-v8a support
- Recommended devices:
  - Google Pixel 6 or later
  - Samsung Galaxy S21 or later
  - OnePlus 9 or later
  - Any device with Snapdragon 888+ or equivalent
- USB cable for adb connection
- USB debugging enabled

### Software Requirements
- Android SDK Platform Tools (adb)
- Gradle 8.7.2+
- Android NDK r25c or later
- Java 17
- Kotlin 2.1.0

### Minimum Android Version
- API 26 (Android 8.0 Oreo) for arm64-v8a builds
- API 21 (Android 5.0 Lollipop) minimum for x86_64

## Test Components

### 1. MainActivity Test Harness

**Location:** `android-app/src/main/java/com/example/lspandroid/MainActivity.kt`

The MainActivity provides a comprehensive UI-based test harness with the following features:

#### Test Categories

1. **Library Loading Test**
   - Verifies System.loadLibrary() succeeds
   - Confirms liblsp_audio_engine.so is loaded
   - Validates ABI selection (arm64-v8a)

2. **ABI Verification Test**
   - Checks device primary ABI
   - Verifies arm64-v8a support
   - Logs all supported ABIs

3. **Engine Initialization Test**
   - Tests nativeInitializeEngine() with valid parameters
   - Sample rate: 48000 Hz
   - Buffer size: 256 frames
   - Validates return value

4. **Parameter Setting Test**
   - Tests nativeSetMasterVolume()
   - Tests nativeSetEqualizerBand()
   - Tests nativeSetBypass()
   - Validates all return values

5. **Parameter Getting Test**
   - Tests nativeGetMasterVolume()
   - Tests nativeGetEqualizerBand()
   - Validates returned values are in expected ranges

6. **EQ Band Operations Test**
   - Tests all 10 EQ bands (0-9)
   - Tests multiple gain values: -12, -6, 0, 6, 12 dB
   - Validates set/get consistency
   - Ensures no data corruption

7. **Volume Control Test**
   - Tests volume range: 0.0 to 2.0
   - Tests intermediate values: 0.25, 0.5, 0.75, 1.0, 1.5
   - Validates precision (±0.01)

8. **Bypass Control Test**
   - Tests bypass enable/disable
   - Validates state transitions

9. **Exception Safety Test**
   - Tests invalid parameters (negative indices, out-of-range values)
   - Ensures no C++ exceptions cross JNI boundary
   - Validates graceful error handling

10. **Performance Test**
    - Measures JNI call overhead
    - 1000 iterations of set/get operations
    - Target: < 100 microseconds per call
    - Reports average call time

#### UI Features

- **Device Information Card**: Displays device model, Android version, and supported ABIs
- **Control Buttons**: Initialize engine, start/stop audio, run tests
- **Status Card**: Shows engine state and test execution status
- **Test Results**: Color-coded pass/fail indicators with detailed messages
- **Summary Card**: Overall test pass rate
- **Scrollable Results**: All test results with timing information

### 2. Automated Test Script

**Location:** `android-app/test_jni_bridge_arm64.sh`

Bash script for automated testing via adb:

#### Features

- Device detection and validation
- Automatic APK building with arm64-v8a ABI
- APK installation and permission granting
- Logcat monitoring for JNI activity
- Real-time test result reporting
- Color-coded console output

#### Usage

```bash
cd android-app
./test_jni_bridge_arm64.sh
```

#### Script Workflow

1. Check for adb availability
2. Detect connected devices
3. Verify arm64-v8a support
4. Build debug APK with arm64-v8a ABI filter
5. Verify APK contains arm64-v8a native library
6. Uninstall existing app (if any)
7. Install new APK
8. Grant required permissions
9. Clear logcat buffer
10. Launch MainActivity
11. Monitor logcat for test results
12. Display real-time logs

### 3. Native JNI Bridge Implementation

**Location:** `android-app/src/main/cpp/jni_bridge_minimal.cpp`

Minimal JNI bridge for testing:

#### Implemented Methods

```cpp
// Engine lifecycle
JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeInitializeEngine(
    JNIEnv *env, jobject thiz, jint sampleRate, jint bufferSize);

JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeStartEqTest(
    JNIEnv *env, jobject thiz);

JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeStopEqTest(
    JNIEnv *env, jobject thiz);

// EQ control
JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeSetEqualizerBand(
    JNIEnv *env, jobject thiz, jint bandIndex, jfloat gainDb);

JNIEXPORT jfloat JNICALL
Java_com_example_lspandroid_MainActivity_nativeGetEqualizerBand(
    JNIEnv *env, jobject thiz, jint bandIndex);

// Volume control
JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeSetMasterVolume(
    JNIEnv *env, jobject thiz, jfloat volume);

JNIEXPORT jfloat JNICALL
Java_com_example_lspandroid_MainActivity_nativeGetMasterVolume(
    JNIEnv *env, jobject thiz);

// Bypass control
JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeSetBypass(
    JNIEnv *env, jobject thiz, jboolean bypass);

// Status query
JNIEXPORT jboolean JNICALL
Java_com_example_lspandroid_MainActivity_nativeIsEngineRunning(
    JNIEnv *env, jobject thiz);

// Cleanup
JNIEXPORT void JNICALL
Java_com_example_lspandroid_MainActivity_nativeCleanup(
    JNIEnv *env, jobject thiz);
```

#### Exception Safety

All JNI methods follow these safety rules:

1. **No C++ exceptions across boundary**: All C++ code uses try-catch blocks
2. **Validate all parameters**: Check for null pointers, invalid indices, out-of-range values
3. **Return error codes**: Use boolean return values or sentinel values (0.0f, -1, etc.)
4. **Log errors**: Use Android log for debugging
5. **Graceful degradation**: Invalid operations return false/0 but don't crash

## Running Tests

### Method 1: Automated Script (Recommended)

```bash
# Connect arm64-v8a device via USB
# Enable USB debugging on device

cd android-app
./test_jni_bridge_arm64.sh
```

The script will:
- Build the APK
- Install on device
- Launch the app
- Monitor logcat for results

### Method 2: Manual Testing

```bash
# Build APK
cd android-app
../gradlew assembleDebug -Pandroid.injected.abi=arm64-v8a

# Install APK
adb install -r build/outputs/apk/debug/android-app-arm64-v8a-debug.apk

# Grant permissions
adb shell pm grant com.example.lspandroid.debug android.permission.RECORD_AUDIO
adb shell pm grant com.example.lspandroid.debug android.permission.MODIFY_AUDIO_SETTINGS

# Launch app
adb shell am start -n com.example.lspandroid.debug/com.example.lspandroid.MainActivity

# Monitor logs
adb logcat -s MainActivity:* AudioEngineJNI:*
```

Then use the app UI to run tests.

### Method 3: Android Studio

1. Open project in Android Studio
2. Connect arm64-v8a device
3. Select "app" run configuration
4. Click Run (Shift+F10)
5. Use app UI to run tests

## Expected Results

### Successful Test Run

All tests should pass with the following characteristics:

1. **Library Loading**: ✓ PASS
   - Message: "lsp_audio_engine library loaded successfully"

2. **ABI Verification**: ✓ PASS
   - Message: "Running on arm64-v8a as expected"

3. **Engine Initialization**: ✓ PASS
   - Message: "Engine initialized successfully"

4. **Parameter Setting**: ✓ PASS
   - Message: "Set master volume to 0.75"

5. **Parameter Getting**: ✓ PASS
   - Message: "Got master volume: 0.75" (or similar)

6. **EQ Band Operations**: ✓ PASS
   - Message: "All EQ band operations successful"

7. **Volume Control**: ✓ PASS
   - Message: "All volume operations successful"

8. **Bypass Control**: ✓ PASS
   - Message: "Bypass control working"

9. **Exception Safety**: ✓ PASS
   - Message: "No exceptions crossed JNI boundary"

10. **Performance Test**: ✓ PASS
    - Message: "Average JNI call time: <100μs"

### Summary

- **10 / 10 tests passed** (green card)
- No crashes or exceptions
- All operations complete within expected time

## Troubleshooting

### Library Loading Fails

**Symptom**: "Failed to load lsp_audio_engine library"

**Possible Causes**:
1. APK doesn't contain arm64-v8a library
2. Missing dependencies (libc++_shared.so)
3. ABI mismatch

**Solutions**:
```bash
# Verify APK contents
unzip -l build/outputs/apk/debug/*.apk | grep lib/arm64-v8a

# Check device ABI
adb shell getprop ro.product.cpu.abilist

# Rebuild with correct ABI
./gradlew clean assembleDebug -Pandroid.injected.abi=arm64-v8a
```

### Permission Denied Errors

**Symptom**: Audio operations fail

**Solution**:
```bash
# Grant permissions manually
adb shell pm grant com.example.lspandroid.debug android.permission.RECORD_AUDIO
adb shell pm grant com.example.lspandroid.debug android.permission.MODIFY_AUDIO_SETTINGS
```

### Device Not Detected

**Symptom**: "No devices connected"

**Solutions**:
1. Enable USB debugging in Developer Options
2. Accept USB debugging prompt on device
3. Check USB cable connection
4. Try different USB port
5. Install device drivers (Windows)

```bash
# Verify device connection
adb devices

# Restart adb server
adb kill-server
adb start-server
```

### Wrong ABI Warning

**Symptom**: "WARNING: Running on x86_64, not arm64-v8a"

**Cause**: Running on emulator instead of physical device

**Solution**: Use a physical arm64-v8a device for accurate testing

### Performance Test Fails

**Symptom**: "Average JNI call time: >100μs"

**Possible Causes**:
1. Device under heavy load
2. Debug build (not optimized)
3. Thermal throttling

**Solutions**:
1. Close background apps
2. Let device cool down
3. Test with release build
4. Adjust performance threshold

## Validation Checklist

Use this checklist to validate JNI bridge implementation:

- [ ] Library loads successfully on arm64-v8a device
- [ ] All JNI method signatures match between Java and C++
- [ ] Engine initializes with valid parameters
- [ ] Parameters can be set and retrieved correctly
- [ ] All 10 EQ bands work independently
- [ ] Volume control works across full range (0.0 - 2.0)
- [ ] Bypass control toggles correctly
- [ ] Invalid parameters don't cause crashes
- [ ] No C++ exceptions cross JNI boundary
- [ ] JNI call overhead is acceptable (< 100μs)
- [ ] No memory leaks during repeated operations
- [ ] App doesn't crash during lifecycle events
- [ ] Logcat shows expected messages
- [ ] All tests pass in automated test run

## Device-Specific Notes

### Google Pixel Devices

- Excellent arm64-v8a support
- Low-latency audio capabilities
- Recommended for testing

### Samsung Galaxy Devices

- May have Samsung-specific audio optimizations
- Test with both Exynos and Snapdragon variants
- Some models have audio processing restrictions

### OnePlus Devices

- Generally good performance
- May require OEM unlocking for some features

### Xiaomi/Redmi Devices

- MIUI may have additional permission restrictions
- Check MIUI security settings
- May need to disable battery optimization

## Performance Benchmarks

Expected performance on modern arm64-v8a devices:

| Operation | Target Time | Typical Time |
|-----------|-------------|--------------|
| Library Load | < 100ms | 20-50ms |
| Engine Init | < 50ms | 10-30ms |
| Set Parameter | < 100μs | 10-50μs |
| Get Parameter | < 100μs | 5-30μs |
| EQ Band Update | < 100μs | 15-60μs |

## Continuous Integration

For CI/CD pipelines:

```yaml
# Example GitHub Actions workflow
- name: Run JNI Bridge Tests
  run: |
    cd android-app
    ./test_jni_bridge_arm64.sh
  env:
    ANDROID_SERIAL: ${{ secrets.TEST_DEVICE_ID }}
```

## References

- [Android NDK Documentation](https://developer.android.com/ndk)
- [JNI Tips](https://developer.android.com/training/articles/perf-jni)
- [Android ABI Management](https://developer.android.com/ndk/guides/abis)
- [LSP Plugins Documentation](https://lsp-plug.in/)

## Maintenance

This test harness should be updated when:

1. New JNI methods are added
2. Method signatures change
3. New parameters are introduced
4. Performance requirements change
5. New device-specific issues are discovered

## Support

For issues or questions:

1. Check logcat output for detailed error messages
2. Verify device meets requirements
3. Review troubleshooting section
4. Check GitHub issues for similar problems
5. Create new issue with device info and logs
