# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- **Package conflict preventing installation**: Fixed AndroidManifest.xml component declarations by adding dot prefix to class names
  - This resolves installation failures caused by Android not properly resolving component class names
  - All components (receiver, service, activity) now use relative class name notation (e.g., `.HomeAssistantPluginReceiver`)

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

## [1.0.7] - Previous Release
(See git history for previous changes)
