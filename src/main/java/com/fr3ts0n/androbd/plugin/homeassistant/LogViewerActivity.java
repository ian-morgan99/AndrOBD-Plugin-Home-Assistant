package com.fr3ts0n.androbd.plugin.homeassistant;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity for viewing and managing application logs
 */
public class LogViewerActivity extends Activity {
    private TextView logTextView;
    private ScrollView scrollView;
    private LogManager logManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create layout programmatically to avoid XML resources
        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT
        ));
        scrollView.setFillViewport(true);
        scrollView.setPadding(16, 16, 16, 16);
        
        // Create vertical layout container
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        // Create log text view
        logTextView = new TextView(this);
        logTextView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        ));
        logTextView.setTextSize(10);
        logTextView.setTypeface(android.graphics.Typeface.MONOSPACE);
        logTextView.setPadding(8, 8, 8, 8);
        logTextView.setBackgroundColor(0xFF000000);
        logTextView.setTextColor(0xFFFFFFFF);
        
        // Create button container
        android.widget.LinearLayout buttonLayout = new android.widget.LinearLayout(this);
        buttonLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        buttonLayout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        buttonLayout.setPadding(0, 16, 0, 0);
        
        // Create Copy button
        Button copyButton = new Button(this);
        copyButton.setText("Copy to Clipboard");
        copyButton.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        ));
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyLogsToClipboard();
            }
        });
        
        // Create Clear button
        Button clearButton = new Button(this);
        clearButton.setText("Clear Logs");
        clearButton.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        ));
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLogs();
            }
        });
        
        // Create Refresh button
        Button refreshButton = new Button(this);
        refreshButton.setText("Refresh");
        refreshButton.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        ));
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadLogs();
            }
        });
        
        // Add buttons to button layout
        buttonLayout.addView(copyButton);
        buttonLayout.addView(clearButton);
        buttonLayout.addView(refreshButton);
        
        // Add views to layout
        layout.addView(logTextView);
        layout.addView(buttonLayout);
        
        scrollView.addView(layout);
        setContentView(scrollView);
        
        // Initialize log manager
        logManager = new LogManager(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        logManager.setLoggingEnabled(prefs.getBoolean("ha_enable_logging", false));
        
        // Load logs
        loadLogs();
    }
    
    private void loadLogs() {
        String logs = logManager.getObfuscatedLog();
        logTextView.setText(logs);
        
        // Scroll to bottom
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
    
    private void copyLogsToClipboard() {
        String logs = logManager.getObfuscatedLog();
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("AndrOBD HA Plugin Logs", logs);
        clipboard.setPrimaryClip(clip);
        
        Toast.makeText(this, "Logs copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    
    private void clearLogs() {
        logManager.clearLogs();
        loadLogs();
        Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show();
    }
}
