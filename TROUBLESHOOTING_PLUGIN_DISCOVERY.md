# Troubleshooting Plugin Discovery

This guide helps diagnose and fix issues when the Home Assistant plugin doesn't appear in AndrOBD's Plugin Manager.

## Quick Checklist

If the plugin isn't showing up in AndrOBD, verify these items:

- [ ] Plugin APK is installed on the device
- [ ] AndrOBD is installed and up to date (v2.0.0 or higher)
- [ ] Both apps have been launched at least once
- [ ] Device is running Android 4.0.3 (API 15) or higher
- [ ] Device has been rebooted since installation (sometimes helps)

## Detailed Troubleshooting Steps

### Step 1: Verify Plugin Installation

Check that the plugin APK is actually installed:

```bash
adb shell pm list packages | grep homeassistant
```

Expected output:
```
package:com.fr3ts0n.androbd.plugin.homeassistant
```

If not present, install the plugin:
```bash
adb install -r AndrOBD-Plugin-Home-Assistant.apk
```

### Step 2: Verify AndrOBD Installation

Check AndrOBD is installed:

```bash
adb shell pm list packages | grep androbd
```

Expected output should include:
```
package:com.fr3ts0n.ecu.gui.androbd
package:com.fr3ts0n.androbd.plugin.homeassistant
```

If AndrOBD is not installed, get it from:
- F-Droid: https://f-droid.org/packages/com.fr3ts0n.ecu.gui.androbd/
- GitHub: https://github.com/fr3ts0n/AndrOBD/releases

### Step 3: Check Plugin Manifest

Verify the plugin's BroadcastReceiver is properly declared:

```bash
adb shell dumpsys package com.fr3ts0n.androbd.plugin.homeassistant | grep -A 20 "Receiver"
```

Look for output showing the receiver with the IDENTIFY intent filter:
```
Receiver #0:
  ...
  com.fr3ts0n.androbd.plugin.homeassistant.HomeAssistantPluginReceiver
  ...
  Action: "com.fr3ts0n.androbd.plugin.IDENTIFY"
  Category: "com.fr3ts0n.androbd.plugin.REQUEST"
```

### Step 4: Test Plugin Receiver Response

Manually send an IDENTIFY broadcast to test if the plugin responds:

```bash
# Clear logs
adb logcat -c

# Send IDENTIFY broadcast
adb shell am broadcast \
  -a com.fr3ts0n.androbd.plugin.IDENTIFY \
  -n com.fr3ts0n.androbd.plugin.homeassistant/.HomeAssistantPluginReceiver

# Check logs for response
adb logcat -d | grep -i "identify\|homeassistant\|plugin"
```

Expected log output should show:
1. Receiver getting the broadcast
2. Service being started
3. IDENTIFY response being sent back

Example successful output:
```
HomeAssistantPluginReceiver: Broadcast received: Intent { act=com.fr3ts0n.androbd.plugin.IDENTIFY ... }
HomeAssistantPlugin: <IDENTIFY: Intent { ... }
HomeAssistantPlugin: >IDENTIFY: Intent { act=com.fr3ts0n.androbd.plugin.IDENTIFY cat=[com.fr3ts0n.androbd.plugin.RESPONSE] ... }
```

### Step 5: Check Android Version Compatibility

Android 11 (API 30) and higher have package visibility restrictions that can affect plugin discovery.

Check Android version:
```bash
adb shell getprop ro.build.version.sdk
```

Version codes:
- 29 = Android 10
- 30 = Android 11
- 31 = Android 12
- 33 = Android 13
- 34 = Android 14

If API 30+, verify AndrOBD has proper `<queries>` declaration. AndrOBD should have been updated to handle this, but if using an old version, update to the latest.

### Step 6: Check Service Permissions

Verify the plugin service is exported and enabled:

```bash
adb shell dumpsys package com.fr3ts0n.androbd.plugin.homeassistant | grep -A 10 "Service"
```

Look for:
```
Service #0:
  ...
  exported=true
  enabled=true
```

### Step 7: Test Direct Service Start

Try starting the plugin service directly:

```bash
adb shell am startservice \
  -n com.fr3ts0n.androbd.plugin.homeassistant/.HomeAssistantPlugin \
  -a com.fr3ts0n.androbd.plugin.IDENTIFY
```

Check logs:
```bash
adb logcat -d | grep HomeAssistant
```

If this fails, there may be an issue with the service implementation.

### Step 8: Check for Installation Conflicts

