# DSP Core Tests

This directory contains tests for the LSP Android Bridge implementation.

## Test Files

### native_test_harness.cpp
Full-featured test harness for testing DSP plugins with various signal types and parameters.

**Usage:**
```bash
./native_test_harness [options]

Options:
  --output FILE          Output WAV file (default: test_output.wav)
  --duration SECONDS     Test duration in seconds (default: 2.0)
  --sample-rate RATE     Sample rate in Hz (default: 48000)
  --signal TYPE          Test signal type: sine, noise, sweep (default: sine)
  --freq FREQ            Frequency for sine wave in Hz (default: 1000)
  --param INDEX=VALUE    Set plugin parameter (e.g., --param 0=12.0)
  --list-params          List available plugin parameters
  --help                 Show help message
```

### test_plugin_destroy.cpp
Unit tests for the `plugin_destroy()` function (Task 2.1).

Tests validate:
- Destruction of valid plugin handles
- Error handling for null/invalid handles
- Thread-local error state management
- Resource cleanup
- Exception safety across C boundary
- Integration with parameter operations
- Integration with audio processing

**Build and run:**
```bash
./build_and_test_destroy.sh
```

### eq_test_harness.cpp
Legacy test harness for parametric EQ plugin.

## Test Results

### Task 2.1 - plugin_destroy() Implementation

✅ All tests pass successfully

**Validated Requirements:**
- Requirement 4: JNI Bridge Interface
  - C-linkage API exposed
  - Proper error code returns
  - Resource cleanup on destroy
  - No exceptions across C/JNI boundary
  
- Requirement 29: Memory Management
  - All allocated memory released on destroy
  - Tested with 1000+ create/destroy cycles
  - No memory leaks detected

**Test Coverage:**
- Valid handle destruction
- Null handle error handling
- Thread-local error state
- Multiple plugin lifecycle
- Exception safety
- Post-operation cleanup
- Error message retrieval

See `PLUGIN_DESTROY_VERIFICATION.md` for detailed verification report.

## Building Tests

Tests can be built using the provided build scripts:

```bash
# Build and run plugin_destroy tests
./build_and_test_destroy.sh

# Build native test harness (if not already built)
g++ -std=c++17 -O2 -Wall -Wextra \
    dsp-core/lsp_android_bridge.cpp \
    dsp-core/tests/native_test_harness.cpp \
    -o dsp-core/tests/native_test_harness \
    -I.
```

## Memory Testing

To verify no memory leaks with valgrind (if available):

```bash
valgrind --leak-check=full ./build_test_destroy/test_plugin_destroy
```

## Integration Testing

The native test harness provides end-to-end integration testing:

```bash
# Quick integration test
./dsp-core/tests/native_test_harness --duration 0.1 --output test.wav

# Test with parameters
./dsp-core/tests/native_test_harness --param 0=12.0 --duration 1.0

# List available parameters
./dsp-core/tests/native_test_harness --list-params
```
