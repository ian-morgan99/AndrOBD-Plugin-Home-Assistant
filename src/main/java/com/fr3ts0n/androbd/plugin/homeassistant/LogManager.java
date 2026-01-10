package com.fr3ts0n.androbd.plugin.homeassistant;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Manages application logging with obfuscation for sensitive data.
 * Logs are stored in a file that can be viewed and shared for debugging.
 */
public class LogManager {
    private static final String TAG = "LogManager";
    private static final String LOG_FILE_NAME = "androbd_ha_plugin.log";
    private static final int MAX_LOG_SIZE = 500 * 1024; // 500KB max log size
    private static final SimpleDateFormat DATE_FORMAT = 
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    
    private final Context context;
    private final File logFile;
    private boolean loggingEnabled = false;
    
    // Patterns for obfuscation
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(bearer\\s+|token[\"']?\\s*[:=]\\s*[\"']?)([a-zA-Z0-9_\\-\\.]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[^/]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SSID_PATTERN = Pattern.compile(
            "(ssid[\"']?\\s*[:=]\\s*[\"']?)([^\"'\\s,}]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern IP_PATTERN = Pattern.compile(
            "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"
    );
    
    public LogManager(Context context) {
        this.context = context;
        this.logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
    }
    
    /**
     * Enable or disable logging
     */
    public void setLoggingEnabled(boolean enabled) {
        this.loggingEnabled = enabled;
        if (enabled) {
            logInfo("Logging enabled");
        }
    }
    
    /**
     * Check if logging is enabled
     */
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }
    
    /**
     * Log an info message
     */
    public void logInfo(String message) {
        log("INFO", message);
    }
    
    /**
     * Log a warning message
     */
    public void logWarning(String message) {
        log("WARN", message);
    }
    
    /**
     * Log an error message
     */
    public void logError(String message) {
        log("ERROR", message);
    }
    
    /**
     * Log an error with exception
     */
    public void logError(String message, Throwable throwable) {
        log("ERROR", message + "\n" + getStackTrace(throwable));
    }
    
    /**
     * Log a debug message
     */
    public void logDebug(String message) {
        log("DEBUG", message);
    }
    
    /**
     * Internal log method
     */
    private void log(String level, String message) {
        if (!loggingEnabled) {
            return;
        }
        
        String timestamp = DATE_FORMAT.format(new Date());
        String logLine = String.format("[%s] %s: %s\n", timestamp, level, message);
        
        // Also log to Android logcat for immediate debugging
        switch (level) {
            case "ERROR":
                Log.e(TAG, message);
                break;
            case "WARN":
                Log.w(TAG, message);
                break;
            case "DEBUG":
                Log.d(TAG, message);
                break;
            default:
                Log.i(TAG, message);
                break;
        }
        
        // Write to file
        writeToFile(logLine);
    }
    
    /**
     * Write log line to file
     */
    private synchronized void writeToFile(String logLine) {
        try {
            // Check file size and truncate if needed
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                truncateLog();
            }
            
            // Append to log file
            FileWriter writer = new FileWriter(logFile, true);
            writer.write(logLine);
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file", e);
        }
    }
    
    /**
     * Truncate log file to half its size, keeping most recent entries
     */
    private void truncateLog() {
        try {
            // Read all lines
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            // Keep only second half
            String fullContent = content.toString();
            int midpoint = fullContent.length() / 2;
            String truncated = fullContent.substring(midpoint);
            
            // Write back truncated content
            FileWriter writer = new FileWriter(logFile, false);
            writer.write("... [log truncated] ...\n");
            writer.write(truncated);
            writer.close();
            
            Log.i(TAG, "Log file truncated to " + logFile.length() + " bytes");
        } catch (IOException e) {
            Log.e(TAG, "Failed to truncate log file", e);
        }
    }
    
    /**
     * Get the full log content with obfuscation applied
     */
    public String getObfuscatedLog() {
        if (!logFile.exists()) {
            return "No logs available yet.";
        }
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            return obfuscateSensitiveData(content.toString());
        } catch (IOException e) {
            Log.e(TAG, "Failed to read log file", e);
            return "Error reading log file: " + e.getMessage();
        }
    }
    
    /**
     * Obfuscate sensitive data in log content
     */
    private String obfuscateSensitiveData(String content) {
        // Obfuscate bearer tokens
        content = TOKEN_PATTERN.matcher(content).replaceAll("$1[REDACTED_TOKEN]");
        
        // Obfuscate URLs (keep protocol and first part of domain)
        content = URL_PATTERN.matcher(content).replaceAll(match -> {
            String url = match.group(1);
            // Keep protocol and first few characters
            int protocolEnd = url.indexOf("://");
            if (protocolEnd != -1 && url.length() > protocolEnd + 3) {
                String protocol = url.substring(0, protocolEnd + 3);
                return protocol + "[REDACTED_HOST]";
            }
            return "[REDACTED_URL]";
        });
        
        // Obfuscate SSIDs
        content = SSID_PATTERN.matcher(content).replaceAll("$1[REDACTED_SSID]");
        
        // Obfuscate IP addresses
        content = IP_PATTERN.matcher(content).replaceAll("[REDACTED_IP]");
        
        return content;
    }
    
    /**
     * Clear all logs
     */
    public void clearLogs() {
        if (logFile.exists()) {
            logFile.delete();
        }
        logInfo("Logs cleared");
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
        }
        
        Throwable cause = throwable.getCause();
        if (cause != null) {
            sb.append("Caused by: ");
            sb.append(getStackTrace(cause));
        }
        
        return sb.toString();
    }
}
