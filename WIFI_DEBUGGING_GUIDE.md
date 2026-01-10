# WiFi Detection Debugging Guide

This guide explains how to use the enhanced logging to debug WiFi detection issues in the Home Assistant plugin.

## Overview

The plugin has been enhanced with comprehensive logging to help diagnose issues where:
- WiFi networks appear to be in range but aren't detected by the plugin
- Automatic network switching doesn't work as expected
- Data transmission conditions aren't being met

## Enabling Logging

1. Open the plugin settings in AndrOBD
2. Enable "Enable Logging" option
3. Use the "View Logs" button to see real-time logs

## Understanding the Enhanced Logs

### Initialization Logging

When the plugin starts, you'll see:

```
[timestamp] INFO: Android version: 33 (13), Device: Samsung SM-G998B
[timestamp] INFO: WiFi enabled: true
[timestamp] INFO: Plugin initialization complete
```

**What to check:**
- Confirm Android version matches your device
- Verify WiFi is enabled
- If WiFi is disabled, the plugin cannot scan for networks

### WiFi State Check Cycle

Every 30 seconds (in ssid_in_range mode), the plugin checks WiFi state. Look for this structure:

```
[timestamp] DEBUG: === WiFi State Check Started ===
[timestamp] DEBUG: Transmission mode: ssid_in_range
[timestamp] DEBUG: Configured Home SSID: 'MyHomeNetwork'
[timestamp] DEBUG: Configured OBD SSID: 'OBDII' (auto-switch enabled)
```

**What to check:**
- Verify transmission mode is correct (realtime, ssid_connected, or ssid_in_range)
- Confirm configured SSIDs match your networks exactly (case-sensitive)
- Check if SSIDs have any special characters or spaces

### WiFi Scanning Details

When scanning for networks, you'll see detailed output:

```
[timestamp] DEBUG: Scanning for WiFi SSID: 'MyHomeNetwork'
[timestamp] DEBUG: WiFi enabled: true
[timestamp] DEBUG: WiFi scan started: true (Android 33)
[timestamp] DEBUG: WiFi scan found 8 networks:
[timestamp] DEBUG:   - SSID: 'MyHomeNetwork' Signal: -45 dBm
[timestamp] DEBUG:   - SSID: 'NeighborWiFi' Signal: -72 dBm
[timestamp] DEBUG:   - SSID: 'OBDII' Signal: -38 dBm
[timestamp] DEBUG:   - SSID: '<hidden>' Signal: -80 dBm
[timestamp] INFO: Target WiFi found in range: 'MyHomeNetwork' (Signal: -45 dBm)
```

**What to check:**
- If "WiFi scan found 0 networks" appears, the device isn't detecting ANY networks
  - This could indicate location permissions are not granted (required on Android 6+)
  - WiFi scanning may be throttled (Android 9+ limits background scans)
- If networks are found but your target isn't listed:
  - Verify the SSID spelling matches exactly (including capitalization)
  - Check if your network is hidden (will show as `<hidden>`)
  - For hidden networks, the plugin cannot detect them by SSID
- Signal strength indicators:
  - -30 to -50 dBm: Excellent signal
  - -50 to -60 dBm: Good signal
  - -60 to -70 dBm: Fair signal
  - -70+ dBm: Weak signal (may have connectivity issues)

### WiFi Not Found Case

If the target WiFi isn't found:

```
[timestamp] DEBUG: Target WiFi 'MyHomeNetwork' NOT found in scan results
```

**Common causes:**
1. **SSID mismatch**: Double-check spelling and capitalization
2. **Hidden network**: Plugin cannot detect hidden SSIDs by name
3. **Out of range**: Device is too far from the access point
4. **Network disabled**: The access point is turned off
5. **5GHz vs 2.4GHz**: Some devices can't scan 5GHz networks

### Connection Check Details

When checking if connected to a specific network:

```
[timestamp] DEBUG: Connection check: Currently connected to 'OBDII', target is 'MyHomeNetwork', match: false
```

**What to check:**
- Verify you're connected to the expected network
- If "No active network" appears, device has no internet connection
- If "Not connected to WiFi (type: MOBILE)", device is using cellular data

### Permission Issues

If you see security exceptions:

```
[timestamp] ERROR: Security exception during WiFi scan - location permission may be required (Android 33): ...
```

