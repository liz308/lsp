# DSP Functions Port Summary

## Overview
This document summarizes the porting of all remaining DSP functions from the lsp-plugins library to Android.

## Date
March 10, 2026

## Ported Components

### 1. Mix Functions (dsp/mix.cpp)
All mixing utilities have been ported:
- `mix2` - Mix two sources with coefficients
- `mix_copy2` - Copy-mix two sources
- `mix_add2` - Add-mix two sources
- `mix3` - Mix three sources with coefficients
- `mix_copy3` - Copy-mix three sources
- `mix_add3` - Add-mix three sources
- `mix4` - Mix four sources with coefficients
- `mix_copy4` - Copy-mix four sources
- `mix_add4` - Add-mix four sources

**Implementation**: Native C++ implementations in `include/dsp/arch/native/mix.h`

### 2. FFT Functions (dsp/fft.cpp)
Complete FFT implementation ported:
- `direct_fft` - Direct Fast Fourier Transform
- `packed_direct_fft` - Direct FFT with packed complex data
- `reverse_fft` - Reverse (inverse) FFT
- `packed_reverse_fft` - Reverse FFT with packed complex data
- `normalize_fft3` - Normalize FFT coefficients (3-arg version)
- `normalize_fft2` - Normalize FFT coefficients (2-arg version)
- `center_fft` - Center FFT coefficients
- `combine_fft` - Combine FFT harmonics (positive frequencies only)
- `packed_combine_fft` - Combine FFT with packed data

**Implementation**: Native C++ implementations in `include/dsp/arch/native/fft.h`
**Dependencies**: Requires bit reversal functions from `dsp/bits.h`

### 3. Filter Functions (dsp/filter.cpp)
Complete biquad filter implementation:

**Static Filters**:
- `biquad_process_x1` - Single biquad filter
- `biquad_process_x2` - Two parallel biquad filters
- `biquad_process_x4` - Four parallel biquad filters
- `biquad_process_x8` - Eight parallel biquad filters

**Dynamic Filters**:
- `dyn_biquad_process_x1` - Single dynamic biquad
- `dyn_biquad_process_x2` - Two dynamic biquads
- `dyn_biquad_process_x4` - Four dynamic biquads
- `dyn_biquad_process_x8` - Eight dynamic biquads

**Transfer Functions**:
- `filter_transfer_calc_ri` - Calculate transfer function (real/imaginary)
- `filter_transfer_apply_ri` - Apply transfer function (real/imaginary)
- `filter_transfer_calc_pc` - Calculate transfer function (packed complex)
- `filter_transfer_apply_pc` - Apply transfer function (packed complex)

**Transform Functions - Bilinear**:
- `bilinear_transform_x1` - Bilinear transform for 1 filter
- `bilinear_transform_x2` - Bilinear transform for 2 filters
- `bilinear_transform_x4` - Bilinear transform for 4 filters
- `bilinear_transform_x8` - Bilinear transform for 8 filters

**Transform Functions - Matched Z**:
- `matched_transform_x1` - Matched Z transform for 1 filter
- `matched_transform_x2` - Matched Z transform for 2 filters
- `matched_transform_x4` - Matched Z transform for 4 filters
- `matched_transform_x8` - Matched Z transform for 8 filters

**Implementation**: Native C++ implementations in:
- `include/dsp/arch/native/filters/static.h`
- `include/dsp/arch/native/filters/dynamic.h`
- `include/dsp/arch/native/filters/transfer.h`
- `include/dsp/arch/native/filters/transform.h`

### 4. Normalization Functions (dsp/normalize.cpp, dsp/misc.cpp)
- `abs_normalized` - Calculate absolute normalized values
- `normalize` - Normalize values to maximum absolute value

**Implementation**: Native C++ implementations in `include/dsp/arch/native/pmath.h`

### 5. Supporting Math Functions (dsp/primitives.cpp, dsp/pmath.cpp)

**Scalar-Constant Operations (op_kx)**:
- `add_k2`, `add_k3` - Add constant to array
- `sub_k2`, `sub_k3` - Subtract constant from array
- `rsub_k2`, `rsub_k3` - Reverse subtract (constant - array)
- `mul_k2`, `mul_k3` - Multiply array by constant
- `div_k2`, `div_k3` - Divide array by constant
- `rdiv_k2`, `rdiv_k3` - Reverse divide (constant / array)
- `mod_k2`, `mod_k3` - Modulo operations
- `rmod_k2`, `rmod_k3` - Reverse modulo operations

**Absolute Value Operations (abs_vv)**:
- `abs1` - In-place absolute value
- `abs2` - Absolute value with copy

**Search/MinMax Operations**:
- `min` - Find minimum value
- `max` - Find maximum value
- `minmax` - Find both min and max

**Implementation**: Native C++ implementations in:
- `include/dsp/arch/native/pmath/op_kx.h`
- `include/dsp/arch/native/pmath/abs_vv.h`
- `include/dsp/arch/native/pmath/minmax.h`
- `include/dsp/arch/native/search.h`

## File Structure

