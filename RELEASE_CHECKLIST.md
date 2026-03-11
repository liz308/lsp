# Release Engineering Checklist

## Phase 6.1: Build and Release Configuration

### R8 Code Shrinking
- [x] Enable R8 in build.gradle (release buildType)
- [x] Create proguard-rules-release.pro
- [x] Test release build with R8 enabled
- [x] Verify no runtime crashes from over-aggressive shrinking
- [x] Keep JNI methods and native interfaces
- [x] Keep serialization classes

### Resource Shrinking
- [x] Enable resource shrinking in build.gradle
- [x] Remove unused resources
- [x] Verify all required resources are kept
- [x] Test UI rendering in release build

### Per-ABI APK Splits
- [x] Configure ABI splits in build.gradle
- [x] Build separate APKs for arm64-v8a and x86_64
- [x] Test each ABI APK on appropriate devices
- [x] Verify APK size reduction

### Release Signing
- [ ] Generate release keystore
- [ ] Configure signing config in build.gradle
- [ ] Store keystore securely (not in repository)
- [ ] Document keystore backup procedure
- [ ] Sign Android App Bundle for Play Store

### Debug Symbol Stripping
- [x] Configure CMake to strip symbols in release
- [x] Verify native library size reduction
- [x] Keep separate symbol files for crash analysis
- [x] Upload symbols to crash reporting service

## Phase 6.2: Licensing and Compliance

### GPL License
- [x] Include LICENSE.txt in repository root
- [x] Add GPL v3 license text
- [x] Include LGPL v3 for LSP Plugins
- [x] Add license notice in About section of app
- [x] Display license in app settings

### Source Code Repository
- [x] Create public GitHub repository
- [x] Add README.md with build instructions
- [x] Include CONTRIBUTING.md
- [x] Add link to repository in app About section
- [x] Ensure all source files have license headers

### Build System Publication
- [x] Publish build.gradle files
- [x] Publish CMakeLists.txt files
- [x] Document build dependencies
- [x] Provide build instructions for all platforms
- [x] Include CI/CD configuration

### JNI Wrapper Publication
- [x] Publish lsp_android_bridge.h
- [x] Publish JNI wrapper implementation
- [x] Document JNI interface
- [x] Provide usage examples
- [x] Include in repository

### Dependency Attributions
- [x] Create LICENSES directory
- [x] Include Oboe license (Apache 2.0)
- [x] Include Jetpack Compose license (Apache 2.0)
- [x] Include Kotlin license (Apache 2.0)
- [x] Include Material Design license (Apache 2.0)
- [x] Display attributions in app About section

## Phase 6.3: Play Store Submission

### Privacy Policy
- [x] Create PRIVACY_POLICY.md
- [x] Host privacy policy on website
- [x] Add privacy policy link to app
- [x] Add privacy policy link to Play Store listing
- [x] Ensure GDPR compliance

### Permission Descriptions
- [x] Add permission rationale in AndroidManifest.xml
- [x] Explain RECORD_AUDIO permission
- [x] Explain FOREGROUND_SERVICE permission
- [x] Explain storage permissions
- [x] Implement runtime permission requests with explanations

### API Level Targeting
- [x] Set targetSdkVersion to 33 (Android 13)
- [x] Set minSdkVersion to 26 (Android 8.0)
- [x] Test on Android 8.0 through 14
- [x] Handle API level differences
- [x] Use compatibility libraries where needed

### Data Safety Declarations
- [ ] Complete Play Console Data Safety form
- [ ] Declare no data collection
- [ ] Declare local-only data storage
- [ ] Explain permission usage
- [ ] Submit for review

