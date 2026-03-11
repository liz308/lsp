# Plugin Porting Analysis for LSP-Plugins Android

## Overview
This document provides a detailed analysis of the 5 launch-tier plugins that need to be ported to Android, including their dependencies, complexity, and porting strategy.

## Date
March 10, 2026

## Launch-Tier Plugins

### 1. Parametric Equalizer (`para_equalizer.cpp`)
**File Size**: 1,210 lines
**Complexity**: HIGH

**Core Dependencies**:
- `core/plugin.h` - Base plugin class ✅ (already ported)
- `core/util/Bypass.h` - Bypass utility ✅ (already ported)
- `core/filters/Equalizer.h` - EQ filter implementation ❓ (needs verification)
- `core/util/Analyzer.h` - Spectrum analyzer ✅ (already ported)
- `metadata/para_equalizer.h` - Plugin metadata ❓ (needs porting)

**Key Features**:
- Multi-band parametric EQ (typically 8-16 bands)
- Per-band controls: frequency, gain, Q factor, filter type
- Real-time frequency response visualization
- Bypass per band
- Multiple filter types (bell, shelf, notch, etc.)

**DSP Requirements**:
- Biquad filters ✅ (ported in dsp/filter.cpp)
- FFT for visualization ✅ (ported in dsp/fft.cpp)
- Filter transfer function calculation ✅ (ported)

**Estimated Porting Effort**: 3-5 days
- Core EQ logic: 2 days
- Metadata and port definitions: 1 day
- Testing and validation: 1-2 days

---

### 2. Compressor (`compressor.cpp`)
**File Size**: 1,148 lines
**Complexity**: HIGH

**Core Dependencies**:
- `core/plugin.h` - Base plugin class ✅
- `core/util/Bypass.h` - Bypass utility ✅
- `core/util/Sidechain.h` - Sidechain processing ✅ (already ported)
- `core/util/Delay.h` - Lookahead delay ✅ (already ported)
- `core/util/MeterGraph.h` - Metering ✅ (already ported)
- `core/filters/Equalizer.h` - Sidechain EQ ❓
- `core/dynamics/Compressor.h` - Compressor DSP core ❓ (needs verification)
- `metadata/compressor.h` - Plugin metadata ❓

**Key Features**:
- Threshold, ratio, attack, release controls
- Knee control for soft/hard compression
- Lookahead for transparent compression
- Sidechain input with EQ
- Makeup gain
- Dry/wet mix
- Multiple modes: Mono, Stereo, L/R, M/S
- Real-time gain reduction metering
- Compression curve visualization

**DSP Requirements**:
- Envelope follower (attack/release)
- Gain calculation based on threshold/ratio/knee
- Lookahead delay ✅
- Sidechain filtering ✅
- RMS/Peak detection

**Estimated Porting Effort**: 4-6 days
- Compressor DSP core: 2-3 days
- Sidechain and routing: 1 day
- Metadata and ports: 1 day
- Testing: 1-2 days

---

### 3. Limiter (`limiter.cpp`)
**File Size**: 31 lines (appears corrupted/placeholder)
**Complexity**: MEDIUM-HIGH

**Note**: The limiter.cpp file in the repository appears to be corrupted or is a placeholder. Need to check if there's a proper implementation elsewhere or if it's implemented as a special case of the compressor.

**Expected Dependencies** (based on typical limiter design):
- `core/plugin.h` - Base plugin class ✅
- `core/util/Bypass.h` - Bypass utility ✅
- `core/util/Delay.h` - Lookahead delay ✅
- `core/dynamics/Limiter.h` or similar ❓
- `metadata/limiter.h` - Plugin metadata ❓

**Key Features** (typical limiter):
- Ceiling/threshold control
- Lookahead time
- Release time
- True peak or sample peak limiting
- Oversampling for inter-sample peak detection
- Gain reduction metering

**DSP Requirements**:
- Peak detection
- Fast attack (instant or near-instant)
- Lookahead buffer ✅
- Oversampling (optional but recommended)
- Brick-wall limiting algorithm

**Estimated Porting Effort**: 2-4 days (if implementation exists)
- Core limiter logic: 1-2 days
- Metadata and ports: 0.5 day
- Testing: 0.5-1.5 days

**Action Required**: Verify limiter implementation source

---

### 4. Gate (`gate.cpp`)
**File Size**: 934 lines
**Complexity**: MEDIUM-HIGH

**Core Dependencies**:
- `core/plugin.h` - Base plugin class ✅
- `core/util/Bypass.h` - Bypass utility ✅
- `core/util/Sidechain.h` - Sidechain processing ✅
- `core/util/Delay.h` - Lookahead delay ✅
- `core/util/MeterGraph.h` - Metering ✅
- `core/filters/Equalizer.h` - Sidechain EQ ❓
- `core/dynamics/Gate.h` - Gate DSP core ❓
- `metadata/gate.h` - Plugin metadata ❓

