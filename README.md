# AndrOBD Home Assistant Plugin

This extension plugin allows AndrOBD to publish OBD-II vehicle data to Home Assistant via webhooks or REST API.

## Features

- **Real-time Data Transmission**: Continuously send OBD data to Home Assistant as it's collected
- **SSID-Triggered Mode**: Buffer data and transmit only when connected to a specific WiFi network
- **Secure Communication**: Support for HTTPS and Bearer token authentication
- **Selective Data Publishing**: Choose which OBD data items to publish
- **Comprehensive Data**: Sends key vehicle metrics including:
  - Engine RPM
  - Vehicle speed
  - Battery voltage
  - Coolant temperature
  - Fuel level and pressure
  - Lambda sensor readings
  - Efficiency metrics
  - And many more OBD parameters

## Installation

### From Source

1. Clone this repository
2. Ensure you have the AndrOBD plugin library submodule initialized:
   ```bash
   git submodule init
   git submodule update
   ```
3. Build the project using Android Studio or Gradle:
   ```bash
   ./gradlew assembleRelease
   ```
4. Install the APK on your Android device

### Requirements

- AndrOBD v2.0.0 or higher installed on your device
- Android 4.0.3 (API 15) or higher
- Network connectivity (WiFi or mobile data)

## Configuration

### 1. Setting up Home Assistant

This plugin uses the Home Assistant REST API to create sensor entities. You need to generate a long-lived access token:

1. In Home Assistant, click on your profile (bottom left)
2. Scroll down to **Long-Lived Access Tokens**
3. Click **Create Token**
4. Give it a name (e.g., "AndrOBD Plugin")
5. Copy the token - you'll need it for the plugin configuration

Your API URL will be: `https://your-homeassistant-url:8123`

### 2. Configuring the Plugin

1. Open the **AndrOBD Home Assistant** plugin app
2. Configure the following settings:

#### Required Settings:
- **Enable Home Assistant**: Check to enable the integration
- **Home Assistant URL**: Enter your Home Assistant base URL
  - Example: `https://homeassistant.local:8123`
  - Do NOT include `/api/states` or entity names
- **Bearer Token**: Paste the long-lived access token you created above

#### Optional Settings:
- **Transmission Mode**: Choose between:
  - **Real-time**: Send data continuously while connected to OBD (requires internet connection)
  - **SSID Connected**: Only send data when connected to specific WiFi network
  - **SSID in Range**: Send data when connected to home WiFi (supports automatic switching for WiFi OBD adapters)
- **Home WiFi SSID**: Your home WiFi network name (required for SSID-based modes)
- **OBD WiFi SSID**: Your OBD adapter's WiFi network name (required for automatic switching)
- **Enable Auto WiFi Switching**: Check to enable automatic network switching between OBD and home WiFi (requires both SSIDs configured)
- **Update Interval**: How often to send data in milliseconds (default: 5000ms = 5 seconds)
- **Data Items**: Select specific OBD parameters to publish (leave empty to publish all)

### 3. Connecting in AndrOBD

1. Open **AndrOBD**
2. Go to **Settings** → **Plugin extensions**
3. Enable the **Home Assistant Publisher** plugin

## Transmission Modes

### Real-time Mode
In real-time mode, the plugin sends OBD data to Home Assistant continuously at the specified update interval. This is ideal for:
- Home setups with VPN or local network access
- Monitoring vehicle data while parked at home
- Real-time dashboards and automations
- USB or Bluetooth OBD connections that don't interfere with WiFi

### SSID Connected Mode
In SSID connected mode, the plugin buffers data and only transmits when the Android device is actively connected to your specified WiFi network. This is ideal for:
- Vehicles with Android head units
- Mobile phones that travel with the vehicle
- Reducing mobile data usage
- Automatic synchronization when arriving home

### SSID in Range Mode (Recommended for WiFi OBD Adapters)
In SSID in range mode, the plugin detects when your home WiFi network is within range. This mode supports both **manual** and **automatic** WiFi switching for wireless OBD adapters that occupy the device's WiFi connection.

#### Manual Switching Mode (Default)
- **Smart WiFi Detection**: Continuously scans for your home WiFi network
- **Manual Network Switching**: User manually switches from OBD WiFi to home WiFi to transmit
- **Connectivity Verification**: Always checks for internet connectivity before attempting transmission
- **Seamless Operation**: Allows alternating between OBD WiFi connection and home WiFi for data upload

