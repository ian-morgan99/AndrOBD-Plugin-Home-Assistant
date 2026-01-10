package com.fr3ts0n.androbd.plugin.homeassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Service starter for Android 12+ (API 31+)
 * This receiver is triggered by AlarmManager to start the foreground service
 * in an exempted context, avoiding ForegroundServiceStartNotAllowedException
 * 
 * Note: This receiver is only used on Android 12+ (API 31), so startForegroundService
 * is always available (it was added in API 26)
 */
public class HomeAssistantServiceStarter extends BroadcastReceiver {
    private static final String TAG = "HomeAssistantServiceStarter";
    
    // Plugin service class to start
    private static final Class<?> PLUGIN_SERVICE_CLASS = HomeAssistantPlugin.class;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // Defensive null check for intent (should never be null per Android contract)
        if (intent == null) {
            Log.e(TAG, "Received null intent, cannot start service");
            return;
        }
        
        Log.d(TAG, "Alarm triggered, starting service: " + intent.getAction());
        
        // Create intent for the plugin service
        Intent serviceIntent = new Intent(context, PLUGIN_SERVICE_CLASS);
        serviceIntent.setAction(intent.getAction());
        if (intent.getExtras() != null) {
            serviceIntent.putExtras(intent.getExtras());
        }
        
        // Start foreground service with error handling
        // This is allowed here because we're in an alarm context (exempted scenario)
        // This receiver is only used on Android 12+ where startForegroundService is available
        try {
            context.startForegroundService(serviceIntent);
        } catch (IllegalStateException e) {
            // IllegalStateException includes ForegroundServiceStartNotAllowedException on Android 12+
            Log.e(TAG, "Failed to start foreground service for action: " + intent.getAction() +
                    " (may be due to Android 12+ restrictions)", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to start foreground service due to security restrictions: " + intent.getAction(), e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting foreground service for action: " + intent.getAction(), e);
        }
    }
}
