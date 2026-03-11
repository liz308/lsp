# Oboe Library Integration

## Overview

The Oboe library has been successfully integrated into the LSP Android app to provide high-performance, low-latency audio I/O. This document describes the integration details and how it meets the requirements.

## Integration Components

### 1. Build System Integration

**CMakeLists.txt** (`android-app/src/main/cpp/CMakeLists.txt`):
- Oboe is added as a subdirectory from `external/oboe`
- The `oboe` target is linked to the `lsp_audio_engine` library
- Oboe headers are included via `${OBOE_ROOT}/include`

**build.gradle.kts** (`android-app/build.gradle.kts`):
- CMake version 3.22.1 is configured
- Native build targets arm64-v8a and x86_64 ABIs
- C++ static STL is used (`-DANDROID_STL=c++_static`)

### 2. AudioEngine Implementation

**AudioEngine.h/cpp** implements the following Oboe callbacks:
- `oboe::AudioStreamCallback` - for real-time audio processing
- `oboe::AudioStreamErrorCallback` - for error handling and device changes

### 3. Requirements Compliance

#### Requirement 3.1: Use Oboe library for audio I/O ✅
- Oboe is integrated via CMake and linked to the audio engine
- AudioEngine uses Oboe's AudioStreamBuilder and AudioStream APIs

#### Requirement 3.2: Buffer sizes ≤256 frames ✅
- `MAX_BUFFER_SIZE_FRAMES` constant set to 256
- `setFramesPerCallback(256)` configures the callback buffer size
- `setBufferSizeInFrames()` adjusts the internal buffer to minimum
- Actual buffer configuration is logged at stream startup

#### Requirement 3.3: Audio glitch logging ✅
- `getXRunCount()` monitors underruns in the audio callback
- Glitches are logged with delta counts
- Processing continues after glitches (no crashes)

#### Requirement 3.4: Device change handling ✅
- `AudioStreamErrorCallback` interface implemented
- `onErrorAfterClose()` detects device changes and triggers graceful restart
- **Enhanced restart mechanism with retry logic**:
  - Implements exponential backoff (50ms, 100ms, 200ms delays)
  - Up to 3 retry attempts for robust recovery
  - Race condition protection via atomic compare-exchange
  - Prevents duplicate restart requests
- **Comprehensive error state logging**:
  - Device ID, sample rate, channel count logged on errors
  - Audio format, sharing mode, performance mode tracked
  - Restart attempt progress logged for debugging
- **Edge case handling**:
  - Rapid device changes handled via restart-in-progress check
  - Partial failures cleaned up before retry
  - Stream state properly reset on success/failure
- Stream automatically reopens with new device configuration
- Graceful restart without crashing, meeting Requirement 3.4

#### Requirement 3.5: Real-time thread with elevated priority ✅
- Oboe automatically creates a real-time audio thread
- `setPerformanceMode(PerformanceMode::LowLatency)` requests elevated priority
- `setSharingMode(SharingMode::Exclusive)` ensures dedicated access
- Oboe handles thread priority elevation internally

## Audio Stream Configuration

The AudioEngine configures the Oboe stream with:
- **Direction**: Output (can be extended for input)
- **Sharing Mode**: Exclusive (for lowest latency)
- **Performance Mode**: LowLatency (requests real-time priority)
- **Format**: Float (32-bit floating point)
- **Channel Count**: 1 (mono, can be extended)
- **Frames Per Callback**: ≤256 frames
- **Callbacks**: Audio processing + error handling

## Permissions

**AndroidManifest.xml** declares:
- `RECORD_AUDIO` permission for future input support
- `android.hardware.audio.low_latency` feature (optional)
- `android.hardware.audio.pro` feature (optional)

## Error Handling

The implementation handles:
1. **Stream open failures** - logged and reported
2. **Stream start failures** - logged and cleaned up
3. **Audio underruns** - logged with counts, processing continues
4. **Device changes** - automatic stream restart with retry mechanism
   - Exponential backoff retry strategy (3 attempts max)
   - Race condition protection for concurrent device changes
   - Comprehensive device state logging for debugging
   - Graceful failure handling if all retries exhausted
5. **Stream errors** - logged before and after close with device info

## Logging

All audio events are logged with the tag `LspAudioEngine`:
- INFO: Stream lifecycle events, buffer configuration
- WARN: Audio glitches (underruns)
- ERROR: Stream failures, device change errors

## Future Enhancements

The current integration provides the foundation for:
- Input audio capture (microphone, line-in)
- Stereo and multi-channel processing
- Dynamic buffer size adjustment
- Latency measurement and reporting
- CPU usage monitoring

## Testing

To verify the integration:
1. Build the Android app with NDK
2. Deploy to a device with low-latency audio support
3. Check logcat for `LspAudioEngine` messages
4. Verify buffer size ≤256 frames in logs
5. Test device changes (plug/unplug headphones)
6. Monitor for underrun warnings

## References

- [Oboe Documentation](https://github.com/google/oboe)
- [Android Audio Performance](https://developer.android.com/ndk/guides/audio/audio-latency)
- LSP Plugins Requirements Document (Requirement 3)
