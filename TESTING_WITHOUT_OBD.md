# Testing AndrOBD Plugin Without a Physical OBD Device

This guide explains how to test the AndrOBD Home Assistant plugin without connecting to a real vehicle or OBD-II adapter.

## Overview

There are several options for testing AndrOBD plugins without a physical OBD device:

1. **AndrOBD Built-in Demo Mode** (Recommended - Easiest)
2. **ELM327-emulator (Python-based)**
3. **ECU Engine Sim (Android App)**
4. **Hardware Emulators** (Most realistic but requires purchase)

## Option 1: AndrOBD Built-in Demo Mode (Recommended)

AndrOBD has a built-in demo mode that simulates live OBD data without requiring any external setup.

### Steps:

1. **Install AndrOBD**
   - Download from [F-Droid](https://f-droid.org/packages/com.fr3ts0n.ecu.gui.androbd/)
   - Or build from source: [GitHub](https://github.com/fr3ts0n/AndrOBD)

2. **Install the Home Assistant Plugin**
   - Build the plugin: `./gradlew assembleDebug`
   - Install the APK: `adb install -r src/build/outputs/apk/debug/AndrOBD-Plugin-Home-Assistant-debug.apk`

3. **Enable Demo Mode in AndrOBD**
   - Open AndrOBD
   - Go to **Settings**
   - Look for **Demo mode** or **Simulation mode**
   - Enable it

4. **Configure the Plugin**
   - Open the **AndrOBD Home Assistant** plugin app
   - Configure your Home Assistant URL and bearer token
   - Set transmission mode to "Real-time" for testing

5. **Enable Plugin in AndrOBD**
   - Open AndrOBD
   - Go to **Settings** → **Plugin extensions**
   - Find and enable **Home Assistant Publisher** plugin
   - The plugin should now receive simulated OBD data from AndrOBD's demo mode

6. **Verify Data Transmission**
   - In Home Assistant, go to **Developer Tools** → **States**
   - Search for `sensor.androbd_` entities
   - You should see sensors being created with simulated data

### Advantages:
- No additional software needed
- Works entirely on the Android device
- Perfect for quick testing and development
- Most straightforward setup

## Option 2: Python ELM327-emulator

The ELM327-emulator is a Python tool that emulates an ELM327 OBD-II adapter.

### Prerequisites:
- Python 3.x installed on your computer
- Android device with Bluetooth or WiFi connectivity
- AndrOBD installed on Android device

### Installation:

```bash
pip install ELM327-emulator
```

### Basic Usage:

#### TCP/IP Mode (WiFi):

1. Start the emulator on your computer:
```bash
elm -n -s car
```
This starts the emulator listening on TCP port 35000.

2. Find your computer's IP address:
   - Linux/Mac: `ifconfig` or `ip addr`
   - Windows: `ipconfig`

3. In AndrOBD on your Android device:
   - Go to **Settings** → **Connection**
   - Select **Network (WiFi/LAN)**
   - Enter your computer's IP address and port 35000
   - Connect

#### Bluetooth Mode:

1. Set up Bluetooth serial port on your computer
2. Start the emulator with Bluetooth:
```bash
elm -s car --device /dev/rfcomm0
```
Replace `/dev/rfcomm0` with your Bluetooth serial port.

3. Pair your Android device with your computer
4. In AndrOBD, connect via Bluetooth

### Advanced Features:

**Custom ECU Simulation:**
```bash
# Simulate specific vehicle responses
elm -s car --plugin my_vehicle.py
```

**Multi-ECU Simulation:**
```bash
elm -s multi_ecu
```

**Error Injection:**
```bash
# Inject specific DTCs or errors for testing error handling
elm -s fault_injection
```

### Advantages:
- Highly configurable
- Supports multiple ECU simulation
- Can inject faults and edge cases
- Works over network (WiFi) or Bluetooth
- Good for testing error handling

### Resources:
- [ELM327-emulator on PyPI](https://pypi.org/project/ELM327-emulator/)
- [Documentation](https://github.com/Ircama/ELM327-emulator)

## Option 3: ECU Engine Sim (Android App)

ECU Engine Sim turns one Android device into an OBD-II adapter emulator that another Android device can connect to.

### Setup:

1. **Install on First Android Device (Emulator)**:
   - Download [ECU Engine Sim](https://play.google.com/store/apps/details?id=com.obdii_lqc.android.obdii.elm327.ecusimfree) from Google Play
   - This device will act as the "vehicle"

2. **Configure ECU Engine Sim**:
   - Open ECU Engine Sim
   - Enable Bluetooth
   - Configure simulated sensors and values
   - Start broadcasting

3. **Connect from Second Device (AndrOBD)**:
   - Install AndrOBD on your second Android device
   - Pair with the first device via Bluetooth
   - Connect to the simulated OBD adapter
   - The Home Assistant plugin should receive the simulated data

### Advantages:
- No computer required
- Tests actual Bluetooth communication path
- More realistic than demo mode
- Can simulate specific sensor values

### Disadvantages:
- Requires two Android devices
- Free version has limitations
- Less flexible than Python emulator

## Option 4: Hardware OBD-II Emulators

For production-grade testing, consider hardware emulators:

### Freematics OBD-II Emulator MK2
- **Type**: Hardware device
- **Features**: Emulates full OBD-II port including physical layer
- **Use case**: Professional testing, timing-sensitive tests
- **Cost**: ~$30-50 USD
- **Link**: [Freematics Store](https://freematics.com/products/obd-emulator/)

### Advantages:
- Most realistic testing environment
- Tests all layers including physical
- Can simulate voltage levels and timing
- Professional quality

### Disadvantages:
- Requires purchase
- Overkill for software-only testing

## Testing the Plugin

Regardless of which emulation method you choose, follow these steps to test the plugin:

### 1. Verify Plugin Discovery

First, ensure the plugin appears in AndrOBD:

1. Open AndrOBD
2. Go to **Settings** → **Plugin extensions** or **Plugin Manager**
3. Look for **Home Assistant** or **Home Assistant Publisher** in the list
4. If not visible, check:
   - Plugin APK is installed (`adb shell pm list packages | grep homeassistant`)
   - Plugin manifest has correct intent filter
   - AndrOBD is updated to latest version

### 2. Configure Plugin Settings

1. Open the **AndrOBD Home Assistant** standalone app
2. Configure:
   - **Enable Home Assistant**: Check this
   - **Home Assistant URL**: Your HA instance URL (e.g., `https://homeassistant.local:8123`)
   - **Bearer Token**: Long-lived access token from Home Assistant
   - **Transmission Mode**: Use "Real-time" for testing
   - **Update Interval**: 5000ms (5 seconds) is good for testing

### 3. Test Data Flow

1. **Start Emulator** (if using external emulator)
2. **Connect AndrOBD** to the emulator or enable demo mode
3. **Start Data Collection** in AndrOBD
4. **Monitor Plugin Logs**:
   ```bash
   adb logcat | grep HomeAssistant
   ```
5. **Check Home Assistant**:
   - Go to **Developer Tools** → **States**
   - Search for `sensor.androbd_`
   - Verify sensors are being created and updated

### 4. Test Different Scenarios

Test these scenarios to ensure robust operation:

**Scenario 1: Real-time Mode**
- Set transmission mode to "Real-time"
- Ensure data flows continuously while connected

**Scenario 2: SSID Connected Mode**
- Set transmission mode to "SSID Connected"
- Configure your WiFi SSID
- Verify data is only sent when connected to specified WiFi

**Scenario 3: Network Failures**
- Temporarily disconnect from WiFi/network
- Verify plugin buffers data
- Reconnect and verify buffered data is sent

**Scenario 4: Invalid Credentials**
- Use an invalid bearer token
- Check that errors are logged properly
- Verify plugin handles auth failures gracefully

### 5. Debugging Tips

**Check Plugin is Running:**
```bash
adb shell dumpsys activity services | grep -i homeassistant
```

**View Plugin Logs:**
```bash
adb logcat | grep -E "HomeAssistant|androbd\.plugin"
```

**Check if Plugin Responds to IDENTIFY:**
```bash
adb logcat | grep IDENTIFY
```

**View All OBD Data:**
```bash
adb logcat | grep -i "obd\|elm327"
```

## Common Issues and Solutions

### Issue: Plugin doesn't appear in AndrOBD Plugin Manager

**Possible causes:**
1. Plugin APK not installed
2. AndrOBD version too old
3. Android 11+ package visibility restrictions
4. Incorrect manifest configuration

**Solutions:**
1. Verify installation: `adb shell pm list packages | grep homeassistant`
2. Update AndrOBD to latest version
3. Check manifest has correct intent filter with `android:exported="true"`
4. Try uninstalling and reinstalling both AndrOBD and plugin

### Issue: Plugin appears but doesn't receive data

**Possible causes:**
1. Plugin not properly enabled in AndrOBD
2. Demo mode not working
3. Emulator not connected

**Solutions:**
1. Check plugin is enabled in AndrOBD settings
2. Restart AndrOBD after enabling plugin
3. Check emulator connectivity
4. View logs: `adb logcat | grep HomeAssistant`

### Issue: Data not appearing in Home Assistant

**Possible causes:**
1. Incorrect Home Assistant URL
2. Invalid bearer token
3. Network connectivity issues
4. HTTPS certificate problems

**Solutions:**
1. Test HA URL in browser from Android device
2. Regenerate bearer token in Home Assistant
3. Check network connectivity
4. For self-signed certificates, install CA cert on device
5. Temporarily use HTTP localhost for testing

## Example Test Session

Here's a complete testing session using AndrOBD demo mode:

```bash
# 1. Install apps
adb install -r AndrOBD-v2.x.x.apk
adb install -r AndrOBD-Plugin-Home-Assistant-debug.apk

# 2. Configure and start logging
adb logcat -c  # Clear logs
adb logcat | grep -E "HomeAssistant|IDENTIFY|Plugin" > plugin-test.log &

# 3. Open apps and configure
# (Do this manually on device)
# - Open AndrOBD, enable demo mode
# - Open HA Plugin, configure URL and token
# - In AndrOBD, enable HA plugin

# 4. Wait 30 seconds for data to flow

# 5. Check results
# - View plugin-test.log for activity
# - Check Home Assistant for sensors
# - Verify data is updating

# 6. Stop logging
kill %1  # Stop logcat background job
```

## Continuous Integration Testing

For automated testing without OBD hardware:

1. **Use AndrOBD Demo Mode** in automated tests
2. **Mock Home Assistant Server** using test HTTP server
3. **Verify Plugin Behavior** by checking HTTP requests
4. **Test Error Handling** by simulating failures

Example test structure:
```
tests/
  ├── mock-homeassistant-server.py  # Fake HA server
  ├── test-plugin-discovery.sh       # Verify plugin appears
  ├── test-data-flow.sh              # Verify data transmission
  └── test-error-handling.sh         # Test failure scenarios
```

## Additional Resources

- **AndrOBD**: https://github.com/fr3ts0n/AndrOBD
- **AndrOBD Plugin Framework**: https://github.com/fr3ts0n/AndrOBD-libplugin
- **ELM327 Command Reference**: https://www.elmelectronics.com/wp-content/uploads/2017/01/ELM327DS.pdf
- **Home Assistant REST API**: https://developers.home-assistant.io/docs/api/rest/
- **AndrOBD Telegram Group**: https://t.me/joinchat/G60ltQv5CCEQ94BZ5yWQbg

## Contributing

If you develop improvements to the testing process or find new emulation tools, please contribute:

1. Fork the repository
2. Add your documentation or scripts
3. Submit a pull request

## Support

For issues or questions:
- GitHub Issues: https://github.com/ian-morgan99/AndrOBD-Plugin-Home-Assistant/issues
- AndrOBD Community: https://t.me/joinchat/G60ltQv5CCEQ94BZ5yWQbg
