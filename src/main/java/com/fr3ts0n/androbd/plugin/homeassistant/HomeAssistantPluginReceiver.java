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
        // Generate requestCode from a wrapping counter to avoid overflow issues
        // Use bitwise AND to ensure positive values (equivalent to modulo but handles negatives)
        int requestCode = requestCounter.incrementAndGet() & Integer.MAX_VALUE;
        
        // FLAG_IMMUTABLE is available from API 23 (M) onwards
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            requestCode, 
            alarmIntent, 
            flags
        );
        
        try {
            // Schedule alarm for immediate execution
            // This creates an exempted context for foreground service start
            // Note: setExactAndAllowWhileIdle is available since API 23, and this code path
            // is only executed on Android 12+ (API 31+)
            
            // On Android 13+ (API 33 / TIRAMISU), check if exact alarms are permitted
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
                    Log.w(TAG, "Service start scheduled via inexact alarm because exact alarms are not permitted. " +
                            "Execution may be significantly delayed by power-saving modes. " +
                            "Time-sensitive functionality may not work properly unless exact alarms are allowed " +
                            "(SCHEDULE_EXACT_ALARM or corresponding system setting).");
                }
            } else {
                // Android 12 (API 31-32 / S and S_V2): exact alarms always permitted
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + ALARM_DELAY_MS,
                    pendingIntent
                );
                Log.d(TAG, "Service start scheduled via exact alarm for Android 12+");
            }
        } catch (SecurityException e) {
            // If alarm scheduling fails, fall back to direct service start
            // Note: This will likely fail on Android 12+ with ForegroundServiceStartNotAllowedException
            // (which extends IllegalStateException), but it's the best fallback we can provide
            Log.e(TAG, "Failed to schedule alarm, falling back to direct service start", e);
            originalIntent.setClass(context, getPluginClass());
            try {
                context.startForegroundService(originalIntent);
            } catch (IllegalStateException fallbackException) {
                // IllegalStateException includes ForegroundServiceStartNotAllowedException on Android 12+
                Log.e(
                    TAG,
                    "Alarm scheduling failed and direct foreground service start also failed. " +
                    "Service cannot be started due to Android 12+ foreground service restrictions.",
                    fallbackException
                );
                // At this point, we've exhausted all options
                // The service cannot start in this restricted context
                // Consider notifying the user that Android 12+ restrictions prevent starting the service
            } catch (SecurityException fallbackException) {
                Log.e(TAG, "Fallback service start failed due to security restrictions", fallbackException);
            }
        }
    }
}
