package com.fr3ts0n.androbd.plugin.homeassistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Plugin broadcast receiver for Home Assistant plugin
 * Handles service start with Android 12+ background service restrictions
 */
public class HomeAssistantPluginReceiver extends com.fr3ts0n.androbd.plugin.PluginReceiver {
    private static final String TAG = "HomeAssistantPluginReceiver";
    
    // Minimal delay for AlarmManager scheduling (1ms for immediate execution)
    private static final long ALARM_DELAY_MS = 1;
    
    @Override
    public Class<?> getPluginClass() {
        return HomeAssistantPlugin.class;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Broadcast received: " + intent);
        
        // Android 12+ (API 31+) has stricter restrictions on starting foreground services
        // from background contexts (BroadcastReceivers). We use AlarmManager as a workaround
        // because exact alarms are an exempted scenario that allows foreground service starts.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+): Use AlarmManager to defer service start
            // This makes the service start from an exempted context
            scheduleServiceStart(context, intent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8-11: Use startForegroundService directly
            intent.setClass(context, getPluginClass());
            context.startForegroundService(intent);
        } else {
            // Android 7.1 and below: Use regular startService
            intent.setClass(context, getPluginClass());
            context.startService(intent);
        }
    }
    
    /**
     * Schedule service start via AlarmManager (Android 12+ workaround)
     * This allows foreground service to start from an exempted context
     */
    private void scheduleServiceStart(Context context, Intent originalIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available - cannot start service on Android 12+");
            return;
        }
        
        // Create an intent for the service starter receiver
        Intent alarmIntent = new Intent(context, HomeAssistantServiceStarter.class);
        // Pass the original action so the service knows what to do
        alarmIntent.setAction(originalIntent.getAction());
        if (originalIntent.getExtras() != null) {
            alarmIntent.putExtras(originalIntent.getExtras());
        }
        
        // Create PendingIntent with FLAG_IMMUTABLE for security (required on Android 12+)
        // Use timestamp as requestCode to avoid PendingIntent collisions
        // Mask to ensure positive value
        int requestCode = (int) (System.currentTimeMillis() & 0x7fffffff);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            requestCode, 
            alarmIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        try {
            // Schedule exact alarm for immediate execution
            // This creates an exempted context for foreground service start
            // Note: setExactAndAllowWhileIdle is always available since this code
            // only runs on Android 12+ (API 31) which is >= API 23
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + ALARM_DELAY_MS,
                pendingIntent
            );
            Log.d(TAG, "Service start scheduled via AlarmManager for Android 12+");
        } catch (SecurityException e) {
            // If alarm scheduling fails, fall back to direct service start
            // This may still fail with ForegroundServiceStartNotAllowedException,
            // but it's better than silently doing nothing
            Log.e(TAG, "SecurityException scheduling alarm - attempting direct service start as fallback", e);
            originalIntent.setClass(context, getPluginClass());
            context.startForegroundService(originalIntent);
        }
    }
}