Sometimes installation issues can cause problems. Try a clean reinstall:

```bash
# Uninstall both apps
adb uninstall com.fr3ts0n.androbd.plugin.homeassistant
adb uninstall com.fr3ts0n.ecu.gui.androbd

# Reboot device
adb reboot

# Wait for reboot to complete...

# Reinstall AndrOBD first
adb install AndrOBD-v2.x.x.apk

# Then install plugin
adb install AndrOBD-Plugin-Home-Assistant.apk

# Launch AndrOBD
adb shell am start -n com.fr3ts0n.ecu.gui.androbd/.Activity
```

### Step 9: Check Plugin Manager in AndrOBD

After installation, open AndrOBD and navigate to the Plugin Manager:

1. Open **AndrOBD**
2. Tap the menu (three dots or bars)
3. Go to **Settings** → **Extensions** or **Plugin extensions** or **Plugin Manager**
   - The exact menu name varies by AndrOBD version
4. Look for **Home Assistant** or **Home Assistant Publisher**

If not visible, try:
- Pull down to refresh the list
- Go back and re-enter the plugin manager
- Restart AndrOBD

### Step 10: Monitor AndrOBD Plugin Discovery

Watch AndrOBD's plugin discovery process in real-time:

```bash
# Clear logs
adb logcat -c

# Start logging
adb logcat | grep -i "plugin\|identify" &

# Open AndrOBD and go to Plugin Manager
# You should see IDENTIFY broadcasts being sent and responses received
```

Look for patterns like:
```
PluginHandler: >IDENTIFY: Intent { act=com.fr3ts0n.androbd.plugin.IDENTIFY ... }
PluginHandler: Plugin identified: com.fr3ts0n.androbd.plugin.homeassistant.HomeAssistantPlugin
```

## Common Issues and Solutions

### Issue: Plugin installed but not visible in Plugin Manager

**Possible Causes:**
1. AndrOBD needs to be restarted
2. Plugin Manager cache needs refresh
3. Android 11+ visibility restrictions

**Solutions:**
1. Force stop AndrOBD:
   ```bash
   adb shell am force-stop com.fr3ts0n.ecu.gui.androbd
   ```
2. Clear AndrOBD's cache:
   ```bash
   adb shell pm clear com.fr3ts0n.ecu.gui.androbd
   ```
   Note: This will reset AndrOBD settings!
3. Restart the device
4. Update AndrOBD to the latest version

### Issue: Receiver doesn't respond to broadcasts

**Possible Causes:**
1. Receiver not properly registered in manifest
2. Plugin service fails to start
3. Permission issues

**Solutions:**
1. Verify manifest (see Step 3 above)
2. Check service logs when broadcast is sent
3. Ensure `android:exported="true"` on both receiver and service
4. Rebuild and reinstall the plugin APK

### Issue: Service starts but doesn't send IDENTIFY response

**Possible Causes:**
1. Plugin doesn't implement getPluginInfo() correctly
2. Error in plugin initialization
3. Missing required plugin metadata

**Solutions:**
1. Check plugin logs for errors:
   ```bash
   adb logcat | grep -i error
   ```
2. Verify PluginInfo is correctly configured in HomeAssistantPlugin.java
3. Check that all required interfaces are implemented

### Issue: Works on older Android but not Android 11+

**Cause:**
Package visibility restrictions in Android 11 (API 30) and higher.

**Solution:**
AndrOBD must declare a `<queries>` element to see plugins. This should be in AndrOBD's manifest, not the plugin's. Update AndrOBD to the latest version which should include this fix.

If developing AndrOBD, add to its AndroidManifest.xml:
```xml
<queries>
    <intent>
        <action android:name="com.fr3ts0n.androbd.plugin.IDENTIFY" />
    </intent>
</queries>
```

### Issue: "Package installer invalid" when installing

**Possible Causes:**
1. APK signature issues
2. Build configuration problems

**Solutions:**
1. Check build.gradle has proper signing config
2. Use debug build for testing:
   ```bash
   ./gradlew assembleDebug
   ```
3. For release, ensure proper signing:
   ```bash
   ./gradlew assembleRelease
   ```

## Advanced Debugging

### Enable Verbose Logging

Get maximum log output:

```bash
# Set log level to verbose
adb shell setprop log.tag.PluginHandler VERBOSE
adb shell setprop log.tag.PluginReceiver VERBOSE
adb shell setprop log.tag.HomeAssistantPlugin VERBOSE

# Then run your tests
```

