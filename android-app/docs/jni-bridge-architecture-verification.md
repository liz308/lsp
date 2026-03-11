# JNI Bridge Architecture Verification

This document describes the verification process for ensuring the JNI bridge works correctly across all supported Android architectures.

## Overview

The JNI bridge must work correctly on:
- **arm64-v8a**: 64-bit ARM (primary architecture for modern Android devices)
- **x86_64**: 64-bit Intel (Android emulators and Intel-based devices)
- **armeabi-v7a**: 32-bit ARM (legacy devices, optional support)

## Requirements Verified

### Requirement 1.5: Android NDK and ABI Compatibility
- NDK r25c or later compilation
- API level 21 (Lollipop) for arm64-v8a and x86_64
- API level 16 (Jelly Bean) for armeabi-v7a
- libc++ (LLVM) C++ standard library
- Android NDK unified headers
- Android log library linking

### Requirement 2: Android Architecture Support
- arm64-v8a compilation with NEON SIMD
- x86_64 compilation with SSE2
- armeabi-v7a compilation with runtime NEON detection
- Architecture-specific compiler flags
- Separate shared libraries per ABI

### Requirement 6: JNI Bridge Interface
- C-linkage API functions
- plugin_create(), plugin_destroy(), set_param(), get_param(), process()
- Error code returns
- No C++ exceptions crossing JNI boundary

## Test Components

### 1. Native Test Suite (`test_jni_bridge_architectures.cpp`)

C++ test suite that runs natively on the device and verifies:

#### Test 1: Architecture Detection
- Detects current architecture (arm64-v8a, x86_64, armeabi-v7a)
- Verifies SIMD capabilities (NEON for ARM, SSE2 for x86_64)
- Confirms architecture-specific optimizations are active

#### Test 2: Bridge API Version
- Verifies bridge API version matches expected version
- Ensures ABI compatibility

#### Test 3: Library Initialization
- Tests `lsp_android_initialize()` function
- Verifies successful initialization

#### Test 4: Plugin Creation
- Tests plugin creation functions
- Verifies no C++ exceptions cross JNI boundary
- Tests all plugin types (EQ, compressor, limiter, gate, analyzer)

#### Test 5: Plugin Initialization
- Tests plugin initialization with sample rate and buffer size
- Tests plugin activation
- Verifies proper lifecycle management

#### Test 6: Port Information
- Tests port count retrieval
- Tests port descriptor retrieval
- Verifies metadata is accessible

#### Test 7: Parameter Operations
- Tests `set_param()` and `get_param()` functions
- Verifies parameter values are correctly set and retrieved
- Tests parameter value accuracy

#### Test 8: Audio Processing
- Tests `process()` function with real audio buffers
- Generates test signal (1kHz sine wave)
- Verifies output is not all zeros
- Confirms audio processing works

#### Test 9: Multiple Plugin Types
- Tests creation of all plugin types
- Verifies each plugin type can be instantiated
- Tests plugin destruction

#### Test 10: Library Cleanup
- Tests `lsp_android_cleanup()` function
- Verifies proper resource cleanup

### 2. Kotlin Instrumented Tests (`JniBridgeArchitectureTest.kt`)

Android instrumented tests that run on device/emulator:

#### Test: System.loadLibrary() with ABI Selection
- Verifies `System.loadLibrary("lsp_audio_engine")` succeeds
- Confirms correct ABI-specific library is loaded
- Tests on arm64-v8a physical devices
- Tests on x86_64 emulators
- Tests on armeabi-v7a legacy devices (if supported)

#### Test: No Exceptions Cross JNI Boundary
- Verifies all native calls return error codes instead of throwing exceptions
- Checks for critical failures indicating exception leakage
- Ensures robust error handling

#### Test: Architecture-Specific Optimizations
- Verifies NEON is active on arm64-v8a
- Verifies SSE2 is active on x86_64
- Checks SIMD support on armeabi-v7a

#### Test: Plugin Lifecycle Functions
- Verifies plugin creation, initialization, and cleanup
- Tests complete lifecycle through JNI bridge

#### Test: Parameter Operations
- Verifies parameter set/get operations work correctly
- Tests parameter value accuracy

