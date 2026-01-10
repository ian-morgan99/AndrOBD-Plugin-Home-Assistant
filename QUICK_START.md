# Quick Start Guide - New Features

## Mobile Data Transmission

**Problem:** WiFi disconnects when mobile data is enabled.

**Solution:**
1. Open plugin settings
2. Enable "Send Data Via Mobile"
3. Connect to OBD WiFi for vehicle data
4. Plugin automatically uses mobile data for Home Assistant

**Works on:** Android 5.0+ (API 21+)

## Logging for Debugging

**Problem:** Can't see what the app is doing.

**Solution:**
1. Open plugin settings
2. Enable "Enable Logging"
3. Click "Show Logs" to view activity
4. Click "Copy to Clipboard" to share

**Privacy:** Logs automatically hide tokens, URLs, SSIDs, and IPs.

## Viewing Logs

In plugin settings, click "Show Logs" to see:
- Data received from OBD
- Network status changes  
- Transmission attempts
- Errors and warnings

**Buttons:**
- **Refresh**: Update log display
- **Copy to Clipboard**: Copy obfuscated logs
- **Clear Logs**: Delete all logs

## Reporting Issues

When reporting a problem:
1. Enable "Enable Logging"
2. Reproduce the issue
3. Click "Show Logs"
4. Click "Copy to Clipboard"
5. Paste in GitHub issue

Logs are automatically obfuscated - safe to share publicly.

## Database Storage

**Automatic:** All OBD data is now stored locally with timestamps.

**Benefits:**
- No data loss on network failures
- Automatic retry of failed transmissions
- Home Assistant receives accurate timestamps

**Maintenance:**
- Sent records kept for 24 hours
- Unsent records kept until successfully transmitted
- Automatic cleanup runs hourly

## Troubleshooting

### Mobile data not working?
- Check Android version (requires 5.0+)
- Verify mobile data is enabled
- Check carrier allows simultaneous WiFi + mobile
- Review logs for errors

### Logs not showing activity?
- Verify "Enable Logging" is checked
- Perform an action (like manual data send)
- Click "Refresh" in log viewer
- Check if log file has write permissions

### Data not reaching Home Assistant?
- Enable logging
- Check logs for transmission errors
- Verify Home Assistant URL and token
- Check network connectivity
- Look for error messages in logs

## Settings Reference

**New Settings:**

- **Send Data Via Mobile**: Route HA traffic via mobile data (Android 5.0+)
- **Enable Logging**: Turn on detailed activity logging
- **Show Logs**: View, copy, or clear application logs

**Recommended Configuration:**

For WiFi OBD adapters:
- Transmission Mode: "SSID in Range"
- Send Data Via Mobile: Enabled (if Android 5.0+)
- Enable Logging: Enabled (for troubleshooting)

For Bluetooth OBD adapters:
- Transmission Mode: "Real-time"
- Send Data Via Mobile: Optional
- Enable Logging: Enabled (for troubleshooting)

## Tips

1. **Always enable logging** - helps diagnose issues quickly
2. **Use mobile data feature** if you have both WiFi and mobile available
3. **Check logs regularly** to ensure everything is working
4. **Copy logs before clearing** if you need them later
5. **Include logs when reporting issues** - makes debugging much easier
