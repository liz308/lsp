# Device Change Handling Implementation

## Overview

This document describes the enhanced device change handling implementation in AudioEngine that ensures graceful stream restart without crashing, meeting Requirement 3.4.

## Requirement

**Requirement 3.4**: "WHEN the audio device changes, THE Audio_Engine SHALL restart the stream without crashing"

## Implementation Details

### Core Components

#### 1. Error Callback Integration

The AudioEngine implements `oboe::AudioStreamErrorCallback` with two methods:

- **`onErrorBeforeClose()`**: Called when an error occurs before the stream is closed
  - Logs the error type
  - Logs device information for debugging

- **`onErrorAfterClose()`**: Called when an error occurs after the stream is closed (typical for device changes)
  - Logs the error type and device information
  - Triggers graceful restart mechanism
  - Implements race condition protection

#### 2. Graceful Restart Mechanism

The restart process is handled by `attemptStreamRestart()` with the following features:

**Retry Strategy**:
- Up to 3 retry attempts (configurable via parameter)
- Exponential backoff delays: 50ms, 100ms, 200ms
- Each attempt is logged for debugging

**Process Flow**:
1. Close the old stream
2. Wait for device to stabilize (exponential backoff)
3. Attempt to open stream with new device configuration
4. If successful, start the stream
5. If failed, clean up and retry
6. After all retries exhausted, mark engine as stopped

#### 3. Race Condition Protection

To prevent concurrent restart attempts:

```cpp
bool expected = false;
if (!mRestartRequested.compare_exchange_strong(expected, true)) {
    // Restart already in progress, ignore duplicate request
    return;
}
```

This atomic compare-exchange ensures only one restart can be in progress at a time.

#### 4. Device Information Logging

The `logDeviceInfo()` method logs comprehensive device state:

- Device ID
- Sample rate
- Channel count
- Audio format (Float, I16, I24, I32)
- Sharing mode (Exclusive, Shared)
- Performance mode (LowLatency, PowerSaving, None)

This information is crucial for debugging device change issues.

### State Management

The implementation uses atomic variables for thread-safe state management:

- `mIsRunning`: Indicates if the audio engine should be running
- `mRestartRequested`: Prevents concurrent restart attempts
- `mRestartAttempts`: Tracks current retry attempt number

### Edge Cases Handled

1. **Rapid Device Changes**: If multiple device changes occur in quick succession, only one restart is processed at a time
2. **Partial Failures**: If stream opens but fails to start, the stream is properly closed before retry
3. **Null Stream**: Device info logging handles null stream pointers gracefully
4. **Restart During Shutdown**: If engine is stopped, restart is not attempted

## Testing Recommendations

To verify the implementation:

1. **Basic Device Change**:
   - Start audio playback
   - Plug in headphones
   - Verify stream restarts without crash
   - Check logs for successful restart

2. **Rapid Device Changes**:
   - Start audio playback
   - Quickly plug/unplug headphones multiple times
   - Verify no race conditions or crashes
   - Check logs for "restart already in progress" messages

3. **Device Unavailable**:
   - Start audio playback
   - Simulate device removal without replacement
   - Verify graceful failure after retries
   - Check logs for retry attempts and final failure

4. **Log Verification**:
   - Monitor logcat with tag `LspAudioEngine`
   - Verify device info is logged on errors
   - Verify retry attempts are logged
   - Verify success/failure messages

## Log Examples

### Successful Restart
```
E/LspAudioEngine: Error after close: ErrorDisconnected - attempting graceful restart
I/LspAudioEngine: Device Info - ID: 123, SampleRate: 48000, Channels: 1, Format: Float, SharingMode: Exclusive, PerfMode: LowLatency
I/LspAudioEngine: Stream restart attempt 1 of 3
I/LspAudioEngine: Stream opened: framesPerBurst=192, bufferCapacity=3840, bufferSize=192
I/LspAudioEngine: Buffer size set to 192 frames (requirement: ≤256)
I/LspAudioEngine: Stream restart succeeded on attempt 1
I/LspAudioEngine: Stream restarted successfully after device change
```

### Failed Restart with Retries
```
E/LspAudioEngine: Error after close: ErrorDisconnected - attempting graceful restart
I/LspAudioEngine: Device Info - ID: 123, SampleRate: 48000, Channels: 1, Format: Float, SharingMode: Exclusive, PerfMode: LowLatency
I/LspAudioEngine: Stream restart attempt 1 of 3
W/LspAudioEngine: Stream open failed on attempt 1
I/LspAudioEngine: Stream restart attempt 2 of 3
W/LspAudioEngine: Stream open failed on attempt 2
I/LspAudioEngine: Stream restart attempt 3 of 3
W/LspAudioEngine: Stream open failed on attempt 3
E/LspAudioEngine: Stream restart failed after 3 attempts
E/LspAudioEngine: Failed to restart stream after all retry attempts
```

### Duplicate Restart Request
```
E/LspAudioEngine: Error after close: ErrorDisconnected - attempting graceful restart
I/LspAudioEngine: Device Info - ID: 123, SampleRate: 48000, Channels: 1, Format: Float, SharingMode: Exclusive, PerfMode: LowLatency
E/LspAudioEngine: Error after close: ErrorDisconnected - attempting graceful restart
W/LspAudioEngine: Restart already in progress, ignoring duplicate request
```

## Compliance with Requirement 3.4

The implementation meets Requirement 3.4 by:

✅ **Detecting device changes**: Via `onErrorAfterClose()` callback
✅ **Restarting the stream**: Via `attemptStreamRestart()` with retry logic
✅ **Without crashing**: Proper error handling, cleanup, and state management
✅ **Gracefully**: Exponential backoff, race condition protection, comprehensive logging

## Future Enhancements

Potential improvements for future iterations:

1. **Configurable retry parameters**: Allow tuning retry count and backoff delays
2. **User notification**: Notify UI layer of device change events
3. **Device preference**: Remember and prefer specific devices
4. **Latency optimization**: Minimize restart time for seamless transitions
5. **Metrics collection**: Track restart success rate and timing