**Key Features**:
- Threshold control
- Attack and release times
- Hold time
- Hysteresis (different open/close thresholds)
- Sidechain input with EQ
- Reduction amount (gate vs. expander)
- Multiple modes: Mono, Stereo, L/R, M/S
- Real-time gate state metering

**DSP Requirements**:
- Envelope follower
- Hysteresis logic
- Smooth gain transitions (attack/release)
- Sidechain filtering ✅
- RMS/Peak detection

**Estimated Porting Effort**: 3-5 days
- Gate DSP core: 2 days
- Sidechain and routing: 1 day
- Metadata and ports: 1 day
- Testing: 1 day

---

### 5. Spectrum Analyzer (`spectrum_analyzer.cpp`)
**File Size**: 851 lines
**Complexity**: MEDIUM

**Core Dependencies**:
- `core/plugin.h` - Base plugin class ✅
- `core/util/Analyzer.h` - Analyzer utility ✅ (already ported)
- `core/windows.h` - Window functions ❓ (needs verification)
- `core/envelope.h` - Envelope utilities ✅ (already ported)
- `dsp/dsp.h` - DSP functions ✅
- `metadata/spectrum_analyzer.h` - Plugin metadata ❓

**Key Features**:
- Real-time FFT-based spectrum analysis
- Multiple window functions (Hann, Hamming, Blackman, etc.)
- Adjustable FFT size
- Peak hold
- Frequency response smoothing
- Multiple channels (stereo, mid/side)
- Logarithmic frequency scale
- dB scale display

**DSP Requirements**:
- FFT ✅ (ported in dsp/fft.cpp)
- Window functions ❓ (need to verify if ported)
- Magnitude calculation
- Peak detection
- Smoothing/averaging

**Estimated Porting Effort**: 2-3 days
- Core analyzer logic: 1 day
- Window functions (if needed): 0.5 day
- Metadata and ports: 0.5 day
- Testing: 0.5-1 day

---

## Common Dependencies Analysis

### Already Ported (✅)
1. **Core Utilities**:
   - `core/plugin.h` - Base plugin class
   - `core/util/Bypass.h` - Bypass utility
   - `core/util/Sidechain.h` - Sidechain processing
   - `core/util/Delay.h` - Delay lines
   - `core/util/DynamicDelay.h` - Dynamic delay
   - `core/util/MeterGraph.h` - Metering
   - `core/util/Analyzer.h` - Spectrum analyzer utility
   - `core/envelope.h` - Envelope utilities

2. **DSP Functions**:
   - All basic DSP operations (copy, fill, reverse, etc.)
   - Mix functions
   - FFT functions
   - Filter functions (biquad, transfer, transform)
   - Normalization functions
   - Math operations (mul_k, add_k, etc.)

### Needs Verification (❓)
1. **Filter Classes**:
   - `core/filters/Equalizer.h` - EQ filter implementation
   - May be a higher-level wrapper around biquad filters

2. **Dynamics Classes**:
   - `core/dynamics/Compressor.h` - Compressor DSP core
   - `core/dynamics/Gate.h` - Gate DSP core
   - `core/dynamics/Limiter.h` - Limiter DSP core
   - These likely contain the envelope detection and gain calculation logic

3. **Window Functions**:
   - `core/windows.h` - Window functions for FFT
   - Needed for spectrum analyzer

4. **Plugin Metadata**:
   - `metadata/*.h` - All plugin metadata files
   - Define ports, parameters, ranges, defaults, etc.

### Missing Components

#### 1. Dynamics Processing Core
**Location**: `include/core/dynamics/`
**Files Needed**:
- `Compressor.h/.cpp` - Compressor algorithm
- `Gate.h/.cpp` - Gate algorithm
- `Limiter.h/.cpp` - Limiter algorithm

**Functionality**:
- Envelope detection (RMS, Peak)
- Gain calculation (threshold, ratio, knee)
- Attack/release smoothing
- Lookahead processing

**Estimated Porting**: 3-5 days total

#### 2. Filter Classes
**Location**: `include/core/filters/`
**Files Needed**:
- `Equalizer.h/.cpp` - Multi-band EQ wrapper
- Possibly other filter utilities

**Functionality**:
- Manages multiple biquad filters
- Calculates filter coefficients
- Provides high-level EQ interface

**Estimated Porting**: 2-3 days

#### 3. Window Functions
**Location**: `include/core/windows.h`
**Files Needed**:
- `windows.h/.cpp` - Window function implementations

**Functionality**:
- Hann window
- Hamming window
- Blackman window
- Other window types

**Estimated Porting**: 0.5-1 day

#### 4. Plugin Metadata
**Location**: `include/metadata/`
**Files Needed**:
- `compressor.h` - Compressor metadata
- `gate.h` - Gate metadata
- `limiter.h` - Limiter metadata
- `para_equalizer.h` - EQ metadata
- `spectrum_analyzer.h` - Analyzer metadata

**Functionality**:
- Port definitions (inputs, outputs, parameters)
- Parameter ranges and defaults
- Plugin identification
- UI hints

