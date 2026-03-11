# Quick Start: x86_64 Test Harness

## One-Command Build

```bash
cd android-app/src/main/cpp
export ANDROID_NDK_HOME=/path/to/ndk
./build_test_harness_x86_64.sh
```

## One-Command Deploy & Run

```bash
cd android-app/src/main/cpp/build_test_harness_x86_64
adb push test-harness-x86_64 /data/local/tmp/ && \
adb shell chmod +x /data/local/tmp/test-harness-x86_64 && \
adb shell /data/local/tmp/test-harness-x86_64
```

## Expected Output (Success)

```
========================================
LSP Plugins Test Harness - x86_64
========================================

=== CPU Feature Detection (x86_64) ===
✓ SSE2: Enabled (baseline for x86_64)

=== Functional Tests ===
Test: min()... PASSED
Test: max()... PASSED
Test: abs_min()... PASSED
Test: abs_max()... PASSED
Test: minmax()... PASSED
Test: min_index()... PASSED
Test: max_index()... PASSED

=== Edge Case Tests ===
Test: Empty array handling... PASSED
Test: Single element array... PASSED
Test: Identical values... PASSED

=== Test Summary ===
Total:  10
Passed: 10 ✓
Failed: 0
Success Rate: 100%

=== Performance Benchmark (x86_64) ===
[Performance metrics displayed]

========================================
✓ All tests PASSED
x86_64 DSP implementation verified
========================================
```

## Troubleshooting

### NDK Not Found
```bash
export ANDROID_NDK_HOME=/path/to/android/ndk
# or
export ANDROID_HOME=/path/to/android/sdk
```

### No Emulator Running
```bash
emulator -list-avds
emulator -avd <x86_64_avd_name> &
```

### Wrong Architecture
Check emulator ABI:
```bash
adb shell getprop ro.product.cpu.abi
# Should return: x86_64
```

## Files

- `test_harness_x86_64.cpp` - Source code
- `build_test_harness_x86_64.sh` - Build script
- `verify_test_harness_x86_64.sh` - Verification
- `README_TEST_HARNESS_X86_64.md` - Full docs
- `TEST_HARNESS_X86_64_SUMMARY.md` - Implementation summary
