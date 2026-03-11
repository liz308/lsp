# JNI Bridge Manual Test Guide

Quick reference for manually testing the JNI bridge on arm64-v8a physical devices.

## Prerequisites

- Physical Android device with arm64-v8a support
- USB cable
- USB debugging enabled
- adb installed

## Quick Start (5 minutes)

### 1. Connect Device

```bash
# Enable USB debugging on device:
# Settings > Developer Options > USB Debugging

# Verify connection
adb devices
```

Expected output:
```
List of devices attached
ABC123XYZ    device
```

### 2. Run Automated Test

```bash
cd android-app
./test_jni_bridge_arm64.sh
```

The script will:
- Build APK
- Install on device
- Launch app
- Monitor logs

### 3. Use App UI

On the device screen:

1. Tap **"Run All Tests"** button
2. Wait for tests to complete (10-15 seconds)
3. Check results:
   - Green cards = PASS ✓
   - Red cards = FAIL ✗
4. Expected: **10 / 10 tests passed**

## Manual Test Steps

If you prefer manual testing:

### Step 1: Build and Install

```bash
cd android-app
../gradlew assembleDebug -Pandroid.injected.abi=arm64-v8a
adb install -r build/outputs/apk/debug/*arm64*.apk
```

### Step 2: Grant Permissions

```bash
adb shell pm grant com.example.lspandroid.debug android.permission.RECORD_AUDIO
adb shell pm grant com.example.lspandroid.debug android.permission.MODIFY_AUDIO_SETTINGS
```

### Step 3: Launch App

```bash
adb shell am start -n com.example.lspandroid.debug/com.example.lspandroid.MainActivity
```

### Step 4: Monitor Logs

```bash
adb logcat -s MainActivity:* AudioEngineJNI:*
```

### Step 5: Test in App

On device:

1. **Initialize Engine**
   - Tap "Initialize Engine"
   - Status should show "Initialized: Yes"

2. **Start Audio**
   - Tap "Start Audio"
   - Status should show "Audio Running: Yes"

3. **Run All Tests**
   - Tap "Run All Tests"
   - Wait for completion
   - Check results

4. **Stop Audio**
   - Tap "Stop Audio"
   - Status should show "Audio Running: No"

## Expected Log Output

### Successful Library Load

```
I/MainActivity: Successfully loaded lsp_audio_engine library
I/MainActivity: === Device Information ===
I/MainActivity: Device: Google Pixel 6
I/MainActivity: Android Version: 13 (API 33)
I/MainActivity: Supported ABIs: arm64-v8a, armeabi-v7a
I/MainActivity: Primary ABI: arm64-v8a
```

### Successful Engine Init

```
I/AudioEngineJNI: nativeInitializeEngine called: sampleRate=48000, bufferSize=256
```

### Successful Parameter Updates

```
I/AudioEngineJNI: nativeSetEqualizerBand: band=0, gain=6.00 dB
I/AudioEngineJNI: nativeSetMasterVolume: volume=0.75
I/AudioEngineJNI: nativeSetBypass: bypass=0
```

## Troubleshooting Quick Fixes

### Library Not Loading

```bash
# Check APK contents
unzip -l build/outputs/apk/debug/*.apk | grep arm64

# Should see:
# lib/arm64-v8a/liblsp_audio_engine.so
```

### Device Not Found

```bash
# Restart adb
adb kill-server
adb start-server
adb devices
```

### Permission Issues

```bash
# Force grant permissions
adb shell pm grant com.example.lspandroid.debug android.permission.RECORD_AUDIO
adb shell pm grant com.example.lspandroid.debug android.permission.MODIFY_AUDIO_SETTINGS
```

### App Crashes

```bash
# View crash logs
adb logcat -s AndroidRuntime:E

# View native crashes
adb logcat -s DEBUG:*
```

## Test Checklist

Quick validation checklist:

- [ ] Device is arm64-v8a
- [ ] Library loads without error
- [ ] Engine initializes successfully
- [ ] Can set master volume
- [ ] Can get master volume
- [ ] Can set EQ bands (0-9)
- [ ] Can get EQ bands (0-9)
- [ ] Can toggle bypass
- [ ] Can start/stop audio
- [ ] No crashes with invalid parameters
- [ ] All automated tests pass

## Performance Check

Expected performance on modern devices:

```
Average JNI call time: 10-50μs  ✓ Good
Average JNI call time: 50-100μs ⚠ Acceptable
Average JNI call time: >100μs   ✗ Poor (investigate)
```

## Common Issues

### Issue: "Running on x86_64, not arm64-v8a"

**Cause**: Using emulator instead of physical device

**Fix**: Use physical device

### Issue: Tests fail with "Engine not initialized"

**Cause**: Initialization failed

**Fix**: 
1. Check logcat for errors
2. Verify permissions granted
3. Try different sample rate/buffer size

### Issue: High JNI call overhead (>100μs)

**Cause**: Device under load or thermal throttling

**Fix**:
1. Close background apps
2. Let device cool down
3. Restart device

## Quick Commands Reference

```bash
# Build
./gradlew assembleDebug -Pandroid.injected.abi=arm64-v8a

# Install
adb install -r build/outputs/apk/debug/*.apk

# Launch
adb shell am start -n com.example.lspandroid.debug/com.example.lspandroid.MainActivity

# Logs
adb logcat -s MainActivity:* AudioEngineJNI:*

# Uninstall
adb uninstall com.example.lspandroid.debug

# Device info
adb shell getprop ro.product.cpu.abilist
```

## Automated Testing

Run instrumented tests:

```bash
# Run all tests
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.lspandroid.JniBridgeInstrumentedTest

# View test results
open android-app/build/reports/androidTests/connected/index.html
```

## Success Criteria

✓ All 10 tests pass in UI
✓ No crashes or exceptions
✓ JNI call time < 100μs
✓ No errors in logcat
✓ App responds to all controls

## Time Estimates

- Automated test script: **5 minutes**
- Manual testing: **10 minutes**
- Instrumented tests: **3 minutes**
- Full validation: **15 minutes**

## Next Steps

After successful testing:

1. Document any device-specific issues
2. Update test results in task tracking
3. Test on additional devices if available
4. Run performance benchmarks
5. Proceed to next task in spec

## Support

If tests fail:

1. Check this guide's troubleshooting section
2. Review full documentation: `jni-bridge-testing.md`
3. Check logcat for detailed errors
4. Verify device meets requirements
5. Report issues with device info and logs