#### Test: Audio Processing
- Verifies audio processing works through JNI bridge
- Tests real-time audio buffer processing

#### Test: Multiple Plugin Types
- Verifies all plugin types can be created
- Tests plugin type enumeration

### 3. Verification Script (`verify_jni_bridge_architectures.sh`)

Automated shell script that:
- Detects connected Android device or emulator
- Identifies device architecture and supported ABIs
- Builds project for each supported architecture
- Installs APK with architecture-specific library
- Runs instrumented tests
- Extracts and displays test results
- Generates test summary report

## Running the Tests

### Option 1: Run All Architectures (Automated)

```bash
cd android-app/src/main/cpp
./verify_jni_bridge_architectures.sh
```

This will:
1. Detect device/emulator
2. Test all supported architectures
3. Generate comprehensive report

### Option 2: Test Specific Architecture

```bash
# Test arm64-v8a on physical device
./verify_jni_bridge_architectures.sh arm64-v8a

# Test x86_64 on emulator
./verify_jni_bridge_architectures.sh x86_64

# Test armeabi-v7a on legacy device
./verify_jni_bridge_architectures.sh armeabi-v7a
```

### Option 3: Run Kotlin Tests Directly

```bash
# Run all JNI bridge tests
./gradlew :android-app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.example.lspandroid.JniBridgeArchitectureTest

# Run specific test
./gradlew :android-app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.example.lspandroid.JniBridgeArchitectureTest \
    -Pandroid.testInstrumentationRunnerArguments.method=testSystemLoadLibraryWithAbiSelection
```

### Option 4: Build and Install for Specific ABI

```bash
# Build for arm64-v8a
./gradlew :android-app:assembleDebug -Pandroid.injected.abi=arm64-v8a

# Build for x86_64
./gradlew :android-app:assembleDebug -Pandroid.injected.abi=x86_64

# Build for armeabi-v7a
./gradlew :android-app:assembleDebug -Pandroid.injected.abi=armeabi-v7a
```

## Test Devices

### arm64-v8a Testing
- **Recommended**: Pixel 6+, Samsung Galaxy S21+
- **Processors**: Snapdragon 8-series, Exynos, MediaTek Dimensity, Google Tensor
- **Requirements**: Physical device with Android API 21+

### x86_64 Testing
- **Recommended**: Android Emulator API 30+
- **Host**: Intel HAXM, AMD-V, or Apple Silicon with Rosetta
- **Requirements**: Emulator with x86_64 system image

### armeabi-v7a Testing (Optional)
- **Recommended**: Legacy devices with Snapdragon 4-series
- **Requirements**: Physical device with Android API 16+
- **Note**: This architecture is optional and primarily for legacy support

## Expected Results

### Successful Test Output

```
[INFO] JNI Bridge Architecture Verification
[INFO] ======================================
[INFO] Device primary ABI: arm64-v8a
[INFO] Device supported ABIs: arm64-v8a,armeabi-v7a,armeabi
[INFO] Testing all supported architectures: arm64-v8a

[INFO] ==========================================
[INFO] Testing Architecture: arm64-v8a
[INFO] Device Architecture: arm64-v8a
[INFO] ==========================================
[INFO] Building for architecture: arm64-v8a
[SUCCESS] Build successful for arm64-v8a
[INFO] Verifying library loading for arm64-v8a
[SUCCESS] Library found in APK: lib/arm64-v8a/liblsp_audio_engine.so
[INFO] Running instrumented tests for architecture: arm64-v8a
[SUCCESS] Tests passed for arm64-v8a

[PASS] Architecture Detection: Architecture: arm64-v8a (NEON enabled)
[PASS] Bridge API Version: Version: 1
[PASS] Library Initialization: Success
[PASS] Plugin Creation: Successfully created and destroyed plugin
[PASS] Plugin Initialization: Successfully initialized and activated plugin
[PASS] Port Information: Port count: 10
[PASS] Parameter Operations: Successfully set and retrieved parameters
[PASS] Audio Processing: Successfully processed 512 frames
[PASS] Multiple Plugin Types: Successfully created all 5 plugin types
[PASS] Library Cleanup: Success

[SUCCESS] All tests passed for arm64-v8a

[INFO] ==========================================
[INFO] Test Summary
[INFO] ==========================================
[INFO] Total architectures tested: 1
[SUCCESS] Passed: 1
[SUCCESS] All tests passed!
```

