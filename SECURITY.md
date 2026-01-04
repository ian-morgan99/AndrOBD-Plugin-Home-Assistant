# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Security Features

### Data Protection

This application implements several security measures to protect your data:

1. **HTTPS Enforcement**: The app enforces HTTPS connections by default to protect bearer tokens and vehicle data from interception
   - HTTP is only allowed for localhost (127.0.0.1) and .local domains (mDNS/Bonjour)
   - For Home Assistant instances on private networks with IP addresses, please use HTTPS

2. **Backup Security**: Sensitive authentication data (bearer tokens, API credentials) is excluded from Android backups
   - This prevents credentials from being exposed through backup mechanisms
   - Other app data can still be backed up normally

3. **Self-Signed Certificate Support**: The app trusts user-added Certificate Authorities
   - This allows you to use self-signed certificates for local Home Assistant instances
   - You can add your certificate to the Android trusted certificate store

### Privacy

1. **Location Permission**: The app requests location permission (`ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`)
   - **Purpose**: Required by Android for WiFi scanning on Android 6.0+
   - **Usage**: Only used to detect WiFi network presence for SSID-based transmission modes
   - **NOT used for**: Tracking your location, collecting location data, or sharing location with any service

2. **Network Permissions**: 
   - **INTERNET**: Required to communicate with Home Assistant
   - **ACCESS_NETWORK_STATE**: Used to check internet connectivity before transmission
   - **ACCESS_WIFI_STATE**: Used to detect current WiFi network
   - **CHANGE_WIFI_STATE**: Used for automatic WiFi switching feature (optional)

3. **No Data Collection**: This app does not collect, store, or transmit any data except:
   - OBD vehicle data sent directly to YOUR Home Assistant instance
   - Only the data you choose to send
   - No analytics, tracking, or third-party data sharing

### Best Practices

To ensure maximum security when using this plugin:

1. **Use HTTPS**: Always configure your Home Assistant URL with HTTPS
   - Example: `https://homeassistant.local:8123`
   - Use Let's Encrypt for free SSL certificates, or self-signed certificates for local use

2. **Secure Tokens**: 
   - Use long-lived access tokens with limited scope
   - Rotate tokens regularly
   - Never share tokens publicly

3. **Network Security**:
   - Use a VPN for remote access instead of exposing Home Assistant to the internet
   - Keep Home Assistant updated
   - Use strong passwords and enable two-factor authentication in Home Assistant

4. **Local Network Setup**:
   - If your Home Assistant is on a local network only (recommended), it will not be accessible from outside your network
   - Consider using mDNS (.local domain) for easy local access

### Configuration for HTTP on Private Networks

If you need to use HTTP with a private IP address (not recommended), you have two options:

#### Option 1: Use HTTPS (Recommended)
Set up HTTPS on your Home Assistant instance using:
- Let's Encrypt with DNS challenge
- Self-signed certificate (add to Android trusted certificates)
- Nginx reverse proxy with SSL

#### Option 2: Use .local Domain
Configure your Home Assistant with a `.local` domain name:
- Example: `http://homeassistant.local:8123`
- This is supported by the network security configuration

#### Option 3: Manual Override (Not Recommended)
If you must use HTTP with an IP address, you can modify the network security configuration:
1. Fork this repository
2. Edit `src/main/res/xml/network_security_config.xml`
3. Add your specific domain pattern to the `domain-config` section
4. Build and install your modified version

**Warning**: Using HTTP with bearer tokens exposes your credentials to network interception attacks.

## Reporting a Vulnerability

If you discover a security vulnerability in this application, please report it by:

1. **Email**: Contact the repository owner directly (see GitHub profile)
2. **Private Security Advisory**: Use GitHub's security advisory feature (if available)
3. **Do NOT**: Create a public issue for security vulnerabilities

Please include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if available)

We aim to respond to security reports within 48 hours and will keep you updated on the progress.

## Security Updates

Security updates will be released as soon as possible after a vulnerability is confirmed. Updates will be:
- Tagged with version numbers
- Documented in release notes
- Announced in the repository README

## Compliance

This application follows modern Android security and privacy policies:
- Targets Android API 33 (Android 13)
- Implements network security configuration
- Excludes sensitive data from backups
- Documents all permission usage
- Follows Android best practices for data protection