**What to do:**
1. Go to Android Settings → Apps → AndrOBD Plugin Home Assistant
2. Grant Location permission (required for WiFi scanning on Android 6+)
3. Restart the plugin

### Transmission Mode Conditions

When checking if data should be sent:

```
[timestamp] DEBUG: Checking if data should be sent - Mode: ssid_in_range, Internet: true, Home connected: false, Home in range: true
[timestamp] DEBUG: SSID in range mode check: connected=false, in_range=true, internet=true -> SKIP
[timestamp] INFO: Home WiFi in range but not connected - switch networks to transmit buffered data
```

**Understanding transmission modes:**

1. **realtime**: Sends whenever internet is available
   - Check: "Internet: true"

2. **ssid_connected**: Only sends when connected to home WiFi
   - Check: "Home connected: true" AND "Internet: true"

3. **ssid_in_range**: Sends when connected to home WiFi (not just in range)
   - Check: "Home connected: true" AND "Internet: true"
   - If "Home in range: true" but "Home connected: false", you need to switch networks

### Empty Scan Results

If scans consistently return empty:

```
[timestamp] WARNING: WiFi scan results are empty - no networks detected (0 networks)
```

**Possible causes:**
1. **Android 9+ throttling**: Background WiFi scans are limited
   - Apps can only scan 4 times per 2-minute period when in background
   - Solution: Keep AndrOBD in foreground or wait between scans
2. **Location services disabled**: Required for WiFi scanning
   - Enable Location in Android settings
3. **WiFi disabled**: Turn on WiFi in Android settings
4. **Permission denied**: Grant location permission to the app

## Common Issues and Solutions

### Issue: "WiFi state - Home in range: false" but I'm definitely in range

**Debug steps:**
1. Check if scan found ANY networks at all
2. Verify SSID spelling matches exactly
3. Confirm location permission is granted
4. Check if network is hidden
5. Verify WiFi is enabled on device
6. Check Android version for scan throttling (Android 9+)

### Issue: Networks found but target not in list

**Debug steps:**
1. Compare configured SSID with scan results list exactly
2. Check for extra spaces or special characters
3. Try connecting to the network manually, then check logs to see the exact SSID format
4. For hidden networks, you cannot use SSID-based detection

### Issue: Scan started returns false

```
[timestamp] DEBUG: WiFi scan started: false (Android 33)
```

**This means:**
- On Android 9+, scan request was throttled (too many recent scans)
- Wait 2 minutes and try again
- Keep app in foreground to avoid throttling

### Issue: Permission errors on every scan

**Solution:**
1. Open Android Settings
2. Go to Apps → AndrOBD Plugin Home Assistant
3. Go to Permissions
4. Grant Location permission (Fine Location or Coarse Location)
5. Restart the app

## Testing Your Configuration

1. **Test with logs enabled:**
   - Enable logging in plugin settings
   - Configure your Home WiFi SSID
   - Wait for next WiFi check cycle (30 seconds)
   - View logs and verify your network appears in scan results

2. **Verify SSID matching:**
   - Connect to your home WiFi manually
   - Check logs for "Connection check: Currently connected to 'XXX'"
   - Copy the exact SSID format (including quotes if any)
   - Use that exact format in plugin settings

3. **Test transmission modes:**
   - Try each mode (realtime, ssid_connected, ssid_in_range)
   - Check "Checking if data should be sent" log entries
   - Verify the conditions match your expectations

## Log Analysis Tips

1. **Search for "=== WiFi State Check Started ==="** to find the beginning of each check cycle
2. **Look for "WARNING" or "ERROR"** entries to find problems quickly
3. **Check the "WiFi scan found X networks"** count to verify scanning is working
4. **Compare configured SSIDs** with the actual scan results list
5. **Watch "Internet: true/false"** to understand connectivity issues

## Reporting Issues

When reporting WiFi detection problems, include:
1. Full log section from "=== WiFi State Check Started ===" to "=== WiFi State Check Complete ==="
2. Your Android version (from initialization logs)
3. Your transmission mode setting
4. Configured Home and OBD SSIDs (you can redact the actual names but show the format)
5. Expected behavior vs actual behavior

## Additional Resources

- [README.md](README.md) - Plugin setup and configuration
- [TROUBLESHOOTING_PLUGIN_DISCOVERY.md](TROUBLESHOOTING_PLUGIN_DISCOVERY.md) - Plugin visibility issues
- [QUICK_START.md](QUICK_START.md) - Getting started guide
