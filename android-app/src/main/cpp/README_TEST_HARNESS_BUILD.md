# Test Harness Build Guide

This document explains how to build and use the architecture-specific test harness executables for LSP Plugins Android port.

## Overview

The test harness system provides standalone native executables for testing DSP code without the full Android app. Each supported architecture has its own test harness binary:

- **test-harness-x86_64**: For Android emulators (x86_64 architecture)
- **test-harness-arm64-v8a**: For modern Android devices (64-bit ARM)
- **test-harness-armeabi-v7a**: For legacy Android devices (32-bit ARM, optional)

## Prerequisites

### Required Tools

1. **Android NDK r25c or later**
   - Download from: https://developer.android.com/ndk/downloads
   - Set environment variable: `export ANDROID_NDK=/path/to/ndk`
   - Or: `export ANDROID_NDK_HOME=/path/to/ndk`

2. **CMake 3.22.1 or later**
   - Usually included with Android NDK
   - Or install separately: `sudo apt install cmake` (Linux)

3. **Android SDK Platform Tools** (for deployment)
   - Includes `adb` command
   - Download from: https://developer.android.com/studio/releases/platform-tools

### Verify Prerequisites

```bash
# Check NDK installation
echo $ANDROID_NDK
ls $ANDROID_NDK/build/cmake/android.toolchain.cmake

# Check CMake version
cmake --version

# Check adb
adb version
```

## Building Test Harnesses

### Quick Build (All Architectures)

Use the provided build script to build all test harnesses at once:

```bash
cd android-app/src/main/cpp
./build_test_harnesses.sh
```

This will:
1. Build test-harness-x86_64 for emulators
2. Build test-harness-arm64-v8a for physical devices
3. Copy binaries to `test_harness_binaries/` directory

### Manual Build (Single Architecture)

To build for a specific architecture manually:

```bash
cd android-app/src/main/cpp
mkdir -p build_test_harness_arm64
cd build_test_harness_arm64

# Configure CMake
cmake .. \
    -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-21 \
    -DCMAKE_BUILD_TYPE=Release

# Build
cmake --build . --config Release

# The executable will be: test-harness-arm64-v8a
```

Replace `arm64-v8a` with `x86_64` or `armeabi-v7a` for other architectures.

## Build Output

After building, test harness binaries are located in:

```
android-app/src/main/cpp/test_harness_binaries/
├── arm64-v8a/
│   └── test-harness-arm64-v8a
├── x86_64/
│   └── test-harness-x86_64
└── armeabi-v7a/
    └── test-harness-armeabi-v7a
```

## Deploying to Devices

### Deploy to Physical Device (arm64-v8a)

```bash
# Push binary to device
adb push test_harness_binaries/arm64-v8a/test-harness-arm64-v8a /data/local/tmp/

# Make executable
adb shell chmod +x /data/local/tmp/test-harness-arm64-v8a

# Run test harness
adb shell /data/local/tmp/test-harness-arm64-v8a
```

### Deploy to Emulator (x86_64)

```bash
# Push binary to emulator
adb push test_harness_binaries/x86_64/test-harness-x86_64 /data/local/tmp/

# Make executable
adb shell chmod +x /data/local/tmp/test-harness-x86_64

# Run test harness
adb shell /data/local/tmp/test-harness-x86_64
```

### Multiple Devices

If you have multiple devices/emulators connected:

```bash
# List devices
adb devices

# Target specific device
adb -s <device_serial> push test_harness_binaries/arm64-v8a/test-harness-arm64-v8a /data/local/tmp/
adb -s <device_serial> shell /data/local/tmp/test-harness-arm64-v8a
```

## Test Harness Output

The test harness performs the following tests:

1. **CPU Feature Detection**
   - Detects available SIMD instruction sets (NEON for ARM, SSE2/SSE4.1/AVX for x86_64)
   - Reports architecture information

2. **Functional Tests**
   - Tests DSP compare operations: min, max, abs_min, abs_max, minmax
   - Tests index finding operations: min_index, max_index
   - Validates correctness of DSP implementations

3. **Edge Case Tests**
   - Empty array handling
   - Single element arrays
   - Identical values

4. **Performance Benchmarks**
   - Measures throughput (MSamples/sec)
   - Compares SIMD vs scalar performance
   - Reports processing times

### Example Output

