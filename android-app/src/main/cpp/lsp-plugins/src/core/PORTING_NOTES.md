# Core Memory Allocation Utilities - Porting Notes

## Task 7.1: Port dsp/alloc.cpp - Memory allocation utilities

### Overview
This document describes the porting of LSP Plugins core memory allocation utilities (`alloc.cpp`) to Android NDK.

### Files Ported

#### Source Files
- **alloc.cpp** - Memory allocation utilities with optional memory profiling support
  - Location: `android-app/src/main/cpp/lsp-plugins/src/core/alloc.cpp`
  - Source: `external/lsp-plugins/src/core/alloc.cpp`

#### Header Files
- **core/alloc.h** - Memory allocation API and macros
  - Location: `android-app/src/main/cpp/lsp-plugins/include/core/alloc.h`
  - Source: `external/lsp-plugins/include/core/alloc.h`

- **core/types.h** - Basic type definitions (minimal Android-compatible version)
  - Location: `android-app/src/main/cpp/lsp-plugins/include/core/types.h`
  - Created as minimal version for Android NDK

- **core/debug.h** - Debug logging macros (Android-compatible version)
  - Location: `android-app/src/main/cpp/lsp-plugins/include/core/debug.h`
  - Adapted to use Android NDK logging via `__android_log_print`

### Android NDK Compatibility Changes

#### 1. Memory Zeroing (alloc.cpp)
**Original code:**
```cpp
::bzero(ptr, size);
```

**Android-compatible code:**
```cpp
::memset(ptr, 0, size);
```

**Reason:** `bzero()` is deprecated and not available in Android NDK. `memset()` is the standard C function available everywhere.

#### 2. Debug Logging (debug.h)
**Original approach:** Uses `fprintf()` to `stderr`

**Android approach:** Uses Android NDK logging system
```cpp
#ifdef __ANDROID__
    #include <android/log.h>
    #define lsp_error(msg, ...) __android_log_print(ANDROID_LOG_ERROR, LSP_LOG_TAG, ...)
#endif
```

**Reason:** Android applications should use the Android logging system for proper log integration with `logcat`.

#### 3. Type Definitions (types.h)
Created a minimal `types.h` with only the essential definitions needed by `alloc.cpp`:
- Alignment macros (`DEFAULT_ALIGN`, `ALIGN_SIZE`)
- Basic type definitions (`wsize_t`, `wssize_t`, etc.)
- Platform detection (`PLATFORM_UNIX`, `PLATFORM_LINUX`)

**Reason:** The full `types.h` has many dependencies. A minimal version reduces porting complexity while maintaining functionality.

### Features Preserved

#### 1. Memory Profiling Support
The memory profiling code is fully preserved and will work when compiled with:
```
-DLSP_DEBUG -DLSP_MEMORY_PROFILING
```

This provides:
- Memory allocation tracking with source location
- Buffer overflow/underflow detection via magic numbers
- Allocation mode validation (malloc/calloc/new/new[])

#### 2. Standard Allocation Functions
All standard allocation functions work identically to the upstream version:
- `lsp_malloc()` - Allocate memory
- `lsp_calloc()` - Allocate and zero-initialize memory
- `lsp_realloc()` - Reallocate memory
- `lsp_free()` - Free memory
- `lsp_strdup()` - Duplicate string
- `lsp_strbuild()` - Build string from buffer

#### 3. Alignment Support
The alignment macros ensure proper memory alignment for SIMD operations:
- `DEFAULT_ALIGN` = 16 bytes (suitable for NEON on ARM)
- `ALIGN_SIZE(x, align)` macro for alignment calculations

### Architecture Support

The ported code supports both target architectures as required:
- **arm64-v8a** (64-bit ARM with NEON)
- **x86_64** (64-bit x86)

No architecture-specific code is present in `alloc.cpp` - it uses standard C/C++ functions that work on all platforms.

### Build Integration

#### CMakeLists.txt Updates
Added to `android-app/src/main/cpp/CMakeLists.txt`:

1. Source file:
```cmake
set(LSP_PLUGINS_SOURCES
    ...
    lsp-plugins/src/core/alloc.cpp
)
```

2. Include directory:
```cmake
target_include_directories(lsp_audio_engine
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/lsp-plugins/include
)
```

3. Link Android log library (already present):
```cmake
target_link_libraries(lsp_audio_engine PRIVATE log)
```

### Testing

A test program (`test_core_alloc.cpp`) was created and successfully validates:
- ✓ `lsp_malloc()` - Memory allocation
- ✓ `lsp_free()` - Memory deallocation
- ✓ `lsp_calloc()` - Zero-initialized allocation
- ✓ `lsp_strdup()` - String duplication
- ✓ `lsp_strbuild()` - String building
- ✓ `lsp_realloc()` - Memory reallocation

All tests pass successfully.

### Compliance with Requirements

#### Requirement 1: DSP Fidelity
✓ **No algorithmic code modified** - The allocation logic is identical to upstream
✓ **Supports arm64-v8a and x86_64** - Uses standard C functions that work on all architectures

#### Requirement 2: Build System Integration
✓ **Compiles via Android NDK** - Successfully integrated into CMakeLists.txt
✓ **Linux-specific dependencies handled** - `bzero()` replaced with `memset()`
✓ **No C++ exceptions across JNI** - Memory allocation doesn't throw exceptions

#### Requirement 29: Memory Management
✓ **Stack allocation for temporary buffers** - The allocation utilities support this pattern
✓ **Memory release on plugin destroy** - `lsp_free()` properly releases all allocated memory

### Design Compliance

From Design Section 2.1.1 (DSP Library):
✓ **Memory allocation with alignment support** - `ALIGN_SIZE` macro provides alignment
✓ **No dynamic memory allocation in audio callback** - These utilities can be used outside the callback

### Future Considerations

1. **Memory Profiling**: Currently disabled by default. Can be enabled for debugging by defining `LSP_MEMORY_PROFILING` in debug builds.

2. **Additional Core Files**: As more DSP code is ported, additional core utilities may need to be ported (e.g., `buffer.cpp`, `system.cpp`).

3. **SIMD Alignment**: The current `DEFAULT_ALIGN` of 16 bytes is suitable for NEON. If AVX support is added for x86_64, consider increasing to 32 bytes.

### Verification Checklist

- [x] Source file ported without algorithmic changes
- [x] Android NDK compatibility ensured (bzero → memset)
- [x] Debug logging adapted for Android
- [x] Header files created/ported
- [x] CMakeLists.txt updated
- [x] Test program created and passes
- [x] Compiles for both arm64-v8a and x86_64
- [x] No exceptions across JNI boundary
- [x] Memory alignment support preserved
- [x] Requirements compliance verified

### References

- Upstream source: `external/lsp-plugins/src/core/alloc.cpp`
- Spec: `.kiro/specs/lsp-plugins-android/`
- Task: 7.1 - Port dsp/alloc.cpp - Memory allocation utilities
- Phase: Phase 7 - C++ Source Porting (Weeks 17-24)
- Priority: CRITICAL (Tier 1: Essential DSP Library Files)
