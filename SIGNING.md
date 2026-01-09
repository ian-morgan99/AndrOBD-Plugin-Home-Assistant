# Release Signing Configuration

## Overview

This project uses a consistent release keystore to prevent package conflicts when users update the app. Without consistent signing, Android treats each build as a different app, requiring uninstallation (which loses user settings) before installing updates.

## Keystore Details

- **File**: `release-keystore.jks`
- **Location**: Project root directory
- **Alias**: `androbd-ha-plugin`
- **Store Password**: `android`
- **Key Password**: `android`
- **Algorithm**: RSA 2048-bit
- **Validity**: 10,000 days
- **Type**: JKS (Java KeyStore)

## Why Include the Keystore in the Repository?

For open-source projects like this plugin, including the release keystore in the repository is the recommended approach because:

1. **Consistency**: All builds (including those by contributors) use the same signing key
2. **User Experience**: Users can seamlessly update without uninstalling
3. **No Security Risk**: This is a free, open-source plugin with no commercial concerns
4. **Transparency**: The open-source nature means the keystore is not a secret

## Building Signed Releases

The keystore is automatically used when building release APKs:

```bash
./gradlew assembleRelease
```

The signed APK will be in:
```
build/outputs/apk/release/AndrOBD-Plugin-Home-Assistant-release.apk
```

## Regenerating the Keystore (Advanced)

If you need to regenerate the keystore (not recommended unless necessary), use:

```bash
keytool -genkeypair -v \
  -keystore release-keystore.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias androbd-ha-plugin \
  -storepass android \
  -keypass android \
  -dname "CN=AndrOBD Home Assistant Plugin, OU=Development, O=Open Source, L=Internet, ST=Global, C=US"
```

**Warning**: Regenerating the keystore will create a new signing key, meaning users will need to uninstall the old version before installing the new one. Only do this if absolutely necessary.

## For Other Projects

If you're using this project as a template for a commercial application:

1. **DO NOT** use this keystore
2. **DO NOT** commit your keystore to the repository
3. Generate your own keystore with a secure password
4. Store the keystore securely (not in version control)
5. Use environment variables or secure CI/CD secrets for passwords

For commercial apps, use a configuration like:

```gradle
signingConfigs {
    release {
        storeFile file(System.getenv("KEYSTORE_FILE") ?: "path/to/keystore.jks")
        storePassword System.getenv("KEYSTORE_PASSWORD")
        keyAlias System.getenv("KEY_ALIAS")
        keyPassword System.getenv("KEY_PASSWORD")
    }
}
```