### Headers Copied
```
android-app/src/main/cpp/lsp-plugins/include/dsp/
├── bits.h
├── common/
│   ├── fft.h
│   ├── filters.h
│   ├── mix.h
│   ├── misc.h
│   ├── pmath/
│   │   ├── op_kx.h
│   │   ├── abs_vv.h
│   │   └── minmax.h
│   ├── search/
│   │   └── minmax.h
│   └── hmath/
└── arch/
    ├── native/
    │   ├── bits.h
    │   ├── fft.h
    │   ├── mix.h
    │   ├── pmath.h
    │   ├── pmath/
    │   │   ├── op_kx.h
    │   │   ├── abs_vv.h
    │   │   └── minmax.h
    │   ├── search.h
    │   ├── hmath/
    │   └── filters/
    │       ├── static.h
    │       ├── dynamic.h
    │       ├── transfer.h
    │       └── transform.h
    └── x86/
        └── bits.h
```

### Source Files Modified
1. **dsp.cpp** - Added function pointer declarations for all new DSP functions
2. **native.cpp** - Added includes and registration for all new DSP functions

## Integration Points

### In dsp.h
Added includes for:
- `<dsp/common/mix.h>`
- `<dsp/common/fft.h>`
- `<dsp/common/filters.h>`
- `<dsp/common/misc.h>`
- `<dsp/common/pmath/op_kx.h>`
- `<dsp/common/pmath/abs_vv.h>`
- `<dsp/common/pmath/minmax.h>`
- `<dsp/common/search/minmax.h>`

### In dsp.cpp
Added function pointer declarations for:
- 9 mix functions
- 9 FFT functions
- 2 normalization functions
- 16 pmath op_kx functions
- 2 abs_vv functions
- 3 search/minmax functions
- 4 static filter functions
- 4 dynamic filter functions
- 4 transfer function operations
- 8 transform functions (4 bilinear + 4 matched Z)

Total: 61 new function pointers

### In native.cpp
Added includes for:
- `<dsp/bits.h>`
- `<dsp/arch/native/mix.h>`
- `<dsp/arch/native/fft.h>`
- `<dsp/arch/native/pmath.h>`
- `<dsp/arch/native/pmath/op_kx.h>`
- `<dsp/arch/native/pmath/abs_vv.h>`
- `<dsp/arch/native/pmath/minmax.h>`
- `<dsp/arch/native/search.h>`
- `<dsp/arch/native/filters/static.h>`
- `<dsp/arch/native/filters/dynamic.h>`
- `<dsp/arch/native/filters/transform.h>`
- `<dsp/arch/native/filters/transfer.h>`

Added EXPORT1 registrations for all 61 functions in `dsp_init()`

## Compilation Status

✅ Successfully compiles with g++ on Linux x86_64
- Only warning: DEFAULT_ALIGN redefinition (harmless)
- All function implementations are present
- All dependencies resolved

## Notes

1. **Architecture Detection**: The code uses native implementations for all functions. Architecture-specific optimizations (SSE, AVX, NEON) are not included in this port but could be added later.

2. **Bit Reversal**: FFT functions depend on the `reverse_bits` functions from `dsp/bits.h` and the `__rb` lookup table from `dsp/bits.cpp`.

3. **Filter Types**: The filter implementation supports both static filters (fixed coefficients) and dynamic filters (time-varying coefficients), with support for 1, 2, 4, or 8 parallel filter banks.

4. **FFT Implementation**: Uses Cooley-Tukey radix-2 algorithm with bit-reversal scrambling. Supports both separate real/imaginary arrays and packed complex format.

5. **Function Pointers**: All DSP functions use function pointers to allow for runtime selection of optimized implementations (though currently only native implementations are registered).

## Testing Recommendations

1. Create unit tests for each function category:
   - Mix functions with various coefficient combinations
   - FFT round-trip (forward + inverse should recover original signal)
   - Filter frequency response verification
   - Normalization edge cases (zero input, single sample, etc.)

2. Performance benchmarking:
   - Compare against reference implementations
   - Measure CPU usage for typical audio buffer sizes (64, 128, 256, 512 samples)

3. Integration testing:
   - Use in actual plugin processing chains
   - Verify no audio artifacts or glitches
   - Test with various sample rates (44.1kHz, 48kHz, 96kHz)

## Completion Status

All Tier 1 Essential DSP Library Files have been successfully ported:
- ✅ dsp/alloc.cpp
- ✅ dsp/array.cpp
- ✅ dsp/bits.cpp
- ✅ dsp/compare.cpp
- ✅ dsp/dsp.cpp
- ✅ dsp/native.cpp
- ✅ dsp/copy.cpp
- ✅ dsp/delay.cpp
- ✅ dsp/fft.cpp (via native headers)
- ✅ dsp/filter.cpp (via native headers)
- ✅ dsp/mix.cpp (via native headers)
- ✅ dsp/normalize.cpp (via native headers)
- ✅ dsp/primitives.cpp (via native headers)

**Note**: The actual .cpp files for fft, filter, mix, normalize, and primitives are not separate files in the lsp-plugins architecture. Instead, these functions are implemented as inline functions in the architecture-specific header files (native, x86, ARM, etc.) and registered via function pointers in native.cpp.
