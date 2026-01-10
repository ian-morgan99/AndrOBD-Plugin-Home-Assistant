# New Features Implementation Summary

## Overview
This document describes the new features implemented to address connectivity issues, debugging capabilities, and data reliability improvements for the AndrOBD Home Assistant Plugin.

## Features Implemented

### 1. Mobile Data Transmission Support

**Problem Addressed:** Mobile devices often disconnect from OBD WiFi when mobile data is also available, causing connectivity issues.

**Solution Implemented:**
- Added "Send Data Via Mobile" preference in settings
- When enabled and mobile data is available, the plugin routes Home Assistant traffic through the mobile data connection
- OBD WiFi connection remains stable for vehicle data collection
- Uses Android's Network API (Android 5.0+) to bind HTTP requests to the mobile network
- Falls back gracefully to default routing on older Android versions

**How It Works:**
1. Plugin detects available networks (WiFi and mobile)
2. When "Send Data Via Mobile" is enabled:
   - OBD data collection happens over WiFi (connected to OBD adapter)
   - Home Assistant data transmission uses mobile data connection
3. Uses `NetworkBoundSocketFactory` to route specific HTTP requests through mobile network
4. Transparent to the user - no manual network switching required

**Benefits:**
- Simultaneous WiFi (for OBD) and mobile data (for HA) connections
- No more WiFi disconnections due to mobile data interference
- Maintains reliable OBD connection while ensuring data reaches Home Assistant

### 2. Comprehensive Logging System

**Problem Addressed:** No visibility into what the app is doing, making debugging impossible.

**Solution Implemented:**
- Added "Enable Logging" preference to control logging
- Added "Show Logs" button to view, copy, and clear logs
- Created `LogManager` class with privacy-focused obfuscation
- Extensive logging throughout all operations
- Logs stored in persistent file with automatic size management

**Logging Features:**
- **Privacy-focused obfuscation:** Automatically redacts:
  - Bearer tokens (replaced with `[REDACTED_TOKEN]`)
  - URLs (replaced with `[REDACTED_HOST]` keeping only protocol)
  - SSIDs (replaced with `[REDACTED_SSID]`)
  - IP addresses (replaced with `[REDACTED_IP]`)
- **Log levels:** DEBUG, INFO, WARN, ERROR
- **Timestamps:** All log entries include precise timestamps
- **Size management:** Logs automatically truncated when exceeding 500KB
- **Log viewer:** Built-in UI to view, copy, or clear logs
- **Copy to clipboard:** Easy sharing of logs for issue reporting

**What Gets Logged:**
- Plugin lifecycle events (onCreate, onDestroy)
- Data reception and storage
- Network state changes
- WiFi scanning and switching
- HTTP requests and responses
- Database operations
- Error conditions with stack traces

**Using the Logs:**
1. Enable "Enable Logging" in settings
2. Use the app normally
3. Click "Show Logs" to view activity
4. Click "Copy to Clipboard" to share logs
5. Paste into GitHub issues for debugging

### 3. Local Database with Sent Status Tracking

**Problem Addressed:** Network drops and partial transmissions cause data loss.

**Solution Implemented:**
- SQLite database for persistent local storage
- Each data record includes:
  - Unique ID
  - Key (OBD parameter name)
  - Value (reading)
  - Timestamp (milliseconds since epoch)
  - Sent status (boolean flag)
- Records marked as "sent" only after successful transmission
- Automatic retry of unsent records
- Periodic cleanup of old sent records (older than 24 hours)

**Database Schema:**
```sql
CREATE TABLE data_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    sent INTEGER DEFAULT 0
);
```

**How It Works:**
1. OBD data received → stored in database with timestamp
2. Transmission scheduled based on mode (realtime/SSID)
3. Unsent records retrieved for transmission
4. Each successful transmission marks record as "sent"
5. Failed transmissions leave records unsent for retry
6. Old sent records cleaned up periodically

**Benefits:**
- No data loss on network failures
- Automatic retry of failed transmissions
- Complete audit trail with timestamps
- Home Assistant receives timestamp with each data point
- Handles intermittent connectivity gracefully

### 4. Enhanced Data Format

**Previous Format:**
```json
{
  "state": "2500",
  "attributes": {
    "friendly_name": "ENGINE_RPM",
    "source": "AndrOBD"
  }
}
```

**New Format:**
```json
{
  "state": "2500",
  "attributes": {
    "friendly_name": "ENGINE_RPM",
    "source": "AndrOBD",
    "timestamp": 1704912345678
  }
}
```

**Benefits:**
- Home Assistant knows exact capture time
- Can create accurate historical logs
- Handles delayed transmissions correctly
- Supports time-series analysis

## Usage Instructions

### Configuring Mobile Data Transmission

1. Open AndrOBD Home Assistant plugin settings
2. Enable "Send Data Via Mobile"
3. Configure both "Home WiFi SSID" and "OBD WiFi SSID"
4. Select transmission mode (Real-time recommended with mobile data)

**Note:** Requires Android 5.0 or higher for network binding. On older versions, setting has no effect.

### Enabling Logging

1. Open AndrOBD Home Assistant plugin settings
2. Check "Enable Logging"
3. Use the app normally
4. Click "Show Logs" to view activity

**Viewing Logs:**
- Logs show in reverse chronological order (newest at bottom)
- Sensitive data automatically obfuscated
- Use "Refresh" button to update view
- Use "Copy to Clipboard" to share
- Use "Clear Logs" to remove all logs

### Troubleshooting with Logs

