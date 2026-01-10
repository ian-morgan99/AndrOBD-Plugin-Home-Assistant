package com.fr3ts0n.androbd.plugin.homeassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Service starter for Android 12+ (API 31+)
 * This receiver is triggered by AlarmManager to start the foreground service
 * in an exempted context, avoiding ForegroundServiceStartNotAllowedException
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
