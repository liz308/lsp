# Privacy Policy for LSP Audio Plugins

**Last Updated:** December 2024

## Overview

LSP Audio Plugins for Android ("the App") is committed to protecting your privacy. This privacy policy explains how we handle your information.

## Data Collection

### Information We DO NOT Collect

The App does NOT collect, store, or transmit any personal information, including but not limited to:
- Personal identification information
- Location data
- Usage analytics
- Crash reports
- Device identifiers
- Contact information
- Audio recordings

### Information Stored Locally

The App stores the following data locally on your device only:
- Plugin presets and settings
- Plugin chain configurations
- User interface preferences
- Audio processing parameters

This data never leaves your device and is not transmitted to any servers.

## Permissions

### Required Permissions

**RECORD_AUDIO** (Microphone Access)
- **Purpose:** To process live audio input through plugins
- **Usage:** Audio is processed in real-time and not recorded or stored
- **Optional:** The App can function in output-only mode without this permission

**FOREGROUND_SERVICE** (Background Processing)
- **Purpose:** To continue audio processing when the App is in the background
- **Usage:** Only used when explicitly enabled by the user
- **Optional:** Not required for basic functionality

### Optional Permissions

**READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE**
- **Purpose:** To import/export presets and audio files
- **Usage:** Only accessed when user explicitly imports or exports files
- **Optional:** Not required for core functionality

## Data Storage

All data is stored locally on your device using Android's standard storage APIs:
- Presets: Stored in app-private directory
- Settings: Stored in SharedPreferences
- Temporary audio buffers: Stored in memory only

## Data Sharing

The App does NOT share any data with third parties. There are no:
- Analytics services
- Advertising networks
- Cloud services
- Remote servers

## Third-Party Libraries

The App uses the following open-source libraries:
- **LSP Plugins:** Audio processing (LGPL v3)
- **Oboe:** Audio I/O (Apache 2.0)
- **Jetpack Compose:** User interface (Apache 2.0)

These libraries do not collect or transmit any data.

## Children's Privacy

The App does not knowingly collect information from children under 13. The App is suitable for all ages.

## Changes to This Policy

We may update this privacy policy from time to time. Changes will be posted in the App and on our website.

## Contact

For questions about this privacy policy, please contact:
- Email: privacy@lsp-android.example.com
- GitHub: https://github.com/lsp-android/lsp-plugins-android

## Your Rights

You have the right to:
- Delete all app data through Android Settings
- Revoke permissions at any time through Android Settings
- Export your presets before uninstalling

## Compliance

This App complies with:
- Google Play Store Data Safety requirements
- GDPR (General Data Protection Regulation)
- CCPA (California Consumer Privacy Act)
- Android privacy best practices

## Open Source

This App is open source. You can review the source code to verify our privacy claims:
https://github.com/lsp-android/lsp-plugins-android