When reporting issues:
1. Enable logging before reproducing the issue
2. Reproduce the issue
3. Open "Show Logs"
4. Click "Copy to Clipboard"
5. Paste logs into GitHub issue
6. Logs are already obfuscated - safe to share

## Technical Details

### Network Binding Implementation

Uses Android's Network API (API 21+) to bind sockets:

```java
// Detect mobile network
Network mobileNetwork = getMobileNetwork();

// Create socket factory bound to mobile network
SocketFactory factory = mobileNetwork.getSocketFactory();

// Configure OkHttp client
OkHttpClient client = httpClient.newBuilder()
    .socketFactory(new NetworkBoundSocketFactory(mobileNetwork))
    .build();
```

**Compatibility:**
- Android 5.0+ (API 21+): Full support with network binding
- Android 4.x and earlier: Setting has no effect, uses default routing

### Database Architecture

**Tables:**
- `data_records`: Main storage for OBD data

**Indexes:**
- `idx_timestamp`: For efficient time-based queries
- `idx_sent`: For efficient unsent record queries

**Queries:**
- Get unsent records: `SELECT * FROM data_records WHERE sent = 0 ORDER BY timestamp ASC`
- Cleanup old data: `DELETE FROM data_records WHERE sent = 1 AND timestamp < ?`

**Maintenance:**
- Automatic cleanup every hour
- Keeps sent records for 24 hours
- Unsent records kept indefinitely until transmitted

### Logging Architecture

**Components:**
- `LogManager`: Central logging service
- File-based storage: `/data/data/com.fr3ts0n.androbd.plugin.homeassistant/files/androbd_ha_plugin.log`
- `LogViewerActivity`: UI for viewing logs

**Obfuscation Rules:**
- Tokens: `bearer <token>` → `bearer [REDACTED_TOKEN]`
- URLs: `https://example.com:8123` → `https://[REDACTED_HOST]`
- SSIDs: `ssid: "MyNetwork"` → `ssid: "[REDACTED_SSID]"`
- IPs: `192.168.1.1` → `[REDACTED_IP]`

## Performance Considerations

### Battery Impact
- Logging: Minimal (only when enabled)
- Database: Low (efficient indexes, infrequent writes)
- Network binding: Negligible (reuses existing connections)

### Storage Impact
- Log file: Max 500KB (auto-truncated)
- Database: Grows with unsent data, cleaned up periodically
- Estimate: ~1KB per record, 24h max retention for sent records

### Network Impact
- Mobile data: Only for HA transmission (typically <1KB per record)
- No change to OBD WiFi usage
- Failed transmissions don't consume data

## Testing Recommendations

### Test Scenarios

1. **Mobile Data Routing:**
   - Connect to OBD WiFi
   - Enable mobile data
   - Enable "Send Data Via Mobile"
   - Verify data reaches Home Assistant
   - Check logs confirm mobile network usage

2. **Logging System:**
   - Enable logging
   - Perform various operations
   - Verify logs show activity
   - Verify sensitive data obfuscated
   - Test copy to clipboard

3. **Database Reliability:**
   - Disable network
   - Generate OBD data
   - Verify database stores records
   - Re-enable network
   - Verify records transmitted and marked sent

4. **Timestamp Accuracy:**
   - Send data to Home Assistant
   - Verify timestamp attribute in entity
   - Confirm timestamp matches capture time

## Known Limitations

1. **Network Binding:**
   - Requires Android 5.0+ (API 21+)
   - May not work on heavily customized Android builds
   - Some carriers may block simultaneous WiFi + mobile data

2. **WiFi Switching:**
   - Auto-switching uses deprecated APIs
   - May not work on Android 10+ due to privacy restrictions
   - Manual switching recommended for Android 10+

3. **Database:**
   - No encryption at rest (data stored in plain text)
   - No synchronization with external systems
   - Manual export not supported (use logs for debugging)

4. **Logging:**
   - Log file not backed up (excluded from Android backup)
   - No log rotation (only truncation)
   - No remote logging support

## Future Enhancements

Potential improvements for consideration:

1. **Database:**
   - Export database to CSV
   - Configurable retention period
   - Compression for old records

2. **Logging:**
   - Multiple log levels (verbose/debug/info)
   - Log rotation with multiple files
   - Remote logging to syslog/HTTP

3. **Network:**
   - VPN support
   - Proxy support
   - Network quality indicators

4. **UI:**
   - Dashboard showing statistics
   - Real-time data view
   - Network status indicators

## Migration Notes

### Existing Users

No breaking changes for existing users:
- All existing settings preserved
- New features disabled by default
- Database created on first launch
- Logging disabled by default

### Upgrading

1. Install new version
2. Existing settings remain intact
3. Database automatically created
4. Enable new features in settings as desired

## Support

For issues or questions:
1. Enable logging
2. Reproduce the issue
3. Copy logs (already obfuscated)
4. Open GitHub issue with:
   - Description of problem
   - Steps to reproduce
   - Logs (paste from clipboard)
   - Android version
   - Device model

## Conclusion

These enhancements significantly improve the reliability, debuggability, and functionality of the AndrOBD Home Assistant plugin. The combination of mobile data routing, comprehensive logging, and database-backed storage ensures that:

1. Connectivity is more reliable (mobile data routing)
2. Issues are easier to diagnose (comprehensive logging)
3. No data is lost (database with retry logic)
4. Home Assistant receives accurate timestamps (enhanced data format)

Users can now confidently use the plugin knowing that data will be reliably captured and transmitted, and any issues can be quickly diagnosed using the built-in logging system.
