# Plugin Porting Checklist

## Quick Reference for 5 Launch-Tier Plugins

### Status Legend
- ✅ Complete
- 🔄 In Progress  
- ❌ Not Started
- ⚠️ Blocked/Issue

---

## Core Dependencies Status

### Dynamics Processing (❌ Not Started)
- [ ] `core/dynamics/Compressor.h` (header)
- [ ] `core/dynamics/Compressor.cpp` (implementation)
- [ ] `core/dynamics/Gate.h` (header)
- [ ] `core/dynamics/Gate.cpp` (implementation)
- [ ] `core/dynamics/Limiter.h` (header)
- [ ] `core/dynamics/Limiter.cpp` (implementation)
- [ ] `core/dynamics/Expander.h` (header)
- [ ] `core/dynamics/Expander.cpp` (implementation)
- [ ] `core/dynamics/DynamicProcessor.h` (header)
- [ ] `core/dynamics/DynamicProcessor.cpp` (implementation)

**Estimated**: 3-5 days

### Filter Classes (❌ Not Started)
- [ ] `core/filters/Equalizer.h` (header)
- [ ] `core/filters/Equalizer.cpp` (implementation)
- [ ] `core/filters/Filter.h` (header)
- [ ] `core/filters/Filter.cpp` (implementation)
- [ ] `core/filters/FilterBank.h` (header)
- [ ] `core/filters/FilterBank.cpp` (implementation)
- [ ] `core/filters/DynamicFilters.h` (header)
- [ ] `core/filters/DynamicFilters.cpp` (implementation)
- [ ] `core/filters/common.h` (header)

**Estimated**: 2-3 days

### Window Functions (❌ Not Started)
- [ ] `core/windows.h` (header)
- [ ] `core/windows.cpp` (implementation - if exists)

**Estimated**: 0.5-1 day

---

## Plugin Metadata (❌ Not Started)

- [ ] `metadata/compressor.h`
- [ ] `metadata/gate.h`
- [ ] `metadata/limiter.h`
- [ ] `metadata/para_equalizer.h`
- [ ] `metadata/spectrum_analyzer.h`

**Estimated**: 2-3 days

---

## Plugin Implementations (❌ Not Started)

### 1. Spectrum Analyzer (Recommended First)
- [ ] Port `plugins/spectrum_analyzer.cpp` (851 lines)
- [ ] Port `include/plugins/spectrum_analyzer.h`
- [ ] Create JNI wrapper
- [ ] Create unit tests
- [ ] Integration testing

**Estimated**: 2-3 days

### 2. Gate (Recommended Second)
- [ ] Port `plugins/gate.cpp` (934 lines)
- [ ] Port `include/plugins/gate.h`
- [ ] Create JNI wrapper
- [ ] Create unit tests
- [ ] Integration testing

**Estimated**: 3-5 days

### 3. Parametric EQ (Recommended Third)
- [ ] Port `plugins/para_equalizer.cpp` (1,210 lines)
- [ ] Port `include/plugins/para_equalizer.h`
- [ ] Create JNI wrapper
- [ ] Create unit tests
- [ ] Integration testing

**Estimated**: 3-5 days

### 4. Compressor (Recommended Fourth)
- [ ] Port `plugins/compressor.cpp` (1,148 lines)
- [ ] Port `include/plugins/compressor.h`
- [ ] Create JNI wrapper
- [ ] Create unit tests
- [ ] Integration testing

**Estimated**: 4-6 days

### 5. Limiter (Recommended Fifth)
- [ ] ⚠️ **ISSUE**: Verify limiter.cpp source (appears corrupted)
- [ ] Port `plugins/limiter.cpp` (31 lines - suspicious)
- [ ] Port `include/plugins/limiter.h`
- [ ] Create JNI wrapper
- [ ] Create unit tests
- [ ] Integration testing

**Estimated**: 2-4 days (if source is valid)

---

## Integration Tasks (❌ Not Started)

### JNI Bridge
- [ ] Create plugin factory
- [ ] Implement plugin lifecycle management
- [ ] Create parameter update mechanism
- [ ] Implement audio processing callback
- [ ] Add error handling

**Estimated**: 2-3 days

