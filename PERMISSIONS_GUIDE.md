# Runtime Permission Handling

## Overview

The AndrOBD Home Assistant plugin now includes proper runtime permission handling for Android 6.0+ (API 23+). This ensures users are properly informed about why permissions are needed and can grant them through the app's settings interface.

## Permissions Required

### Location Permission (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
**Status:** Required for WiFi scanning on Android 6.0+

**Why it's needed:**
- Android requires location permission for WiFi scanning (security measure)
- Used to detect when home WiFi network is in range
- Enables automatic WiFi switching between OBD-II adapter and home network
- Future feature: Send vehicle GPS location to Home Assistant

**Important:** The app does NOT track your location continuously. Location permission is only used for WiFi network detection as required by Android's security model.

### Notification Permission (POST_NOTIFICATIONS)
**Status:** Required on Android 13+ (API 33+)

**Why it's needed:**
- Display foreground service notification (required by Android)
- Show network status indicator
- Display connection and transmission status updates

## Permission Request Flow

### First Launch
When the app is opened for the first time (or after permissions have been revoked):

1. **Permission Check:** App automatically checks if required permissions are granted
2. **Explanation Dialog:** If permissions are missing, a dialog explains what each permission is used for
3. **Permission Request:** User can choose to:
   - "Grant Permission" - Shows Android's permission request dialog
   - "Not Now" - Dismisses the dialog (can grant later)
   - "More Info" - Opens Android Settings for manual permission management

### Permission Grant
If user grants all permissions, a confirmation dialog shows:
- ✓ Scan for WiFi networks
- ✓ Detect home network proximity
- ✓ Enable automatic WiFi switching
- ✓ Display status notifications
- ✓ Future: Track vehicle location

### Permission Denial
If user denies any permission, a dialog explains the impact:
- **Location Denied:**
  - WiFi network scanning disabled
  - Cannot detect when home network is in range
  - Automatic WiFi switching unavailable
  - Location sensor feature unavailable

- **Notifications Denied:**
  - Status notifications may not appear
  - Network indicator may not display

The dialog provides a shortcut to Android Settings where permissions can be granted manually.

## User Experience

### Minimal Friction
- Permission request only happens once on first launch
- Clear explanation before requesting permissions
- Users can choose to grant later
- Easy access to settings if permissions are denied

### Transparency
- Clear explanation of what each permission is used for
- Explicit statement that location is NOT tracked
- List of features that will be enabled/disabled

### Compliance
- Follows Android best practices for runtime permissions
- Compatible with Android 6.0+ permission model
- Works on all Android versions (gracefully handles older versions)

## Technical Implementation

### Compatibility
- Uses AndroidX Core library for backward compatibility
- Checks Android version before requesting permissions
- Only requests POST_NOTIFICATIONS on Android 13+

### Permission States
The app handles three states:
1. **Granted:** All features enabled
2. **Denied:** Limited functionality, clear explanation provided
3. **Never Ask Again:** Directs user to Settings

### Testing
To test permission handling:

1. **Grant Flow:**
   ```bash
   # Install app
   adb install app.apk
   # Open app - should show permission dialog
   # Grant permissions - should show confirmation
   ```

2. **Denial Flow:**
   ```bash
   # Deny permissions when prompted
   # Should show limited functionality dialog
   ```

3. **Reset Permissions:**
   ```bash
   # Clear app data to reset permissions
   adb shell pm clear com.fr3ts0n.androbd.plugin.homeassistant
   ```

4. **Manual Settings:**
   ```bash
   # Check current permissions
   adb shell dumpsys package com.fr3ts0n.androbd.plugin.homeassistant | grep permission
   ```

## Future Location Sensor Feature

The location permission also enables a planned future feature:
- Send vehicle GPS coordinates to Home Assistant
- Create location-based automations (geo-fencing)
- Track vehicle location on map
- Historical trip logging with GPS data

This feature will be implemented in a future update and will respect user privacy:
- Optional (can be disabled in settings)
- Only sent when OBD-II is connected
- Not tracked when vehicle is off
- No background location tracking

## Troubleshooting

### Permission Dialog Not Appearing
- Check Android version (requires 6.0+)
- Ensure app is not in "Never Ask Again" state
- Clear app data and reinstall

### Features Not Working After Granting
- Restart the plugin service
- Check that permissions are actually granted in Android Settings
- Check logs for permission-related errors

### Manual Permission Management
Users can always manage permissions manually:
1. Open Android Settings
2. Go to Apps → AndrOBD Home Assistant
3. Tap Permissions
4. Enable/disable individual permissions

## Related Documentation
- [WIFI_DEBUGGING_GUIDE.md](WIFI_DEBUGGING_GUIDE.md) - WiFi detection debugging
- [README.md](README.md) - General setup and configuration
- [AndroidManifest.xml](src/main/AndroidManifest.xml) - Permission declarations
