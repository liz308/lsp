# Delay Implementation Technical Notes

## Overview

This document provides technical details about the delay line implementations ported from lsp-plugins to Android NDK.

## Architecture

### Delay Class - Fixed Circular Buffer

The `Delay` class implements a classic circular buffer with head and tail pointers:

```
Buffer Layout:
┌─────────────────────────────────────┐
│  [tail]  ...  [head]  ...  [wrap]  │
└─────────────────────────────────────┘
     ↑            ↑
     │            └─ Write position
     └─ Read position (head - delay)

Delay = distance between head and tail
```

**Key Implementation Details:**

1. **Buffer Sizing:** The buffer is allocated with extra padding (DELAY_GAP = 0x200) and aligned to DELAY_GAP boundaries for cache efficiency.

2. **Wraparound Handling:** Uses modulo arithmetic `(pos + offset) % nSize` to handle circular wraparound.

3. **Delay Setting:** When `set_delay(d)` is called, the tail pointer is calculated as:
   ```cpp
   nTail = (nHead + nSize - delay) % nSize
   ```

4. **Processing Loop:** The process methods handle wraparound by splitting operations at buffer boundaries:
   ```cpp
   while (count > 0) {
       size_t to_copy = nSize - nHead;  // Space until wrap
       if (to_copy > count) to_copy = count;
       // Copy to_copy samples
       nHead = (nHead + to_copy) % nSize;
   }
   ```

### DynamicDelay Class - Variable Delay with Feedback

The `DynamicDelay` class supports per-sample variable delay and feedback:

```
Processing Flow (per sample):
┌─────────┐
│ Input   │
└────┬────┘
     │
     ▼
┌─────────────┐
│ Write to    │
│ vDelay[head]│
└─────────────┘
     │
     ▼
┌─────────────────────┐
│ Read from           │
│ vDelay[head-delay]  │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Add feedback to     │
│ vDelay[tail+fdelay] │
└──────────┬──────────┘
           │
           ▼
┌─────────────┐
│ Output      │
└─────────────┘
```

**Key Implementation Details:**

1. **Buffer Sizing:** Allocated in multiples of BUF_SIZE (0x400) with extra capacity:
   ```cpp
   buf_sz = delay - (delay % BUF_SIZE) + BUF_SIZE * 2
   ```

2. **Variable Delay:** Each sample can have a different delay time, specified by the `delay` array.

3. **Feedback Path:** Feedback is added to a position determined by:
   ```cpp
   feed = tail + lsp_limit(fdelay[i], 0, shift)
   ```
   This allows the feedback to be delayed relative to the main delay tap.

4. **Limiting:** Delay values are clamped to `[0, nMaxDelay]` to prevent buffer overruns.

## Performance Characteristics

### Delay Class

**Time Complexity:**
- `process(single)`: O(1)
- `process(buffer)`: O(n) where n = sample count
- `set_delay()`: O(1)
- `clear()`: O(buffer_size)

**Memory:**
- Buffer size: `ALIGN_SIZE(max_size + DELAY_GAP, DELAY_GAP) * sizeof(float)`
- Typical overhead: ~512 bytes for alignment

**Cache Behavior:**
- Sequential access pattern in most cases
- Wraparound causes cache miss at buffer boundary
- Aligned allocation improves cache line utilization

### DynamicDelay Class

**Time Complexity:**
- `process()`: O(n) where n = sample count
- Each sample requires 3 buffer accesses (read, write, feedback)

**Memory:**
- Buffer size: `(max_size + 1 + BUF_SIZE*2) * sizeof(float)`
- Aligned to cache line boundaries

**Cache Behavior:**
- Random access pattern due to variable delay
- Feedback path adds additional random access
- Larger buffers may cause cache thrashing

## Real-Time Safety

Both classes are designed for real-time audio processing:

✅ **Safe Operations (in audio callback):**
- `process()` - All variants
- `set_delay()` - Delay class only
- Single-sample processing

❌ **Unsafe Operations (not for audio callback):**
- `init()` - Allocates memory
- `destroy()` - Frees memory
- `clear()` - Writes entire buffer (may take too long)

## Numerical Considerations

### Delay Class - Ramping

The `process_ramping()` method smoothly transitions between delay times:

```cpp
float delta = float(ssize_t(delay) - ssize_t(nDelay)) / float(count);
nTail = (nHead + nSize - ssize_t(nDelay + delta * step)) % nSize;
```