**How it works (Manual):**
1. Device connects to OBD adapter's WiFi network to collect vehicle data
2. Plugin buffers the OBD data locally while connected to OBD WiFi
3. Plugin periodically scans for home WiFi network in range (every 30 seconds)
4. When home WiFi is detected in range, plugin logs a message indicating you can switch networks
5. **User manually switches** from OBD WiFi to home WiFi in device settings
6. Wait a few seconds for stable connection and internet connectivity
7. Plugin automatically transmits all buffered data to Home Assistant
8. After successful transmission, **user manually switches** back to OBD WiFi to continue data collection

#### Automatic Switching Mode (New!)
Enable automatic WiFi switching for truly hands-free operation:
- **Automatic Detection**: Plugin detects both home and OBD WiFi networks
- **Smart Switching**: Automatically switches to home WiFi when in range and data needs transmission
- **Auto Return**: Automatically switches back to OBD WiFi after transmission completes
- **Requires Configuration**: Both Home WiFi SSID and OBD WiFi SSID must be configured

**How it works (Automatic):**
1. Configure both "Home WiFi SSID" and "OBD WiFi SSID" in settings
2. Enable "Enable Auto WiFi Switching" option
3. Plugin automatically connects to OBD WiFi to collect vehicle data
4. Data is buffered locally while connected to OBD WiFi
5. When home WiFi is detected in range AND buffered data exists:
   - **Automatically switches** to home WiFi
   - Waits 5 seconds for connection to stabilize
   - Transmits all buffered data to Home Assistant
   - **Automatically switches** back to OBD WiFi
6. When leaving home range, automatically reconnects to OBD WiFi

**Important notes:**
- Automatic switching requires `CHANGE_WIFI_STATE` permission (already included)
- Both WiFi networks must be saved in device settings before automatic switching works
- Transmission only occurs when actively connected to home WiFi with internet access
- The plugin verifies internet connectivity before every transmission attempt
- Failed transmissions are logged with detailed error messages for troubleshooting
- On Android 6.0+, location permission is required for WiFi scanning functionality

## Data Format

The plugin sends individual OBD sensor data to Home Assistant using the REST API `/api/states/` endpoint. Each OBD parameter is sent as a separate entity.

**HTTP Request Format:**
```
POST https://your-homeassistant-url:8123/api/states/sensor.androbd_engine_rpm
Authorization: Bearer YOUR_TOKEN
Content-Type: application/json

{
  "state": "2500",
  "attributes": {
    "friendly_name": "ENGINE_RPM",
    "source": "AndrOBD"
  }
}
```

**Entity Naming:**
- OBD keys are converted to valid Home Assistant entity IDs
- Default prefix: `sensor.androbd_`
- Keys are lowercased and special characters replaced with underscores
- Examples:
  - `ENGINE_RPM` → `sensor.androbd_engine_rpm`
  - `COOLANT_TMP` → `sensor.androbd_coolant_tmp`
  - `SPEED` → `sensor.androbd_speed`

**Data Transmission:**
- Each OBD parameter is sent as an individual POST request
- Only selected data items are sent (or all if none selected)
- Data is buffered locally and sent based on transmission mode settings
- Failed transmissions are logged with error details

**What gets sent to Home Assistant:**
- **state**: The current value of the OBD parameter (e.g., "2500", "65", "90.5")
- **attributes.friendly_name**: The original OBD key name for display
- **attributes.source**: Always set to "AndrOBD" to identify the data source

## Using Data in Home Assistant

### Accessing the Sensor Data

The plugin creates individual sensor entities in Home Assistant that you can use directly. No additional configuration is needed - the sensors appear automatically when data is first sent.

**Entity IDs created:**
- `sensor.androbd_engine_rpm` - Engine RPM
- `sensor.androbd_speed` - Vehicle speed
- `sensor.androbd_coolant_tmp` - Coolant temperature
- `sensor.androbd_fuel` - Fuel level
- And more, depending on what your vehicle's OBD system provides

**Viewing the sensors:**
1. Go to **Developer Tools** → **States** in Home Assistant
2. Search for `sensor.androbd_` to see all available sensors
3. Each sensor shows its current value and attributes

**Using in Dashboards:**
You can add these sensors directly to your Home Assistant dashboard using entity cards, gauge cards, or any other card type.

### Optional: Customizing Sensors

If you want to customize the sensors (add units, device class, etc.), add this to your `configuration.yaml`:

```yaml
homeassistant:
  customize:
    sensor.androbd_engine_rpm:
      friendly_name: "Vehicle RPM"
      unit_of_measurement: "rpm"
      icon: mdi:engine
      
    sensor.androbd_speed:
      friendly_name: "Vehicle Speed"
      unit_of_measurement: "km/h"
      icon: mdi:speedometer
      
    sensor.androbd_coolant_tmp:
      friendly_name: "Coolant Temperature"
      unit_of_measurement: "°C"
      device_class: temperature
      icon: mdi:thermometer
```