### Adaptive Icons
- [x] Create adaptive icon foreground (108x108dp)
- [x] Create adaptive icon background
- [x] Provide icons for all densities (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
- [x] Test icon on different launchers
- [x] Verify icon meets Play Store guidelines

## Phase 6.4: Additional Features

### Undo/Redo System
- [ ] Implement undo stack (50+ actions)
- [ ] Implement redo stack
- [ ] Add undo/redo UI buttons
- [ ] Test with parameter changes
- [ ] Test with plugin chain modifications

### A/B Comparison
- [x] Implement A/B state management
- [x] Add A/B toggle UI
- [ ] Implement crossfade between states
- [ ] Test state switching
- [ ] Add preset copy between states

### Master Bypass
- [ ] Implement master bypass toggle
- [ ] Add crossfade for bypass transitions
- [ ] Update UI to show bypass state
- [ ] Test with full plugin chain
- [ ] Verify no audio glitches

### CPU Monitoring Per Plugin
- [x] Implement per-plugin CPU measurement
- [x] Add CPU display in plugin chain
- [ ] Add performance profiling mode
- [ ] Log performance metrics
- [ ] Add performance warnings

### Latency Compensation
- [x] Query plugin latency
- [ ] Calculate chain latency
- [ ] Implement delay compensation
- [x] Display total latency
- [ ] Test with mixed-latency plugins

### MIDI Control
- [ ] Detect MIDI devices
- [ ] Implement MIDI CC mapping
- [ ] Add MIDI learn mode
- [ ] Persist MIDI mappings
- [ ] Support program change messages

### Export Functionality
- [ ] Implement WAV export
- [ ] Implement FLAC export
- [ ] Add export progress UI
- [ ] Support background export
- [ ] Add export quality settings

### Preset Sharing
- [x] Implement JSON preset export
- [x] Implement JSON preset import
- [ ] Add Android share sheet integration
- [ ] Support preset collections
- [ ] Add preset browser

### Background Processing
- [ ] Implement foreground service
- [ ] Add processing notification
- [ ] Support cancellation
- [ ] Handle service lifecycle
- [ ] Test battery impact

### Multi-Language Support
- [ ] Create strings.xml for English
- [ ] Create strings.xml for Spanish
- [ ] Create strings.xml for German
- [ ] Create strings.xml for French
- [ ] Create strings.xml for Japanese
- [ ] Test RTL languages
- [ ] Localize plugin descriptions

## Testing Checklist

### Functional Testing
- [ ] Test all plugins load correctly
- [ ] Test parameter adjustments
- [ ] Test preset save/load
- [ ] Test plugin chain management
- [ ] Test audio routing modes
- [ ] Test device change handling
- [ ] Test app lifecycle (pause/resume)

### Performance Testing
- [ ] Measure plugin instantiation time
- [ ] Measure UI frame rate
- [ ] Measure CPU usage
- [ ] Measure memory usage
- [ ] Measure battery drain
- [ ] Test on low-end devices

### Compatibility Testing
- [ ] Test on Android 8.0
- [ ] Test on Android 9.0
- [ ] Test on Android 10
- [ ] Test on Android 11
- [ ] Test on Android 12
- [ ] Test on Android 13
- [ ] Test on Android 14

### Device Testing
- [ ] Test on phone (small screen)
- [ ] Test on phone (large screen)
- [ ] Test on tablet (7-inch)
- [ ] Test on tablet (10-inch)
- [ ] Test on foldable device
- [ ] Test with external audio interface

### Regression Testing
- [ ] Run all unit tests
- [ ] Run DSP regression tests
- [ ] Verify bit-accurate output
- [ ] Test all sample rates
- [ ] Test all buffer sizes

## Pre-Release Checklist

- [ ] All tests passing
- [ ] No critical bugs
- [ ] Performance targets met
- [ ] Documentation complete
- [ ] Privacy policy published
- [ ] License compliance verified
- [ ] Release notes written
- [ ] Screenshots prepared
- [ ] Play Store listing complete
- [ ] Beta testing complete

## Release Process

1. **Version Bump**
   - Update versionCode in build.gradle
   - Update versionName in build.gradle
   - Update CHANGELOG.md

2. **Build Release**
   - Clean build directory
   - Build release AAB: `./gradlew bundleRelease`
   - Build release APKs: `./gradlew assembleRelease`
   - Verify signatures

3. **Upload to Play Console**
   - Upload AAB to internal testing track
   - Complete store listing
   - Submit for review

4. **Post-Release**
   - Monitor crash reports
   - Monitor user reviews
   - Respond to feedback
   - Plan next release

## Rollback Plan

If critical issues are discovered:
1. Halt rollout in Play Console
2. Identify and fix issue
3. Build hotfix release
4. Test thoroughly
5. Resume rollout

## Support Plan

- Monitor GitHub issues
- Respond to user feedback
- Provide documentation
- Release updates regularly
- Maintain compatibility
