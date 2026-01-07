package com.fr3ts0n.androbd.plugin.homeassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fr3ts0n.androbd.plugin.Plugin;
import com.fr3ts0n.androbd.plugin.PluginInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import androidx.annotation.NonNull;

import java.util.Set;

/**
 * Home Assistant plugin for AndrOBD
 */
public class HomeAssistantPlugin extends Plugin 
    implements Plugin.ConfigurationHandler,
               Plugin.ActionHandler,
               Plugin.DataReceiver,
               SharedPreferences.OnSharedPreferenceChangeListener,
               Handler.Callback {

    private static final String TAG = "HomeAssistantPlugin";
    
    // Notification constants
    private static final String NOTIFICATION_CHANNEL_ID = "network_status_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    // Static plugin info - required by PluginReceiver
    static final PluginInfo myInfo = new PluginInfo(
        "Home Assistant",
        HomeAssistantPlugin.class,
        "Send OBD data to Home Assistant",
        "1.0",
        "GPL-3.0",
        "Ian Morgan"
    );
    
    // Preference keys - must be public for SettingsActivity access
    public static final String PREF_HA_URL = "ha_url";
    public static final String PREF_HA_TOKEN = "ha_token";
    public static final String PREF_HA_SSID = "ha_ssid";
    public static final String PREF_HA_OBD_SSID = "ha_obd_ssid";
    public static final String PREF_HA_AUTO_SWITCH = "ha_auto_switch";
    public static final String PREF_HA_UPDATE_INTERVAL = "ha_update_interval";
    public static final String PREF_HA_TRANSMISSION_MODE = "ha_transmission_mode";
    public static final String PREF_HA_ENTITY_PREFIX = "ha_entity_prefix";
    public static final String ITEMS_SELECTED = "items_selected";
    public static final String ITEMS_KNOWN = "items_known";

    private OkHttpClient httpClient;
    private SharedPreferences prefs;
    private Handler handler;
    private HashSet<String> mKnownItems = new HashSet<>();
    private HashSet<String> mSelectedItems = new HashSet<>();
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private NotificationManager notificationManager;

    // Data storage
    private final Map<String, String> dataCache = new HashMap<>();
    private static final int MSG_SEND_UPDATE = 1;
    private static final int MSG_CHECK_WIFI = 2;
    private static final int MSG_SWITCH_TO_HOME = 3;
    private static final int MSG_SWITCH_TO_OBD = 4;
    private long updateInterval = 5000; // Default 5 seconds
    private long wifiCheckInterval = 30000; // Check WiFi every 30 seconds
    private long switchDelay = 5000; // Wait 5 seconds for stable connection after switch
    private long transmissionTimeout = 10000; // Wait 10 seconds for transmission to complete before switching back
    private String transmissionMode = "realtime";
    private String targetSSID = "";
    private String obdSSID = "";
    private boolean autoSwitch = false;
    
    // WiFi state tracking
    private boolean isHomeWifiInRange = false;
    private boolean isConnectedToHomeWifi = false;
    private boolean isOBDWifiInRange = false;
    private boolean isSwitchingNetwork = false;
    private boolean hasPendingTransmission = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Plugin created");

        // Initialize handler first - required by preference loading
        handler = new Handler(Looper.getMainLooper(), this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        
        // Load all preferences
        onSharedPreferenceChanged(prefs, null);

        // Initialize WiFi and connectivity managers
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Initialize HTTP client
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        
        // Create notification channel for Android O+
        createNotificationChannel();
        
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Start WiFi monitoring if needed
        scheduleWifiCheck();
    }

    /**
     * Create notification channel for Android O and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Create or update notification based on current network status
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        }

        // Determine current network state and appropriate icon/text
        int iconRes;
        String notificationText;
        
        if (isConnectedToHomeWifi) {
            iconRes = R.drawable.ic_notification_home;
            notificationText = getString(R.string.notification_text_home);
        } else if (obdSSID != null && !obdSSID.isEmpty() && isConnectedToSSID(obdSSID)) {
            iconRes = R.drawable.ic_notification_car;
            notificationText = getString(R.string.notification_text_car);
        } else if (wifiManager != null && wifiManager.getConnectionInfo() != null && 
                   wifiManager.getConnectionInfo().getSSID() != null &&
                   !wifiManager.getConnectionInfo().getSSID().equals("<unknown ssid>")) {
            iconRes = R.drawable.ic_notification_home; // Default to home icon for unknown WiFi
            notificationText = getString(R.string.notification_text_other);
        } else {
            iconRes = R.drawable.ic_notification_home; // Default icon
            notificationText = getString(R.string.notification_text_disconnected);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(getString(R.string.notification_title))
                .setContentText(notificationText)
                .setSmallIcon(iconRes)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return builder.build();
        } else {
            return builder.getNotification();
        }
    }

    /**
     * Update the notification to reflect current network status
     */
    private void updateNotification() {
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Plugin destroyed");
        
        // Stop foreground service and remove notification
        stopForeground(true);
        
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        
        if (prefs != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    /**
     * Handle plugin requirements
     */
    @Override
    public void performConfigure() {
        Log.d(TAG, "Configure requested");
        Intent cfgIntent = new Intent(this, SettingsActivity.class);
        cfgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(cfgIntent);
    }

    /**
     * Handle data updates
     * Note: When mSelectedItems is empty, all data items are cached (default behavior).
     * This provides a "select all" default when no specific items are filtered.
     */
    @Override
    public void onDataUpdate(String key, String value) {
        if (key == null || value == null) return;

        // Check if this item should be cached (thread-safe)
        boolean shouldCache;
        synchronized (this) {
            shouldCache = mSelectedItems.isEmpty() || mSelectedItems.contains(key);
        }
        
        if (shouldCache) {
            synchronized (dataCache) {
                dataCache.put(key, value);
            }
            
            // Schedule update if not already scheduled
            if (handler != null) {
                if (!handler.hasMessages(MSG_SEND_UPDATE)) {
                    handler.sendEmptyMessageDelayed(MSG_SEND_UPDATE, updateInterval);
                }
            }
        }
    }

    /**
     * Handler callback for scheduled updates
     */
    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == MSG_SEND_UPDATE) {
            sendDataToHomeAssistant();
            return true;
        } else if (msg.what == MSG_CHECK_WIFI) {
            checkWifiState();
            scheduleWifiCheck();
            return true;
        } else if (msg.what == MSG_SWITCH_TO_HOME) {
            performNetworkSwitch(targetSSID, true);
            return true;
        } else if (msg.what == MSG_SWITCH_TO_OBD) {
            performNetworkSwitch(obdSSID, false);
            return true;
        }
        return false;
    }

    /**
     * Schedule periodic WiFi state checking
     */
    private void scheduleWifiCheck() {
        // Schedule WiFi checks to update notification
        if (handler != null) {
            handler.removeMessages(MSG_CHECK_WIFI);
            handler.sendEmptyMessageDelayed(MSG_CHECK_WIFI, wifiCheckInterval);
        }
    }

    /**
     * Check current WiFi state and update flags
     */
    private void checkWifiState() {
        if (targetSSID == null || targetSSID.isEmpty()) {
            // Still check connection state for notification even without targetSSID configured
            if (wifiManager != null && wifiManager.getConnectionInfo() != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String currentSSID = wifiInfo.getSSID();
                if (currentSSID != null) {
                    currentSSID = currentSSID.replace("\"", "");
                    // Check if connected to OBD WiFi
                    if (obdSSID != null && !obdSSID.isEmpty() && currentSSID.equals(obdSSID.replace("\"", ""))) {
                        updateNotification();
                    } else {
                        updateNotification();
                    }
                }
            }
            return;
        }

        // Always check if connected to home WiFi (needed for both modes)
        isConnectedToHomeWifi = isConnectedToSSID(targetSSID);
        
        // Check if target WiFi is in range (only needed for ssid_in_range mode)
        if ("ssid_in_range".equals(transmissionMode)) {
            isHomeWifiInRange = isSSIDInRange(targetSSID);
            
            // Also check OBD WiFi state if auto-switching is enabled
            if (autoSwitch && obdSSID != null && !obdSSID.isEmpty()) {
                isOBDWifiInRange = isSSIDInRange(obdSSID);
            }
            
            Log.d(TAG, "WiFi state - Home in range: " + isHomeWifiInRange + 
                       ", Connected: " + isConnectedToHomeWifi + 
                       ", OBD in range: " + isOBDWifiInRange);
            
            // Update notification to reflect current state
            updateNotification();
            
            // Handle automatic WiFi switching
            if (autoSwitch && !isSwitchingNetwork) {
                handleAutoSwitch();
            }
        } else if ("ssid_connected".equals(transmissionMode)) {
            Log.d(TAG, "WiFi state - Connected: " + isConnectedToHomeWifi);
            
            // Update notification to reflect current state
            updateNotification();
        } else {
            // For realtime or other modes, still update notification
            updateNotification();
        }
    }

    /**
     * Check if currently connected to a specific SSID
     * Note: Uses deprecated NetworkInfo API for compatibility with minSdkVersion 15.
     * For apps targeting API 29+, consider using NetworkCallback instead.
     */
    private boolean isConnectedToSSID(String ssid) {
        if (wifiManager == null || connectivityManager == null) {
            return false;
        }

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected() || networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
            return false;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return false;
        }

        String currentSSID = wifiInfo.getSSID();
        if (currentSSID == null) {
            return false;
        }

        // Remove quotes from SSID if present
        currentSSID = currentSSID.replace("\"", "");
        String targetSSIDClean = ssid.replace("\"", "");

        return currentSSID.equals(targetSSIDClean);
    }

    /**
     * Check if a specific SSID is in range (visible in scan results)
     * Note: WiFi scanning is asynchronous. This method uses the most recent scan results
     * available, which may be slightly stale. Since we check periodically (every 30 seconds),
     * this provides sufficient accuracy for detecting home WiFi proximity.
     */
    private boolean isSSIDInRange(String ssid) {
        if (wifiManager == null) {
            return false;
        }

        try {
            // Start WiFi scan - results will be available after a short delay
            wifiManager.startScan();
            
            // Get most recent scan results (may be from previous scan)
            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (scanResults == null || scanResults.isEmpty()) {
                return false;
            }

            String targetSSIDClean = ssid.replace("\"", "");

            // Check if our target SSID is in the scan results
            for (ScanResult result : scanResults) {
                if (result.SSID != null && result.SSID.equals(targetSSIDClean)) {
                    Log.d(TAG, "Home WiFi found in range: " + result.SSID + " (Signal: " + result.level + ")");
                    return true;
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception during WiFi scan - location permission may be required", e);
        } catch (Exception e) {
            Log.e(TAG, "Error scanning for WiFi networks", e);
        }

        return false;
    }

    /**
     * Handle automatic WiFi switching logic for SSID in Range mode
     */
    private void handleAutoSwitch() {
        if (handler == null) {
            return;
        }
        
        // Check if we have buffered data to transmit
        boolean hasDataToSend = false;
        synchronized (dataCache) {
            hasDataToSend = !dataCache.isEmpty();
        }
        
        // Decision logic for automatic switching
        if (hasDataToSend && isHomeWifiInRange && !isConnectedToHomeWifi) {
            // We have data to send and home WiFi is in range but not connected
            Log.i(TAG, "Auto-switch: Switching to home WiFi to transmit buffered data");
            hasPendingTransmission = true;
            handler.sendEmptyMessage(MSG_SWITCH_TO_HOME);
        } else if (hasPendingTransmission && isConnectedToHomeWifi) {
            // We switched to home WiFi, transmission should happen automatically
            // After successful transmission, switch back to OBD WiFi
            Log.i(TAG, "Auto-switch: Connected to home WiFi, data transmission will occur");
            hasPendingTransmission = false;
            
            // Schedule switch back to OBD WiFi after transmission completes
            // Give time for transmission to complete
            handler.sendEmptyMessageDelayed(MSG_SWITCH_TO_OBD, transmissionTimeout);
        } else if (!isHomeWifiInRange && isOBDWifiInRange && !isConnectedToSSID(obdSSID)) {
            // Home WiFi not in range, OBD WiFi is available but not connected
            Log.i(TAG, "Auto-switch: Switching back to OBD WiFi to continue data collection");
            handler.sendEmptyMessage(MSG_SWITCH_TO_OBD);
        }
    }

    /**
     * Perform network switch to specified SSID
     * Note: Requires CHANGE_WIFI_STATE permission.
     * Note: Uses deprecated WiFi APIs for compatibility with minSdkVersion 15.
     * On Android 10+ (API 29+), these APIs are deprecated and may not work reliably.
     * For production use on modern Android versions, consider using NetworkSpecifier
     * or WifiNetworkSuggestion APIs, but these require higher minSdkVersion.
     */
    private void performNetworkSwitch(String ssid, boolean isHomeNetwork) {
        if (handler == null || wifiManager == null || ssid == null || ssid.isEmpty()) {
            Log.w(TAG, "Cannot switch network - handler, WiFi manager not available or SSID empty");
            return;
        }
        
        isSwitchingNetwork = true;
        String ssidClean = ssid.replace("\"", "");
        
        try {
            Log.i(TAG, "Attempting to switch to network: " + ssidClean);
            
            // Get list of configured networks
            // NOTE: Returns null on Android 10+ due to privacy restrictions
            List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
            if (configuredNetworks == null) {
                Log.e(TAG, "Could not get configured networks - may not work on Android 10+ or need CHANGE_WIFI_STATE permission");
                isSwitchingNetwork = false;
                return;
            }
            
            // Find the network configuration for the target SSID
            int networkId = -1;
            for (WifiConfiguration config : configuredNetworks) {
                String configSSID = config.SSID.replace("\"", "");
                if (configSSID.equals(ssidClean)) {
                    networkId = config.networkId;
                    break;
                }
            }
            
            if (networkId == -1) {
                Log.w(TAG, "Network " + ssidClean + " not found in configured networks. Please connect to it manually first.");
                isSwitchingNetwork = false;
                return;
            }
            
            // Disconnect from current network (deprecated in API 29+)
            wifiManager.disconnect();
            
            // Enable the target network (deprecated in API 29+, requires device/profile owner on 29+)
            boolean enabled = wifiManager.enableNetwork(networkId, true);
            if (!enabled) {
                Log.e(TAG, "Failed to enable network: " + ssidClean);
                isSwitchingNetwork = false;
                return;
            }
            
            // Reconnect to the network (deprecated in API 29+)
            boolean reconnected = wifiManager.reconnect();
            if (!reconnected) {
                Log.e(TAG, "Failed to reconnect to network: " + ssidClean);
                isSwitchingNetwork = false;
                return;
            }
            
            Log.i(TAG, "Successfully initiated switch to: " + ssidClean + ". Waiting for connection to stabilize...");
            
            // Wait for connection to stabilize before marking switch as complete
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isSwitchingNetwork = false;
                    // Update WiFi state to get current connection status
                    checkWifiState();
                    
                    if (isHomeNetwork && isConnectedToHomeWifi) {
                        Log.i(TAG, "Successfully connected to home WiFi, ready for transmission");
                        updateNotification();
                    } else if (!isHomeNetwork && isConnectedToSSID(obdSSID)) {
                        Log.i(TAG, "Successfully switched to OBD WiFi, resuming data collection");
                        updateNotification();
                    } else {
                        Log.w(TAG, "Network switch may not have completed successfully");
                    }
                }
            }, switchDelay);
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception during network switch - CHANGE_WIFI_STATE permission required", e);
            isSwitchingNetwork = false;
        } catch (Exception e) {
            Log.e(TAG, "Error switching network", e);
            isSwitchingNetwork = false;
        }
    }

    /**
     * Check if data should be sent based on current transmission mode and WiFi state
     */
    private boolean shouldSendData() {
        switch (transmissionMode) {
            case "realtime":
                // Always send in real-time mode (assuming internet connectivity)
                return hasInternetConnectivity();
                
            case "ssid_connected":
                // Only send when connected to target WiFi
                if (targetSSID == null || targetSSID.isEmpty()) {
                    Log.w(TAG, "Target SSID not configured for ssid_connected mode");
                    return false;
                }
                // Must be connected to home WiFi AND have internet connectivity
                return isConnectedToHomeWifi && hasInternetConnectivity();
                
            case "ssid_in_range":
                // For ssid_in_range mode: Only send when actually CONNECTED to home WiFi with internet
                // (not just when in range - user must manually switch to home WiFi first)
                if (targetSSID == null || targetSSID.isEmpty()) {
                    Log.w(TAG, "Target SSID not configured for ssid_in_range mode");
                    return false;
                }
                // Must be connected to home WiFi (not just in range) AND have internet connectivity
                boolean canSend = isConnectedToHomeWifi && hasInternetConnectivity();
                if (!canSend && isHomeWifiInRange) {
                    Log.d(TAG, "Home WiFi in range but not connected - switch networks to transmit buffered data");
                }
                return canSend;
                
            default:
                Log.w(TAG, "Unknown transmission mode: " + transmissionMode);
                return hasInternetConnectivity(); // Default to sending if internet available
        }
    }

    /**
     * Check if device has active internet connectivity
     * This verifies that we can actually reach the internet, not just that WiFi is connected
     * Note: Uses deprecated NetworkInfo API for compatibility with minSdkVersion 15.
     * For apps targeting API 29+, consider using NetworkCapabilities with registerDefaultNetworkCallback.
     */
    private boolean hasInternetConnectivity() {
        if (connectivityManager == null) {
            return false;
        }

        try {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && 
                                  activeNetwork.isConnectedOrConnecting() &&
                                  activeNetwork.isAvailable();
            
            if (!isConnected) {
                Log.d(TAG, "No active network connection available");
            }
            
            return isConnected;
        } catch (Exception e) {
            Log.e(TAG, "Error checking internet connectivity", e);
            return false;
        }
    }

    /**
     * Send accumulated data to Home Assistant
     */
    private void sendDataToHomeAssistant() {
        // Check if we should send based on transmission mode and WiFi state
        if (!shouldSendData()) {
            Log.d(TAG, "Not sending data - transmission mode conditions not met (mode: " + transmissionMode + ")");
            return;
        }

        String url = prefs.getString(PREF_HA_URL, "");
        String token = prefs.getString(PREF_HA_TOKEN, "");
        String entityPrefix = prefs.getString(PREF_HA_ENTITY_PREFIX, "sensor.androbd_");

        if (url.isEmpty() || token.isEmpty()) {
            Log.w(TAG, "Home Assistant URL or token not configured");
            return;
        }

        Map<String, String> dataToSend;
        synchronized (dataCache) {
            dataToSend = new HashMap<>(dataCache);
            dataCache.clear();
        }

        if (dataToSend.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : dataToSend.entrySet()) {
            sendSensorUpdate(url, token, entityPrefix, entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void onDataListUpdate(String csvString) {
        if (csvString == null || csvString.isEmpty()) return;
        
        // Parse CSV format: "key;description;value;units\nkey;description;value;units\n..."
        synchronized (this) {
            for (String csvLine : csvString.split("\n")) {
                String[] fields = csvLine.split(";");
                if (fields.length > 0) {
                    String key = fields[0].trim();
                    if (!key.isEmpty()) {
                        mKnownItems.add(key);
                    }
                }
            }
            // Persist known items
            prefs.edit().putStringSet(ITEMS_KNOWN, mKnownItems).apply();
        }
        
        // Clear data cache for fresh update cycle
        synchronized (dataCache) {
            dataCache.clear();
        }
    }

    /**
     * Send individual sensor update to Home Assistant
     */
    private void sendSensorUpdate(String baseUrl, String token, String entityPrefix, String key, String value) {
        // Clean up the key to make it a valid entity ID
        String entityId = entityPrefix + key.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_");

        String url = baseUrl + "/api/states/" + entityId;

        try {
            JSONObject json = new JSONObject();
            json.put("state", value);

            JSONObject attributes = new JSONObject();
            attributes.put("friendly_name", key);
            attributes.put("source", "AndrOBD");
            json.put("attributes", attributes);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Network error sending update for " + key + ": " + e.getMessage(), e);
                    // Data will remain in cache and retry on next update cycle
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Successfully updated " + entityId);
                        } else {
                            Log.e(TAG, "HTTP error updating " + entityId + ": " + response.code() + " " + response.message());
                            // Log response body for debugging if available
                            if (response.body() != null) {
                                try {
                                    String responseBody = response.body().string();
                                    if (responseBody != null && !responseBody.isEmpty()) {
                                        Log.e(TAG, "Response body: " + responseBody);
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not read error response body", e);
                                }
                            }
                        }
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for " + key, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error sending update for " + key, e);
        }
    }

    @Override
    public void performAction() {
        Log.d(TAG, "Action requested - triggering manual update");
        sendDataToHomeAssistant();
    }

    /**
     * Get plugin information
     */
    @Override
    public PluginInfo getPluginInfo() {
        return myInfo;
    }
    
    /**
     * Handle preference changes
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) {
            // Load all preferences
            loadPreferences(sharedPreferences);
            return;
        }
        
        // Handle specific preference changes
        switch (key) {
            case PREF_HA_UPDATE_INTERVAL:
                String intervalStr = sharedPreferences.getString(key, "5");
                try {
                    updateInterval = Long.parseLong(intervalStr) * 1000;
                } catch (NumberFormatException e) {
                    updateInterval = 5000;
                }
                break;
                
            case PREF_HA_TRANSMISSION_MODE:
                transmissionMode = sharedPreferences.getString(key, "realtime");
                Log.d(TAG, "Transmission mode changed to: " + transmissionMode);
                // Reschedule WiFi checking based on new mode
                if (handler != null) {
                    handler.removeMessages(MSG_CHECK_WIFI);
                }
                scheduleWifiCheck();
                // Check WiFi state immediately
                checkWifiState();
                break;
                
            case PREF_HA_SSID:
                targetSSID = sharedPreferences.getString(key, "");
                Log.d(TAG, "Home SSID changed to: " + targetSSID);
                // Check WiFi state immediately when SSID changes
                checkWifiState();
                break;
                
            case PREF_HA_OBD_SSID:
                obdSSID = sharedPreferences.getString(key, "");
                Log.d(TAG, "OBD SSID changed to: " + obdSSID);
                // Check WiFi state immediately when SSID changes
                checkWifiState();
                break;
                
            case PREF_HA_AUTO_SWITCH:
                autoSwitch = sharedPreferences.getBoolean(key, false);
                Log.d(TAG, "Auto-switch changed to: " + autoSwitch);
                if (autoSwitch) {
                    if (targetSSID.isEmpty() || obdSSID.isEmpty()) {
                        Log.w(TAG, "Auto-switch enabled but SSIDs not fully configured");
                    }
                }
                break;
                
            case ITEMS_SELECTED:
                Set<String> selectedSet = sharedPreferences.getStringSet(key, new HashSet<>());
                synchronized (this) {
                    mSelectedItems = new HashSet<>(selectedSet);
                }
                break;
                
            case ITEMS_KNOWN:
                Set<String> knownSet = sharedPreferences.getStringSet(key, new HashSet<>());
                synchronized (this) {
                    mKnownItems = new HashSet<>(knownSet);
                }
                break;
        }
    }
    
    /**
     * Load all preferences on initialization
     * Note: Only loads preferences that require special processing on startup.
     * Other preferences (PREF_HA_URL, PREF_HA_TOKEN, etc.) are 
     * accessed directly when needed and don't require initialization.
     */
    private void loadPreferences(SharedPreferences prefs) {
        onSharedPreferenceChanged(prefs, PREF_HA_UPDATE_INTERVAL);
        onSharedPreferenceChanged(prefs, PREF_HA_TRANSMISSION_MODE);
        onSharedPreferenceChanged(prefs, PREF_HA_SSID);
        onSharedPreferenceChanged(prefs, PREF_HA_OBD_SSID);
        onSharedPreferenceChanged(prefs, PREF_HA_AUTO_SWITCH);
        onSharedPreferenceChanged(prefs, ITEMS_SELECTED);
        onSharedPreferenceChanged(prefs, ITEMS_KNOWN);
    }
}