### Example Automations

**Alert when coolant temperature is high:**
```yaml
automation:
  - alias: "High Coolant Temperature Alert"
    trigger:
      - platform: numeric_state
        entity_id: sensor.androbd_coolant_tmp
        above: 100
    action:
      - service: notify.mobile_app
        data:
          message: "Warning: Vehicle coolant temperature is high!"
```

**Track vehicle data when it changes:**
```yaml
automation:
  - alias: "Log Vehicle Data"
    trigger:
      - platform: state
        entity_id: sensor.androbd_speed
    action:
      - service: logbook.log
        data:
          name: "Vehicle Data"
          message: "Speed: {{ states('sensor.androbd_speed') }} km/h, RPM: {{ states('sensor.androbd_engine_rpm') }}"
```

## Troubleshooting

### Data not appearing in Home Assistant
1. Check that Home Assistant URL is correct and accessible from your device
2. Verify webhook ID or Bearer token is correct
3. Check network connectivity (try accessing the URL in a browser)
4. Look at AndrOBD logs for connection errors
5. Ensure the plugin is enabled in AndrOBD settings

### SSID-based modes not working
1. Ensure the SSID is entered exactly as it appears in WiFi settings
2. Check that WiFi and location permissions are granted to the plugin (required for WiFi scanning on Android 6.0+)
3. Verify you're connected to the correct network (for SSID Connected mode)
4. For SSID in Range mode, ensure WiFi is enabled on the device
5. Check that the home WiFi network has sufficient signal strength to be detected

### High data usage
1. Increase the update interval (e.g., from 5000ms to 10000ms)
2. Use SSID-triggered mode instead of real-time mode
3. Consider using a local network or VPN instead of remote access
4. Select only specific data items to publish instead of all

### No data items available to select
1. Connect AndrOBD to your vehicle first
2. Allow data to be collected for a few minutes
3. The plugin will automatically discover available OBD parameters
4. Return to plugin settings to select items

## Security Considerations

- Always use HTTPS URLs to ensure encrypted communication
- Use strong, unique Bearer tokens
- Consider using a VPN for remote access instead of exposing Home Assistant to the internet
- Regularly rotate access tokens
- Monitor Home Assistant logs for unexpected access

## Example Use Cases

1. **Home Garage Display**: Show vehicle stats on a dashboard when parked at home
2. **Maintenance Tracking**: Log engine hours, average RPM, temperature ranges
3. **Fuel Efficiency Monitoring**: Track MPG over time and create efficiency reports
4. **Alert System**: Get notified of engine problems or maintenance needs
5. **Trip Logging**: Automatically log trips when leaving/arriving home
6. **Battery Health Monitoring**: Track battery voltage trends over time

## Development

### Building from Source

1. Clone the repository with submodules:
   ```bash
   git clone --recursive https://github.com/ian-morgan99/AndrOBD-HomeAssistantPlugin.git
   ```

2. Open in Android Studio or build with Gradle:
   ```bash
   cd AndrOBD-HomeAssistantPlugin
   ./gradlew assembleDebug
   ```

### Project Structure

```
HomeAssistantPlugin/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/fr3ts0n/androbd/plugin/homeassistant/
│   │   ├── HomeAssistantPlugin.java    # Main plugin service
│   │   ├── PluginReceiver.java         # Plugin info receiver
│   │   └── SettingsActivity.java       # Settings UI
│   └── res/
│       ├── values/strings.xml          # String resources
│       └── xml/preferences.xml         # Settings UI definition
├── build.gradle                        # Build configuration
└── README.md                           # This file
```

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## Credits

This plugin was inspired by the [WiCAN firmware](https://github.com/ian-morgan99/wican-fw) Home Assistant integration approach and the [AndrOBD MQTT Publisher](https://github.com/fr3ts0n/AndrOBD-Plugin/tree/master/MqttPublisher) plugin.

## License

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License version 3 or later as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

## Support

- Report issues: [GitHub Issues](https://github.com/ian-morgan99/AndrOBD-HomeAssistantPlugin/issues)
- AndrOBD chat: [Telegram](https://t.me/joinchat/G60ltQv5CCEQ94BZ5yWQbg) or [Matrix](https://matrix.to/#/#AndrOBD:matrix.org)
- Plugin development: [AndrOBD Plugin Framework](https://github.com/fr3ts0n/AndrOBD-Plugin)
