package com.fr3ts0n.androbd.plugin.homeassistant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    // Data storage
    private final Map<String, String> dataCache = new HashMap<>();
    private static final int MSG_SEND_UPDATE = 1;
    private long updateInterval = 5000; // Default 5 seconds

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Plugin created");

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        
        // Load all preferences
        onSharedPreferenceChanged(prefs, null);

        // Initialize HTTP client
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // Initialize handler
        handler = new Handler(Looper.getMainLooper(), this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Plugin destroyed");
        
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
     */
    @Override
    public void onDataUpdate(String key, String value) {
        if (key == null || value == null) return;

        synchronized (dataCache) {
            // Only add if selected for publishing or no filter set
            if (mSelectedItems.isEmpty() || mSelectedItems.contains(key)) {
                dataCache.put(key, value);
            }
        }

        // Schedule update if not already scheduled
        if (!handler.hasMessages(MSG_SEND_UPDATE)) {
            handler.sendEmptyMessageDelayed(MSG_SEND_UPDATE, updateInterval);
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
        }
        return false;
    }

    /**
     * Send accumulated data to Home Assistant
     */
    private void sendDataToHomeAssistant() {
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
        synchronized (mKnownItems) {
            for (String csvLine : csvString.split("\n")) {
                String[] fields = csvLine.split(";");
                if (fields.length > 0) {
                    mKnownItems.add(fields[0].trim());
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
                    Log.e(TAG, "Failed to send update for " + key, e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Successfully updated " + entityId);
                    } else {
                        Log.e(TAG, "Failed to update " + entityId + ": " + response.code());
                    }
                    response.close();
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for " + key, e);
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
                
            case ITEMS_SELECTED:
                Set<String> selectedSet = sharedPreferences.getStringSet(key, new HashSet<>());
                mSelectedItems = new HashSet<>(selectedSet);
                break;
                
            case ITEMS_KNOWN:
                Set<String> knownSet = sharedPreferences.getStringSet(key, new HashSet<>());
                mKnownItems = new HashSet<>(knownSet);
                break;
        }
    }
    
    /**
     * Load all preferences on initialization
     */
    private void loadPreferences(SharedPreferences prefs) {
        onSharedPreferenceChanged(prefs, PREF_HA_UPDATE_INTERVAL);
        onSharedPreferenceChanged(prefs, ITEMS_SELECTED);
        onSharedPreferenceChanged(prefs, ITEMS_KNOWN);
    }
}