**Potential Issues:**
- Floating-point accumulation error over long ramps
- Modulo of negative numbers (handled by adding nSize)

### DynamicDelay - Interpolation

The current implementation uses **no interpolation** - it reads the nearest sample:

```cpp
float s = vDelay[tail];  // Nearest-neighbor
```

**Implications:**
- Fast processing
- Aliasing artifacts when delay is modulated
- Suitable for fixed delays or slow modulation
- Not ideal for pitch-shifting effects

**Future Enhancement:**
Linear or cubic interpolation could be added:
```cpp
// Linear interpolation (not implemented)
float frac = delay[i] - floor(delay[i]);
float s = vDelay[tail] * (1-frac) + vDelay[tail-1] * frac;
```

## Android-Specific Considerations

### Memory Alignment

Both classes use aligned allocation for SIMD optimization:
- `Delay`: Uses standard malloc (no special alignment)
- `DynamicDelay`: Uses `alloc_aligned()` for cache line alignment

On ARM64 (Android), cache lines are typically 64 bytes. The alignment helps:
- Reduce cache line splits
- Enable NEON SIMD operations (future optimization)

### Compiler Optimizations

With `-O3 -ffast-math`:
- Loop unrolling in process methods
- Vectorization of copy operations (via dsp::copy)
- Aggressive inlining of single-sample methods

### Architecture Differences

**ARM64 (arm64-v8a):**
- NEON SIMD available for vectorization
- Efficient modulo operations
- Good branch prediction

**x86_64 (emulator):**
- SSE/AVX available
- Similar performance characteristics
- Useful for development/testing

## Common Use Cases

### Fixed Delay (Delay class)
```cpp
Delay delay;
delay.init(48000);  // 1 second at 48kHz
delay.set_delay(4800);  // 100ms delay

// Process audio
delay.process(output, input, buffer_size);
```

### Chorus Effect (DynamicDelay)
```cpp
DynamicDelay chorus;
chorus.init(4800);  // Max 100ms

// Generate LFO-modulated delay times
for (size_t i = 0; i < samples; i++) {
    delay_times[i] = 2400 + 1200 * sin(2*PI*lfo_freq*i/sample_rate);
    feedback_gain[i] = 0.3f;
    feedback_delay[i] = 0.0f;
}

chorus.process(output, input, delay_times, feedback_gain, feedback_delay, samples);
```

### Feedback Delay (DynamicDelay)
```cpp
// Constant delay with feedback
for (size_t i = 0; i < samples; i++) {
    delay_times[i] = 24000;  // 500ms at 48kHz
    feedback_gain[i] = 0.5f;  // 50% feedback
    feedback_delay[i] = 0.0f;  // Feedback at same tap
}

delay.process(output, input, delay_times, feedback_gain, feedback_delay, samples);
```

## Debugging Tips

### Common Issues

1. **Clicks/Pops:**
   - Cause: Delay time changed without ramping
   - Solution: Use `process_ramping()` for smooth transitions

2. **Feedback Runaway:**
   - Cause: Feedback gain > 1.0
   - Solution: Clamp feedback gain to [0, 1)

3. **Buffer Overrun:**
   - Cause: Delay time exceeds buffer size
   - Solution: Ensure `delay < nSize` (Delay) or `delay <= nMaxDelay` (DynamicDelay)

4. **Silence Output:**
   - Cause: Buffer not initialized or cleared
   - Solution: Check `init()` return value, avoid calling `clear()` during processing

### Verification

To verify correct operation:

1. **Impulse Response:**
   ```cpp
   // Send impulse
   input[0] = 1.0f;
   for (int i = 1; i < buffer_size; i++) input[i] = 0.0f;
   
   delay.process(output, input, buffer_size);
   
   // Output should be zero until delay time, then impulse appears
   ```

2. **Delay Time Measurement:**
   ```cpp
   // Find impulse in output
   for (size_t i = 0; i < buffer_size; i++) {
       if (output[i] > 0.5f) {
           printf("Delay measured: %zu samples\n", i);
           break;
       }
   }
   ```

## References

- LSP Plugins upstream: https://github.com/sadko4u/lsp-plugins
- Circular buffer theory: https://en.wikipedia.org/wiki/Circular_buffer
- Digital delay lines: "DAFX - Digital Audio Effects" by Udo Zölzer
