# x86_64 Test Harness

## Overview

The x86_64 test harness is a standalone native executable that validates DSP functionality on Android x86_64 emulators. It provides comprehensive testing of:

- CPU feature detection (SSE2, SSE4.1, AVX)
- DSP compare operations (min, max, abs_min, abs_max, etc.)
- Memory allocation and management
- SIMD vs scalar performance benchmarking
- Edge case handling

## Building

### Prerequisites

1. Android NDK r25c or later installed
2. Set `ANDROID_NDK_HOME` environment variable:
   ```bash
   export ANDROID_NDK_HOME=/path/to/android/ndk
   ```

### Build Command

```bash
cd android-app/src/main/cpp
./build_test_harness_x86_64.sh
```

This will create the test executable at:
```
android-app/src/main/cpp/build_test_harness_x86_64/test-harness-x86_64
```

## Running on Emulator

### 1. Start an x86_64 Android Emulator

```bash
# List available emulators
emulator -list-avds

# Start an x86_64 emulator
emulator -avd <avd_name> &
```

### 2. Push the Test Binary

```bash
cd android-app/src/main/cpp/build_test_harness_x86_64
adb push test-harness-x86_64 /data/local/tmp/
```

### 3. Make it Executable

```bash
adb shell chmod +x /data/local/tmp/test-harness-x86_64
```

### 4. Run the Tests

```bash
adb shell /data/local/tmp/test-harness-x86_64
```

## Expected Output

The test harness will output:

1. **CPU Feature Detection**
   - SSE2 status (should be enabled - baseline for x86_64)
   - SSE4.1 status (if available)
   - AVX status (if available)
   - Architecture information

2. **Functional Tests**
   - DSP compare operations (min, max, abs_min, abs_max, minmax)
   - Index finding operations (min_index, max_index, etc.)
   - Edge cases (empty arrays, single elements, identical values)

3. **Test Summary**
   - Total tests run
   - Passed/Failed counts
   - Success rate percentage

4. **Performance Benchmarks**
   - Processing time for 1M samples
   - Throughput in MSamples/sec
   - Comparison notes for SSE2 optimizations

## Success Criteria

All tests should PASS with output similar to:

```
========================================
LSP Plugins Test Harness - x86_64
========================================

=== CPU Feature Detection (x86_64) ===
✓ SSE2: Enabled (baseline for x86_64)
  SSE4.1: Not enabled at compile time
  AVX: Not enabled at compile time

Architecture: x86_64
SIMD Width: 128-bit (SSE2)
Float Vector Size: 4 floats per register

=== Functional Tests ===

Test: min()... PASSED (result: -8.3)
Test: max()... PASSED (result: 5.7)
Test: abs_min()... PASSED (result: 0.5)
Test: abs_max()... PASSED (result: 8.3)
Test: minmax()... PASSED (min: -8.3, max: 5.7)
Test: min_index()... PASSED (index: 3, value: -8.3)
Test: max_index()... PASSED (index: 2, value: 5.7)

=== Edge Case Tests ===

Test: Empty array handling... PASSED (returns 0.0 for empty array)
Test: Single element array... PASSED
Test: Identical values... PASSED

=== Test Summary ===
Total:  10
Passed: 10 ✓
Failed: 0
Success Rate: 100%

=== Performance Benchmark (x86_64) ===
Test size: 1048576 samples
Iterations: 100

min() performance:
  Average time: XXX µs
  Throughput: XXX MSamples/sec

minmax() performance:
  Average time: XXX µs
  Throughput: XXX MSamples/sec

Note: SSE2 optimizations should provide 2-4x speedup over scalar

========================================
✓ All tests PASSED
x86_64 DSP implementation verified
========================================
```

## Architecture-Specific Notes

### x86_64 Characteristics

- **Baseline SIMD**: SSE2 (128-bit registers, 4 floats per vector)
- **Target API Level**: Android 21 (Lollipop)
- **Optimization Level**: -O3 with -ffast-math
- **Expected Performance**: 2-4x speedup over scalar with SSE2

### Compiler Flags

The test harness is compiled with:
```
-std=c++17 -O3 -msse2 -ffast-math
-DANDROID -DARCH_X86_64 -D__SSE2__=1
```

### Performance Targets

Based on Requirement 16 (Performance Targets):

- **Buffer Processing**: <3ms for 512-sample buffer (accounting for emulator overhead)
- **CPU Usage**: ≤35% on Intel Core i5 or equivalent host processor
- **SIMD Speedup**: At least 2x improvement over scalar implementations

## Troubleshooting

### NDK Not Found

If you see "Android NDK not found", ensure:
```bash
export ANDROID_NDK_HOME=/path/to/android/ndk
# or
export ANDROID_HOME=/path/to/android/sdk
```

### Compilation Errors

Check that you have:
- Android NDK r25c or later
- CMake 3.22.1+
- All required source files in `lsp-plugins/src/`

### Runtime Errors on Emulator

If the test crashes on the emulator:
1. Check emulator architecture: `adb shell getprop ro.product.cpu.abi`
2. Should return `x86_64`
3. Verify binary was pushed correctly: `adb shell ls -l /data/local/tmp/test-harness-x86_64`
4. Check permissions: `adb shell chmod +x /data/local/tmp/test-harness-x86_64`

### Performance Issues

If performance is lower than expected:
- Emulator performance depends on host CPU
- Intel HAXM or AMD-V acceleration should be enabled
- Check host CPU load during testing
- SSE2 optimizations may not be fully utilized in emulator

## Integration with Regression Suite

This test harness is part of the architecture-specific validation required by:

- **Requirement 14**: Automated DSP Regression Testing
- **Requirement 19**: Native Test Harness
- **Requirement 19.5**: Architecture-Specific Validation

The x86_64 test harness verifies:
1. DSP algorithms compile correctly for x86_64
2. SSE2 baseline optimizations are available
3. Output matches expected values within tolerance
4. Performance meets targets for emulator testing

## Related Files

- `build_test_harness_x86_64.sh` - Build script
- `test_harness_x86_64.cpp` - Test harness source code
- `test_dsp_compare.cpp` - DSP compare function tests
- `lsp-plugins/src/dsp/compare.cpp` - DSP implementation
- `CMakeLists.txt` - Main build configuration

## Next Steps

After verifying the x86_64 test harness:

1. Create test-harness-arm64-v8a for physical devices
2. Create test-harness-armeabi-v7a for legacy devices (if supported)
3. Integrate all test harnesses into CI/CD pipeline
4. Run cross-architecture consistency tests
5. Generate architecture-specific performance reports
