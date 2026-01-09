package com.fr3ts0n.androbd.plugin.homeassistant;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Plugin broadcast receiver for Home Assistant plugin
 * Overrides onReceive to properly start foreground service on Android O+
 */
public class HomeAssistantPluginReceiver extends com.fr3ts0n.androbd.plugin.PluginReceiver {
    private static final String TAG = "HomeAssistantPluginReceiver";
    
    @Override
    public Class<?> getPluginClass() {
        return HomeAssistantPlugin.class;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Broadcast received: " + intent);
        intent.setClass(context, getPluginClass());
        
        // On Android O and above, use startForegroundService for services that will call startForeground()
        // This is required to avoid IllegalStateException on modern Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