**Estimated Porting**: 2-3 days total

---

## Porting Strategy

### Phase 1: Core Dependencies (5-8 days)
1. **Dynamics Processing** (3-5 days)
   - Port `core/dynamics/Compressor.h/.cpp`
   - Port `core/dynamics/Gate.h/.cpp`
   - Port `core/dynamics/Limiter.h/.cpp` (or verify if it exists)
   - Create unit tests for each

2. **Filter Classes** (2-3 days)
   - Port `core/filters/Equalizer.h/.cpp`
   - Verify integration with existing biquad filters
   - Create unit tests

3. **Window Functions** (0.5-1 day)
   - Port `core/windows.h/.cpp`
   - Add to DSP library
   - Create unit tests

### Phase 2: Plugin Metadata (2-3 days)
1. Port metadata headers for all 5 plugins
2. Verify port definitions match Android requirements
3. Create metadata parser/validator

### Phase 3: Plugin Implementations (10-15 days)
1. **Spectrum Analyzer** (2-3 days) - Simplest, good starting point
2. **Gate** (3-5 days) - Medium complexity
3. **Parametric EQ** (3-5 days) - High complexity
4. **Compressor** (4-6 days) - High complexity
5. **Limiter** (2-4 days) - Medium complexity (if implementation exists)

### Phase 4: Integration & Testing (5-7 days)
1. Create JNI wrappers for each plugin
2. Integrate with Android audio engine
3. Create UI for each plugin
4. Comprehensive testing:
   - Unit tests for DSP algorithms
   - Integration tests with audio engine
   - UI tests
   - Performance tests
   - Regression tests against Linux reference

---

## Total Estimated Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Core Dependencies | 5-8 days | None (can start immediately) |
| Phase 2: Plugin Metadata | 2-3 days | None (parallel with Phase 1) |
| Phase 3: Plugin Implementations | 10-15 days | Phases 1 & 2 complete |
| Phase 4: Integration & Testing | 5-7 days | Phase 3 complete |
| **Total** | **22-33 days** | Sequential + some parallel work |

**Optimistic**: 22 days (3-4 weeks)
**Realistic**: 27 days (4-5 weeks)
**Pessimistic**: 33 days (5-6 weeks)

---

## Risk Factors

### High Risk
1. **Limiter Implementation Missing**: The limiter.cpp file appears corrupted. Need to locate proper implementation or implement from scratch.
2. **Dynamics Core Complexity**: The compressor/gate/limiter cores may have complex interdependencies.
3. **Performance on Mobile**: DSP algorithms may need optimization for mobile CPUs.

### Medium Risk
1. **Metadata Complexity**: Plugin metadata may have desktop-specific assumptions.
2. **Filter Class Dependencies**: Equalizer class may depend on additional utilities.
3. **UI Integration**: Mapping plugin parameters to Android UI may require custom work.

### Low Risk
1. **Window Functions**: Standard implementations, well-documented.
2. **Spectrum Analyzer**: Mostly uses already-ported DSP functions.
3. **Testing Infrastructure**: Can reuse existing test framework.

---

## Recommendations

### Immediate Actions
1. **Verify Limiter Implementation**: Check git history or alternative sources for proper limiter.cpp
2. **Audit Core Dependencies**: List all files in `core/dynamics/` and `core/filters/`
3. **Create Dependency Graph**: Map exact dependencies for each plugin

### Porting Order (Recommended)
1. **Start with Spectrum Analyzer**: 
   - Simplest plugin
   - Good test of FFT and visualization
   - Builds confidence

2. **Then Gate**:
   - Simpler than compressor
   - Tests dynamics processing
   - Shares code with compressor

3. **Then Parametric EQ**:
   - Tests filter classes
   - Independent of dynamics
   - High user value

4. **Then Compressor**:
   - Most complex
   - Builds on gate experience
   - High user value

5. **Finally Limiter**:
   - May be simplest if it's a compressor variant
   - Or most complex if needs full implementation

### Testing Strategy
1. **Unit Tests First**: Test each DSP component in isolation
2. **Reference Comparison**: Compare output with Linux version
3. **Performance Profiling**: Measure CPU usage on target devices
4. **Real-World Testing**: Test with actual audio content

---

## Next Steps

1. **Audit Dependencies**: 
   ```bash
   # List all files in core/dynamics and core/filters
   find external/lsp-plugins/include/core/dynamics -type f
   find external/lsp-plugins/include/core/filters -type f
   find external/lsp-plugins/src/core/dynamics -type f
   find external/lsp-plugins/src/core/filters -type f
   ```

2. **Check Limiter Status**:
   ```bash
   # Check git history for limiter.cpp
   git log external/lsp-plugins/src/plugins/limiter.cpp
   ```

3. **Create Dependency Matrix**: Document exact file dependencies for each plugin

4. **Set Up Test Framework**: Prepare infrastructure for plugin testing

5. **Begin Phase 1**: Start porting core dynamics and filter classes
