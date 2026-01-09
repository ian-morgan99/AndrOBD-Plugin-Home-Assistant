# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- **Package conflict when updating app**: Changed from debug signing to consistent release keystore
  - Created a release keystore (`release-keystore.jks`) that is included in the repository
  - All builds now use the same signing key, allowing seamless updates without uninstallation
  - Users can now update the app without losing their settings and configuration
  - This fixes the "conflicts with an existing package" error when installing new versions
  - See [SIGNING.md](SIGNING.md) for technical details about the signing configuration
- **Plugin not recognized on Android 8.0+ (API 26+)**: Fixed service startup for modern Android versions
  - Updated `HomeAssistantPluginReceiver` to use `startForegroundService()` on Android O and above
  - This is required on Android 8.0+ when starting a service that calls `startForeground()`
  - Resolves "Background execution not allowed" and service startup failures on Android 8+
  - Plugin now properly starts and registers with AndrOBD's Plugin Manager on all Android versions
- **Service crash on Android O+**: Optimized service initialization to call `startForeground()` immediately
  - Moved `startForeground()` call to the beginning of `onCreate()` to avoid ANR (Application Not Responding)
  - Android O+ requires `startForeground()` to be called within 5 seconds of `startForegroundService()`
  - Prevents service crashes on Android 8.0+ due to delayed foreground service promotion
- **Plugin not appearing in AndrOBD Plugin Manager**: Fixed PluginInfo constructor parameters
  - Copyright field was incorrectly set to "1.0" instead of proper copyright notice
  - URL field was incorrectly set to "Ian Morgan" instead of GitHub repository URL
  - This fix ensures the plugin is properly identified by AndrOBD's plugin discovery system
- **Invalid package warning during installation**: Added signing configuration to release builds
  - Release APKs now use debug signing configuration to ensure proper signing
  - This resolves "invalid package" warnings that prevented APK installation on Android devices
  - Previously, release builds were missing signing configuration, causing installation failures
- **Package conflict preventing installation**: Fixed AndroidManifest.xml manifest merger conflicts
  - Added `tools:replace` directive to resolve conflicts between app and library manifest attributes
  - This fixes installation failures caused by conflicting `allowBackup` and `label` attributes between the app and plugin library
  - The manifest merger now correctly uses the app's values instead of causing conflicts during installation

### Security
- **HTTPS enforcement**: Added network security configuration to enforce HTTPS by default
  - HTTP is only allowed for localhost and .local domains
  - Protects bearer tokens and vehicle data from network interception
  - Supports self-signed certificates via user-added Certificate Authorities
- **Backup security**: Added backup rules to exclude sensitive authentication data
  - Bearer tokens and API credentials are not included in Android backups
  - Prevents credential exposure through backup mechanisms
- **Modern Android target**: Updated targetSdkVersion from 25 to 33
  - Ensures compliance with modern Android security standards
  - Maintains compatibility with Android 4.0.3+ (minSdkVersion 15)

### Added
- **Testing without OBD device support**: Comprehensive documentation for testing without a vehicle
  - Created TESTING_WITHOUT_OBD.md guide covering multiple emulation options
  - Documents AndrOBD built-in demo mode (recommended method)
  - Documents Python ELM327-emulator integration for advanced testing
  - Documents ECU Engine Sim Android app for Bluetooth testing
  - Includes hardware emulator options for production-grade testing
  - Provides debugging tips and troubleshooting procedures
- **Plugin discovery troubleshooting guide**: Added TROUBLESHOOTING_PLUGIN_DISCOVERY.md
  - Step-by-step diagnostic procedures for plugin visibility issues
  - ADB commands for testing plugin discovery
  - Common issues and solutions
  - Android version-specific guidance
  - Advanced debugging techniques
- **Permission documentation**: Added detailed comments explaining why each permission is needed
- **Privacy declarations**: Added hardware feature declarations to clarify optional features
  - WiFi hardware is marked as not required
  - App can be installed on devices without WiFi hardware
- **ACCESS_COARSE_LOCATION permission**: Added for better Android compatibility
  - Complements ACCESS_FINE_LOCATION for WiFi scanning
  - Required by some Android versions for WiFi network detection
- **SECURITY.md**: Added comprehensive security policy documentation
  - Details all security features and privacy protections
  - Provides best practices for secure configuration
  - Explains how to report security vulnerabilities

## [1.0.8] - Previous Release
(See git history for previous changes)

## [1.0.7] - Previous Release
(See git history for previous changes)
