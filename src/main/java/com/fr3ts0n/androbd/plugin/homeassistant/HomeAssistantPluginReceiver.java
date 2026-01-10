package com.fr3ts0n.androbd.plugin.homeassistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Plugin broadcast receiver for Home Assistant plugin
 * Handles service start with Android 12+ background service restrictions
 */
public class HomeAssistantPluginReceiver extends com.fr3ts0n.androbd.plugin.PluginReceiver {
    private static final String TAG = "HomeAssistantPluginReceiver";
    
    // Minimal delay for AlarmManager scheduling (1ms for immediate execution)
    private static final long ALARM_DELAY_MS = 1;
    
    // Counter to prevent PendingIntent collisions (thread-safe)
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    
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
            Log.e(TAG, "AlarmManager not available - falling back to direct service start");
            // Fallback to direct service start, may fail with ForegroundServiceStartNotAllowedException
            originalIntent.setClass(context, getPluginClass());
            context.startForegroundService(originalIntent);
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
        // Use timestamp with counter to avoid PendingIntent collisions
        // Combine lower 16 bits of timestamp with counter (mod 65536) in upper 16 bits
        int requestCode = (int) ((System.currentTimeMillis() & 0xFFFF) | 
                                  ((requestCounter.getAndIncrement() & 0xFFFF) << 16));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            requestCode, 
            alarmIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        try {
            // Schedule alarm for immediate execution
            // This creates an exempted context for foreground service start
            // Note: setExactAndAllowWhileIdle is always available since this code
            // only runs on Android 12+ (API 31) which is >= API 23
            
            // On Android 13+ (API 33), check if exact alarms are permitted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + ALARM_DELAY_MS,
                        pendingIntent
                    );
                    Log.d(TAG, "Service start scheduled via exact alarm for Android 12+");
                } else {
                    // Fall back to inexact alarm if exact alarms not permitted
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + ALARM_DELAY_MS,
                        pendingIntent
                    );
                    Log.d(TAG, "Service start scheduled via inexact alarm (exact alarms not permitted)");
                }
            } else {
                // Android 12 (API 31-32): exact alarms always permitted
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + ALARM_DELAY_MS,
                    pendingIntent
                );
                Log.d(TAG, "Service start scheduled via exact alarm for Android 12+");
            }
        } catch (SecurityException e) {
            // If alarm scheduling fails, fall back to direct service start
            // This may still fail with ForegroundServiceStartNotAllowedException,
            // but it's better than silently doing nothing
            // SecurityException can occur if SCHEDULE_EXACT_ALARM permission is missing
            // or if alarm scheduling is restricted by the system
            Log.e(TAG, "SecurityException scheduling alarm (SCHEDULE_EXACT_ALARM permission issue?) - attempting direct service start as fallback", e);
            originalIntent.setClass(context, getPluginClass());
            context.startForegroundService(originalIntent);
        }
    }
}
