# ABI Selection Testing Documentation

## Overview

This document describes the testing strategy for verifying that `System.loadLibrary()` correctly selects the appropriate ABI-specific native library for each Android device architecture.

## Requirements

This testing addresses the following requirements:

- **Requirement 1.5 AC 15**: The Plugin_Host SHALL verify native library loading succeeds for all architectures using System.loadLibrary() with proper ABI selection
- **Requirement 2**: Android Architecture Support (arm64-v8a, x86_64, armeabi-v7a)
- **Requirement 2 AC 7**: Separate shared libraries per architecture with architecture-specific optimizations

## Test Implementation

### Location

`android-app/src/androidTest/java/com/example/lspandroid/JniBridgeArchitectureTest.kt`

### Test Cases

#### 1. testSystemLoadLibraryWithAbiSelection()

**Purpose**: Verifies that System.loadLibrary() correctly selects and loads the native library for the current device architecture.

**What it tests**:
- Library loads successfully without UnsatisfiedLinkError
- Native methods can be called through JNI bridge
- Architecture detection matches device's primary ABI
- Library path contains correct ABI directory

**Expected behavior**:
- On arm64-v8a devices: Loads `liblsp_audio_engine.so` from arm64-v8a directory
- On x86_64 emulators: Loads `liblsp_audio_engine.so` from x86_64 directory
- On armeabi-v7a devices: Loads `liblsp_audio_engine.so` from armeabi-v7a directory

#### 2. testLibraryLoadedFromCorrectAbiDirectory()

**Purpose**: Verifies that the native library was loaded from the correct ABI-specific directory in the APK.

**What it tests**:
- Native library directory path contains the correct ABI name
- Library file exists at the expected location
- Library file is readable
- Library file size is reasonable (not empty)

**Expected directory structure**:
```
/data/app/<package>/lib/<abi>/liblsp_audio_engine.so
```

Where `<abi>` is one of:
- `arm64-v8a` (primary target for modern devices)
- `x86_64` (emulators)
- `armeabi-v7a` (legacy devices)

#### 3. testAllSupportedAbisHaveLibraries()

**Purpose**: Verifies that the primary ABI library exists and logs information about ABI splits.

**What it tests**:
- Primary ABI library exists and is accessible
- Logs information about device's supported ABIs
- Acknowledges that secondary ABIs may not be present due to ABI splits

**Note**: With ABI splits enabled, only the primary ABI library will be present in the installed APK. This is expected and correct behavior for optimizing APK size.

#### 4. testLibraryLoadingFailsGracefullyForMissingAbi()

**Purpose**: Verifies that attempting to load a nonexistent library fails gracefully with an appropriate error.

**What it tests**:
- UnsatisfiedLinkError is thrown for missing libraries
- Error message is informative and mentions the library name
- Error handling doesn't crash the application

## ABI Selection Mechanism

### How System.loadLibrary() Works

1. **ABI Priority**: Android selects libraries based on `Build.SUPPORTED_ABIS` array
   - Index 0: Primary ABI (preferred)
   - Index 1+: Secondary ABIs (fallback)

2. **Library Search Path**:
   ```
   /data/app/<package>/lib/<primary_abi>/
   /data/app/<package>/lib/<secondary_abi>/
   ...
   ```

3. **Selection Process**:
   - Searches for library in primary ABI directory first
   - Falls back to secondary ABIs if not found
   - Throws UnsatisfiedLinkError if library not found in any ABI directory

### ABI Splits Configuration

The app uses ABI splits to generate separate APKs per architecture:

```kotlin
splits {
    abi {
        isEnable = true
        reset()
        include("arm64-v8a", "x86_64", "armeabi-v7a")
        isUniversalApk = false
    }
}
```

**Benefits**:
- Smaller APK size (only includes one ABI per APK)
- Faster downloads for users
- Reduced storage requirements on device

**Version Code Strategy**:
- arm64-v8a: base version code (e.g., 120)
- x86_64: base + 1 (e.g., 121)
- armeabi-v7a: base + 2 (e.g., 122)

## Running the Tests

### Prerequisites

- Android device or emulator with API level 26+
- Connected via ADB
- App installed with appropriate ABI

### Run All Architecture Tests

```bash
./gradlew connectedAndroidTest
```

