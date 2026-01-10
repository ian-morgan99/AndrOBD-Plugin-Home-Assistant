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
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm triggered, starting service: " + intent.getAction());
        
        // Create intent for the plugin service
        Intent serviceIntent = new Intent(context, HomeAssistantPlugin.class);
        serviceIntent.setAction(intent.getAction());
        if (intent.getExtras() != null) {
            serviceIntent.putExtras(intent.getExtras());
        }
        
        // Start foreground service
        // This is allowed here because we're in an alarm context (exempted scenario)
        // This receiver is only used on Android 12+ where startForegroundService is available
        context.startForegroundService(serviceIntent);
    }
}
