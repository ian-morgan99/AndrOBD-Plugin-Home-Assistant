package com.fr3ts0n.androbd.plugin.homeassistant;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings activity for Home Assistant plugin configuration
 */
public class SettingsActivity extends Activity {
    
    private static final int REQUEST_PERMISSIONS = 100;
    private static final String PREF_PERMISSION_DIALOG_SHOWN = "permission_dialog_shown";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Display the fragment as the main content
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        
        // Check and request permissions on Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }
    }
    
    /**
     * Check if required permissions are granted and request if needed
     */
    private void checkAndRequestPermissions() {
        // Check if we've already shown the dialog
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dialogShown = prefs.getBoolean(PREF_PERMISSION_DIALOG_SHOWN, false);
        
        List<String> permissionsNeeded = new ArrayList<>();
        
        // Location permission (required for WiFi scanning on Android 6.0+)
        // Note: ACCESS_FINE_LOCATION automatically grants ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        // Notification permission (required on Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        // Only show dialog if permissions are needed and we haven't shown it before
        // OR if user denied but should show rationale (meaning they can still grant)
        if (!permissionsNeeded.isEmpty()) {
            boolean shouldShowRationale = false;
            for (String permission : permissionsNeeded) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    shouldShowRationale = true;
                    break;
                }
            }
            
            // Show dialog if: not shown before OR should show rationale
            if (!dialogShown || shouldShowRationale) {
                showPermissionExplanationDialog(permissionsNeeded);
                // Mark dialog as shown
                prefs.edit().putBoolean(PREF_PERMISSION_DIALOG_SHOWN, true).apply();
            }
        }
    }
    
    /**
     * Show dialog explaining why permissions are needed
     */
    private void showPermissionExplanationDialog(final List<String> permissions) {
        StringBuilder message = new StringBuilder();
        message.append("This app requires the following permissions:\n\n");
        
        boolean hasLocation = false;
        boolean hasNotification = false;
        
        for (String permission : permissions) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission) ||
                Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission)) {
                hasLocation = true;
            } else if (Manifest.permission.POST_NOTIFICATIONS.equals(permission)) {
                hasNotification = true;
            }
        }
        
        if (hasLocation) {
            message.append("Location:\n");
            message.append("- Scan for WiFi networks (required by Android)\n");
            message.append("- Detect when home network is in range\n");
            message.append("- Enable automatic WiFi switching\n");
            message.append("- Optional: Send vehicle location to Home Assistant\n\n");
            message.append("Note: Location is not tracked unless you explicitly enable the location tracking option.\n\n");
        }
        
        if (hasNotification) {
            message.append("Notifications:\n");
            message.append("- Display network status indicator\n");
            message.append("- Show connection/transmission status\n\n");
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(message.toString())
            .setPositiveButton("Grant Permission", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(SettingsActivity.this,
                        permissions.toArray(new String[0]),
                        REQUEST_PERMISSIONS);
                }
            })
            .setNegativeButton("Not Now", null)
            .setNeutralButton("More Info", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Open app settings where user can see and grant permissions
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            })
            .show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            List<String> deniedPermissions = new ArrayList<>();
            
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    deniedPermissions.add(permissions[i]);
                }
            }
            
            if (allGranted) {
                // All permissions granted
                new AlertDialog.Builder(this)
                    .setTitle("Permissions Granted")
                    .setMessage("All required permissions have been granted. The app can now:\n\n" +
                        "- Scan for WiFi networks\n" +
                        "- Detect home network proximity\n" +
                        "- Enable automatic WiFi switching\n" +
                        "- Display status notifications\n" +
                        "- Optional: Track vehicle location (when enabled)\n\n" +
                        "You can revoke these permissions anytime in Android Settings.")
                    .setPositiveButton("OK", null)
                    .show();
            } else {
                // Some permissions denied
                showPermissionDeniedDialog(deniedPermissions);
            }
        }
    }
    
    /**
     * Show dialog when permissions are denied
     */
    private void showPermissionDeniedDialog(List<String> deniedPermissions) {
        StringBuilder message = new StringBuilder();
        message.append("Some permissions were not granted. This will limit functionality:\n\n");
        
        for (String permission : deniedPermissions) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission) ||
                Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission)) {
                message.append("Location Denied:\n");
                message.append("- WiFi network scanning disabled\n");
                message.append("- Cannot detect when home network is in range\n");
                message.append("- Automatic WiFi switching unavailable\n");
                message.append("- Location sensor feature unavailable\n\n");
            } else if (Manifest.permission.POST_NOTIFICATIONS.equals(permission)) {
                message.append("Notifications Denied:\n");
                message.append("- Status notifications may not appear\n");
                message.append("- Network indicator may not display\n\n");
            }
        }
        
        message.append("You can grant permissions later:\nAndroid Settings -> Apps -> AndrOBD Home Assistant -> Permissions");
        
        new AlertDialog.Builder(this)
            .setTitle("Limited Functionality")
            .setMessage(message.toString())
            .setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            })
            .setNegativeButton("OK", null)
            .show();
    }
    
    /**
     * Settings fragment
     */
    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        
        private SharedPreferences prefs;
        private MultiSelectListPreference dataItemsPref;
        private Preference showLogsPref;
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            // Load preferences from XML
            addPreferencesFromResource(R.xml.preferences);
            
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            
            // Setup data items preference
            dataItemsPref = (MultiSelectListPreference) findPreference(HomeAssistantPlugin.ITEMS_SELECTED);
            updateDataItemsList();
            
            // Setup show logs preference click handler
            showLogsPref = findPreference("ha_show_logs");
            if (showLogsPref != null) {
                showLogsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        android.content.Intent intent = new android.content.Intent(getActivity(), LogViewerActivity.class);
                        startActivity(intent);
                        return true;
                    }
                });
            }
            
            // Set summary updaters for all preferences
            updateAllSummaries();
        }
        
        @Override
        public void onResume() {
            super.onResume();
            prefs.registerOnSharedPreferenceChangeListener(this);
        }
        
        @Override
        public void onPause() {
            super.onPause();
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (HomeAssistantPlugin.ITEMS_KNOWN.equals(key)) {
                // Update data items list when new items are discovered
                updateDataItemsList();
            }
            
            // Update summary for changed preference
            Preference pref = findPreference(key);
            if (pref != null) {
                updateSummary(pref);
            }
        }
        
        /**
         * Update the list of available data items
         */
        private void updateDataItemsList() {
            if (dataItemsPref == null) {
                return;
            }
            
            // Get known items from preferences (defensively copy to avoid modifications)
            Set<String> knownItemsSet = prefs.getStringSet(HomeAssistantPlugin.ITEMS_KNOWN, new HashSet<String>());
            Set<String> knownItems = new HashSet<>(knownItemsSet);
            
            if (knownItems.isEmpty()) {
                // No items discovered yet
                dataItemsPref.setEnabled(false);
                dataItemsPref.setSummary("No OBD data items discovered yet. Connect to vehicle to discover items.");
                return;
            }
            
            // Sort items alphabetically
            ArrayList<String> sortedItems = new ArrayList<>(knownItems);
            Collections.sort(sortedItems);
            
            // Convert to arrays
            CharSequence[] entries = sortedItems.toArray(new CharSequence[0]);
            CharSequence[] entryValues = sortedItems.toArray(new CharSequence[0]);
            
            // Update preference
            dataItemsPref.setEntries(entries);
            dataItemsPref.setEntryValues(entryValues);
            dataItemsPref.setEnabled(true);
            
            // Update summary
            updateSummary(dataItemsPref);
        }
        
        /**
         * Update summaries for all preferences
         */
        private void updateAllSummaries() {
            updateSummary(findPreference(HomeAssistantPlugin.PREF_HA_URL));
            updateSummary(findPreference(HomeAssistantPlugin.PREF_HA_TOKEN));
            updateSummary(findPreference(HomeAssistantPlugin.PREF_HA_TRANSMISSION_MODE));
            updateSummary(findPreference(HomeAssistantPlugin.PREF_HA_SSID));
            updateSummary(findPreference(HomeAssistantPlugin.PREF_HA_OBD_SSID));
            updateSummary(findPreference(HomeAssistantPlugin.PREF_HA_UPDATE_INTERVAL));
            updateSummary(dataItemsPref);
        }
        
        /**
         * Update summary for a preference to show current value
         */
        private void updateSummary(Preference pref) {
            if (pref == null) {
                return;
            }
            
            if (pref instanceof EditTextPreference) {
                EditTextPreference editPref = (EditTextPreference) pref;
                String value = editPref.getText();
                
                // Don't show token value for security
                if (HomeAssistantPlugin.PREF_HA_TOKEN.equals(pref.getKey())) {
                    if (value != null && !value.isEmpty()) {
                        pref.setSummary("••••••••");
                    } else {
                        pref.setSummary(getString(R.string.ha_token_description));
                    }
                } else {
                    if (value != null && !value.isEmpty()) {
                        pref.setSummary(value);
                    }
                }
            } else if (pref instanceof ListPreference) {
                ListPreference listPref = (ListPreference) pref;
                pref.setSummary(listPref.getEntry());
            } else if (pref instanceof MultiSelectListPreference) {
                MultiSelectListPreference multiPref = (MultiSelectListPreference) pref;
                Set<String> values = multiPref.getValues();
                
                if (values == null || values.isEmpty()) {
                    pref.setSummary("All items (none selected = publish all)");
                } else {
                    pref.setSummary(values.size() + " items selected");
                }
            }
        }
    }
}