### Test Failure Indicators

If tests fail, look for:
- `[FAIL]` markers in output
- `CRITICAL` messages indicating exceptions crossed JNI boundary
- Library loading failures
- Build failures for specific architectures
- Missing SIMD support on expected architectures

## Troubleshooting

### Library Not Found in APK

**Problem**: `Library not found in APK: lib/arm64-v8a/liblsp_audio_engine.so`

**Solution**:
1. Check CMakeLists.txt has correct architecture configuration
2. Verify build.gradle.kts includes correct ndk.abiFilters
3. Clean and rebuild: `./gradlew clean :android-app:assembleDebug`

### System.loadLibrary() Fails

**Problem**: `UnsatisfiedLinkError: dlopen failed`

**Solution**:
1. Verify library is built for correct architecture
2. Check for missing dependencies (log, android, OpenSLES)
3. Verify NDK version is r25c or later
4. Check for architecture mismatch (e.g., arm64 library on x86_64 device)

### C++ Exceptions Cross JNI Boundary

**Problem**: `CRITICAL: C++ exception crossed JNI boundary`

**Solution**:
1. Add try-catch blocks in all JNI functions
2. Convert exceptions to error codes
3. Verify no exceptions in C-linkage functions
4. Check CMakeLists.txt has `-fno-exceptions` flag

### SIMD Support Not Detected

**Problem**: `arm64-v8a should have NEON support` failure

**Solution**:
1. Verify CMakeLists.txt has correct architecture flags
2. Check for `-march=armv8-a+simd` on arm64-v8a
3. Check for `-msse2` on x86_64
4. Verify preprocessor definitions: `__ARM_NEON`, `__SSE2__`

### Tests Fail on Emulator

**Problem**: Tests pass on physical device but fail on emulator

**Solution**:
1. Ensure emulator uses x86_64 system image
2. Verify emulator has sufficient resources (RAM, CPU)
3. Check emulator API level matches requirements
4. Try different emulator configurations (Intel HAXM vs AMD-V)

## Architecture-Specific Notes

### arm64-v8a
- **Optimization**: `-O3` for maximum performance
- **SIMD**: NEON is baseline (always available)
- **Flags**: `-march=armv8-a+simd`
- **API Level**: 21 (Lollipop)
- **Primary Target**: Modern Android devices

### x86_64
- **Optimization**: `-O3` for maximum performance
- **SIMD**: SSE2 is baseline (always available)
- **Flags**: `-msse2`
- **API Level**: 21 (Lollipop)
- **Primary Target**: Android emulators

### armeabi-v7a
- **Optimization**: `-Oz` for minimal binary size
- **SIMD**: NEON is optional (runtime detection required)
- **Flags**: `-march=armv7-a -mfloat-abi=softfp -mfpu=neon`
- **API Level**: 16 (Jelly Bean)
- **Primary Target**: Legacy devices (optional support)

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: JNI Bridge Architecture Tests

on: [push, pull_request]

jobs:
  test-arm64:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run arm64-v8a tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 30
          arch: arm64-v8a
          script: ./android-app/src/main/cpp/verify_jni_bridge_architectures.sh arm64-v8a

  test-x86_64:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run x86_64 tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 30
          arch: x86_64
          script: ./android-app/src/main/cpp/verify_jni_bridge_architectures.sh x86_64
```

## Conclusion

This verification suite ensures the JNI bridge works correctly across all supported Android architectures. All tests must pass before marking task 2.1 as complete.

**Success Criteria**:
- ✅ All 10 native tests pass on each architecture
- ✅ All 7 Kotlin instrumented tests pass on each architecture
- ✅ System.loadLibrary() works with proper ABI selection
- ✅ No C++ exceptions cross JNI boundary
- ✅ Architecture-specific optimizations are active (NEON, SSE2)
- ✅ Tests pass on arm64-v8a physical devices
- ✅ Tests pass on x86_64 emulators
- ✅ Tests pass on armeabi-v7a legacy devices (if supported)