### Capture Complete Log Session

Capture everything for detailed analysis:

```bash
# Clear logs
adb logcat -c

# Start logging to file
adb logcat > plugin-debug.log &
LOGPID=$!

# Perform your tests:
# 1. Open AndrOBD
# 2. Go to Plugin Manager
# 3. Try to enable plugin

# Wait 30 seconds
sleep 30

# Stop logging
kill $LOGPID

# Analyze log
grep -i "identify\|plugin\|homeassistant\|error\|exception" plugin-debug.log
```

### Inspect APK Contents

Verify APK is properly built:

```bash
# List contents
unzip -l AndrOBD-Plugin-Home-Assistant.apk | grep -i manifest

# Extract and read manifest (requires apktool)
apktool d AndrOBD-Plugin-Home-Assistant.apk
cat AndrOBD-Plugin-Home-Assistant/AndroidManifest.xml
```

### Test with ADB Intent Injection

Simulate complete plugin discovery flow:

```bash
# 1. Send IDENTIFY request
adb shell am broadcast \
  -a com.fr3ts0n.androbd.plugin.IDENTIFY \
  -n com.fr3ts0n.androbd.plugin.homeassistant/.HomeAssistantPluginReceiver

# 2. Watch for response in logs
adb logcat -d | grep -A 5 ">IDENTIFY"

# 3. Test CONFIGURE intent
adb shell am startservice \
  -n com.fr3ts0n.androbd.plugin.homeassistant/.HomeAssistantPlugin \
  -a com.fr3ts0n.androbd.plugin.CONFIGURE

# 4. Test DATA intent
adb shell am startservice \
  -n com.fr3ts0n.androbd.plugin.homeassistant/.HomeAssistantPlugin \
  -a com.fr3ts0n.androbd.plugin.DATA \
  --es com.fr3ts0n.androbd.plugin.extra.DATA "ENGINE_RPM=2500"
```

## Getting Help

If you've tried all these steps and the plugin still doesn't appear:

1. **Gather Information:**
   ```bash
   # Device info
   adb shell getprop ro.build.version.release  # Android version
   adb shell getprop ro.build.version.sdk      # API level
   
   # Package versions
   adb shell dumpsys package com.fr3ts0n.ecu.gui.androbd | grep version
   adb shell dumpsys package com.fr3ts0n.androbd.plugin.homeassistant | grep version
   ```

2. **Capture Logs:**
   - Follow "Capture Complete Log Session" above
   - Include the complete log file when reporting issues

3. **Report Issue:**
   - GitHub Issues: https://github.com/ian-morgan99/AndrOBD-Plugin-Home-Assistant/issues
   - Include:
     - Device model and Android version
     - AndrOBD version
     - Plugin version
     - Complete logs
     - Steps taken so far

4. **Community Support:**
   - AndrOBD Telegram: https://t.me/joinchat/G60ltQv5CCEQ94BZ5yWQbg
   - AndrOBD Matrix: https://matrix.to/#/#AndrOBD:matrix.org

## Reference Documents

- [AndrOBD Plugin Development](https://github.com/fr3ts0n/AndrOBD-Plugin)
- [Android Package Visibility](https://developer.android.com/training/package-visibility)
- [Android BroadcastReceiver Guide](https://developer.android.com/guide/components/broadcasts)
- [TESTING_WITHOUT_OBD.md](TESTING_WITHOUT_OBD.md) - Testing with emulators

## Success Indicators

When everything is working correctly, you should see:

1. **In Plugin Manager:**
   - "Home Assistant" or "Home Assistant Publisher" listed
   - Can tap to enable/disable
   - Shows description: "Send OBD data to Home Assistant"

2. **In Logs:**
   ```
   PluginHandler: >IDENTIFY: Intent { act=com.fr3ts0n.androbd.plugin.IDENTIFY ... }
   HomeAssistantPluginReceiver: Broadcast received
   HomeAssistantPlugin: <IDENTIFY: Intent { ... }
   HomeAssistantPlugin: >IDENTIFY: Intent { cat=[...RESPONSE] ... }
   PluginHandler: Plugin identified: com.fr3ts0n.androbd.plugin.homeassistant.HomeAssistantPlugin
   ```

3. **When Enabled:**
   - Plugin service stays running
   - Receives data updates from AndrOBD
   - Shows notification (if configured)
   - Sends data to Home Assistant

If you see all of these, your plugin is successfully integrated!