### Android UI
- [ ] Create plugin UI components
- [ ] Implement parameter controls
- [ ] Add visualization (meters, graphs)
- [ ] Create preset management UI
- [ ] Add plugin browser

**Estimated**: 3-4 days

---

## Testing Tasks (❌ Not Started)

### Unit Tests
- [ ] Dynamics processing tests
- [ ] Filter class tests
- [ ] Window function tests
- [ ] Plugin DSP tests

**Estimated**: 2-3 days

### Integration Tests
- [ ] Plugin loading tests
- [ ] Audio processing tests
- [ ] Parameter update tests
- [ ] Preset save/load tests

**Estimated**: 1-2 days

### Regression Tests
- [ ] Compare with Linux reference output
- [ ] Test at multiple sample rates (44.1, 48, 96 kHz)
- [ ] Test at multiple buffer sizes (64, 128, 256, 512)
- [ ] Performance profiling

**Estimated**: 2-3 days

---

## Total Effort Estimate

| Category | Optimistic | Realistic | Pessimistic |
|----------|-----------|-----------|-------------|
| Core Dependencies | 5 days | 7 days | 10 days |
| Plugin Metadata | 2 days | 2.5 days | 3 days |
| Plugin Implementations | 10 days | 13 days | 18 days |
| Integration | 5 days | 7 days | 9 days |
| Testing | 5 days | 7 days | 10 days |
| **TOTAL** | **27 days** | **36.5 days** | **50 days** |

**Timeline**: 4-7 weeks for complete implementation

---

## Critical Path

1. **Week 1-2**: Core dependencies (dynamics, filters, windows)
2. **Week 2**: Plugin metadata
3. **Week 3-4**: Plugin implementations (analyzer, gate, EQ)
4. **Week 5**: Plugin implementations (compressor, limiter)
5. **Week 6**: Integration and JNI bridge
6. **Week 7**: Testing and validation

---

## Immediate Next Steps

1. ✅ **Complete**: DSP functions porting (DONE)
2. ❌ **Next**: Audit and port core/dynamics classes
3. ❌ **Then**: Audit and port core/filters classes
4. ❌ **Then**: Port window functions
5. ❌ **Then**: Port plugin metadata
6. ❌ **Then**: Begin plugin implementations

---

## Blockers & Risks

### Current Blockers
1. ⚠️ **Limiter.cpp appears corrupted** - Need to verify source
   - Action: Check git history or find alternative source

### Potential Risks
1. **Dynamics classes may have complex interdependencies**
   - Mitigation: Port in order (DynamicProcessor → Compressor → Gate → Limiter)

2. **Filter classes may need additional DSP functions**
   - Mitigation: Audit dependencies before starting

3. **Performance on mobile devices**
   - Mitigation: Profile early, optimize as needed

4. **Metadata may have desktop-specific assumptions**
   - Mitigation: Review and adapt for Android

---

## Success Criteria

### Phase 1: Core Dependencies
- [ ] All dynamics classes compile and link
- [ ] All filter classes compile and link
- [ ] Window functions compile and link
- [ ] Unit tests pass for all core classes

### Phase 2: Plugin Metadata
- [ ] All metadata files ported
- [ ] Metadata parser works correctly
- [ ] Port definitions validated

### Phase 3: Plugin Implementations
- [ ] All 5 plugins compile and link
- [ ] All plugins process audio without crashes
- [ ] Basic functionality verified for each plugin

### Phase 4: Integration
- [ ] JNI bridge works correctly
- [ ] Plugins load in Android app
- [ ] Audio processing works end-to-end
- [ ] UI controls update plugin parameters

### Phase 5: Testing
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Regression tests show <0.1% deviation from reference
- [ ] Performance meets targets (<25% CPU for full chain)

---

## Resources Needed

### Development
- Android NDK r26+
- CMake 3.22+
- C++17 compiler
- Git for version control

### Testing
- Reference Linux build of lsp-plugins
- Test audio files (various formats and sample rates)
- Android test devices (min API 26, target API 33+)
- Performance profiling tools

### Documentation
- LSP-plugins documentation
- Android audio development guides
- DSP algorithm references

---

## Notes

- This checklist should be updated as work progresses
- Estimates are based on single developer working full-time
- Parallel work possible on some tasks (metadata + core dependencies)
- Testing should be continuous, not just at the end