```
==========================================
LSP Plugins Test Harness - arm64-v8a
==========================================

=== CPU Feature Detection (arm64-v8a) ===
✓ NEON: Enabled (baseline for arm64-v8a)
✓ Architecture: AArch64 (64-bit ARM)

Architecture: arm64-v8a
SIMD Width: 128-bit (NEON)
Float Vector Size: 4 floats per register (float32x4_t)

=== Functional Tests ===

Test: min()... PASSED (result: -8.3)
Test: max()... PASSED (result: 5.7)
Test: abs_min()... PASSED (result: 0.5)
Test: abs_max()... PASSED (result: 8.3)
Test: minmax()... PASSED (min: -8.3, max: 5.7)
Test: min_index()... PASSED (index: 3, value: -8.3)
Test: max_index()... PASSED (index: 2, value: 5.7)

=== Edge Case Tests ===

Test: Empty array handling... PASSED
Test: Single element array... PASSED
Test: Identical values... PASSED

=== Test Summary ===
Total:  10
Passed: 10 ✓
Failed: 0
Success Rate: 100%

=== Performance Benchmark (arm64-v8a) ===
Test size: 1048576 samples
Iterations: 100

min() performance:
  Average time: 245.3 µs
  Throughput: 4274.5 MSamples/sec

minmax() performance:
  Average time: 312.7 µs
  Throughput: 3352.1 MSamples/sec

Note: NEON optimizations should provide 3-4x speedup over scalar
Expected: arm64-v8a NEON ~1.5x faster than x86_64 SSE2

==========================================
✓ All tests PASSED
arm64-v8a DSP implementation verified
==========================================
```

## Troubleshooting

### Build Errors

**Error: ANDROID_NDK not set**
```bash
export ANDROID_NDK=/path/to/android-ndk-r25c
# Or
export ANDROID_NDK_HOME=/path/to/android-ndk-r25c
```

**Error: CMake not found**
```bash
# Use CMake from NDK
export PATH=$ANDROID_NDK/cmake/bin:$PATH
```

**Error: Undefined reference to `native::min`**
- Ensure `lsp-plugins/src/dsp/compare.cpp` is included in CMakeLists.txt
- Check that DSP source files are being compiled

### Deployment Errors

**Error: Permission denied**
```bash
# Make sure binary is executable
adb shell chmod +x /data/local/tmp/test-harness-arm64-v8a
```

**Error: No such file or directory**
```bash
# Verify device architecture matches binary
adb shell getprop ro.product.cpu.abi
# Should output: arm64-v8a, x86_64, or armeabi-v7a
```

**Error: adb: device offline**
```bash
# Restart adb server
adb kill-server
adb start-server
adb devices
```

### Runtime Errors

**Segmentation fault**
- Check that DSP library dependencies are properly linked
- Verify memory alignment for SIMD operations
- Run with debug build for more information

**Tests failing**
- Compare output with Linux reference implementation
- Check floating-point tolerance settings
- Verify SIMD intrinsics are correctly implemented

## Architecture-Specific Notes

### x86_64 (Emulators)
- **Baseline**: SSE2 (always available on x86_64)
- **Optional**: SSE4.1, AVX (detected at runtime)
- **Performance**: 2-3x speedup with SSE2 vs scalar
- **Target devices**: Android emulators, Intel-based Android devices

### arm64-v8a (Modern Devices)
- **Baseline**: NEON (always available on arm64-v8a)
- **Performance**: 3-4x speedup with NEON vs scalar
- **Expected**: 1.5x faster than x86_64 SSE2 for equivalent operations
- **Target devices**: Pixel 6+, Samsung Galaxy S21+, most modern Android phones

### armeabi-v7a (Legacy Devices)
- **Baseline**: ARMv7-A with optional NEON
- **NEON detection**: Runtime detection required
- **Performance**: 2-3x speedup with NEON vs scalar (when available)
- **Optimization**: -Oz for size (legacy devices have limited storage)
- **Target devices**: Older Android phones (API 16-20)

## Integration with CI/CD

### GitHub Actions Example

```yaml
- name: Build Test Harnesses
  run: |
    export ANDROID_NDK=$ANDROID_NDK_HOME
    cd android-app/src/main/cpp
    ./build_test_harnesses.sh

- name: Run Tests on Emulator
  run: |
    adb wait-for-device
    adb push test_harness_binaries/x86_64/test-harness-x86_64 /data/local/tmp/
    adb shell chmod +x /data/local/tmp/test-harness-x86_64
    adb shell /data/local/tmp/test-harness-x86_64
```

## Next Steps

After verifying test harnesses work:

1. **Add more DSP tests**: Extend test harness to cover more DSP operations (filters, FFT, etc.)
2. **Regression testing**: Compare output with Linux reference builds
3. **Performance profiling**: Use Android systrace for detailed performance analysis
4. **Automated testing**: Integrate into CI/CD pipeline for continuous validation

## Related Documentation

- [Test Harness x86_64 Summary](TEST_HARNESS_X86_64_SUMMARY.md)
- [DSP ARM64 Compilation Report](DSP_ARM64_COMPILATION_REPORT.md)
- [CMakeLists.txt](CMakeLists.txt) - Build configuration
- [Requirements Document](../../../.kiro/specs/lsp-plugins-android/requirements.md) - Requirement 19

## Support

For issues or questions:
1. Check build logs for specific error messages
2. Verify NDK and CMake versions match requirements
3. Test on both emulator and physical device
4. Compare with working x86_64 test harness implementation