### Run Specific ABI Selection Tests

```bash
# Run only ABI selection tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.lspandroid.JniBridgeArchitectureTest

# Run specific test method
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.lspandroid.JniBridgeArchitectureTest#testSystemLoadLibraryWithAbiSelection
```

### Test on Specific Architecture

```bash
# Build and test on arm64-v8a device
./gradlew assembleDebug -Pandroid.injected.abi=arm64-v8a
./gradlew connectedAndroidTest

# Build and test on x86_64 emulator
./gradlew assembleDebug -Pandroid.injected.abi=x86_64
./gradlew connectedAndroidTest
```

## Expected Test Output

### Successful Test Run

```
I/JniBridgeArchTest: === Testing System.loadLibrary() ABI Selection ===
I/JniBridgeArchTest: Current architecture: Architecture: arm64-v8a
                     SIMD Support: NEON
I/JniBridgeArchTest: Device primary ABI: arm64-v8a
I/JniBridgeArchTest: All supported ABIs: arm64-v8a, armeabi-v7a
I/JniBridgeArchTest: ✓ Correctly loaded arm64-v8a library
I/JniBridgeArchTest: Library path: /data/app/.../lib/arm64-v8a
I/JniBridgeArchTest: ✓ Library loaded from correct ABI directory: arm64-v8a
I/JniBridgeArchTest: ✓ Successfully called native methods through JNI bridge
I/JniBridgeArchTest: === System.loadLibrary() ABI Selection Test PASSED ===
```

### Test Failure Scenarios

#### Library Not Found
```
E/JniBridgeArchTest: Failed to load lsp_audio_engine library
java.lang.UnsatisfiedLinkError: dlopen failed: library "liblsp_audio_engine.so" not found
```

**Cause**: Native library not built or not included in APK
**Solution**: Run `./gradlew externalNativeBuildDebug` to build native libraries

#### Wrong ABI
```
E/JniBridgeArchTest: Architecture mismatch: expected arm64-v8a, got x86_64
```

**Cause**: Wrong APK installed for device architecture
**Solution**: Build correct ABI variant or use universal APK

## Verification Checklist

- [ ] Test passes on arm64-v8a physical device (Pixel, Samsung Galaxy)
- [ ] Test passes on x86_64 emulator (Android Studio AVD)
- [ ] Test passes on armeabi-v7a device (if supported)
- [ ] Library loads from correct ABI directory
- [ ] Native methods callable through JNI bridge
- [ ] Architecture detection matches device ABI
- [ ] Error handling works for missing libraries
- [ ] ABI splits generate correct APK variants

## Troubleshooting

### Library Not Loading

1. **Check library exists**:
   ```bash
   adb shell run-as com.example.lspandroid.debug ls -la /data/data/com.example.lspandroid.debug/lib
   ```

2. **Check library dependencies**:
   ```bash
   adb pull /data/app/.../lib/arm64-v8a/liblsp_audio_engine.so
   readelf -d liblsp_audio_engine.so
   ```

3. **Check logcat for errors**:
   ```bash
   adb logcat | grep -E "(JniBridgeArchTest|UnsatisfiedLinkError|dlopen)"
   ```

### Wrong ABI Selected

1. **Check device ABIs**:
   ```bash
   adb shell getprop ro.product.cpu.abilist
   ```

2. **Check installed APK ABI**:
   ```bash
   adb shell pm path com.example.lspandroid.debug
   adb pull <path> app.apk
   unzip -l app.apk | grep "lib/"
   ```

3. **Verify build configuration**:
   - Check `build.gradle.kts` splits configuration
   - Verify CMakeLists.txt builds all ABIs
   - Confirm no conflicting ndk.abiFilters

## Related Documentation

- [JNI Bridge Architecture Verification](jni-bridge-architecture-verification.md)
- [Architecture-Specific Optimizations](arm64-v8a-optimization-verification.md)
- [Build System Configuration](../build.gradle.kts)
- [CMake Configuration](../src/main/cpp/CMakeLists.txt)

## References

- Android NDK Documentation: https://developer.android.com/ndk/guides/abis
- System.loadLibrary() Documentation: https://developer.android.com/reference/java/lang/System#loadLibrary(java.lang.String)
- ABI Management: https://developer.android.com/ndk/guides/abi-management
