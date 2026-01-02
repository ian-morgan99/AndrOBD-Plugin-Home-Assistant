package com.fr3ts0n.androbd.plugin.homeassistant;

import com.fr3ts0n.androbd.plugin.PluginReceiver;

/**
 * Plugin broadcast receiver for Home Assistant plugin
 */
public class PluginReceiver extends com.fr3ts0n.androbd.plugin.PluginReceiver {
    @Override
    public Class getPluginClass() {
        return HomeAssistantPlugin.class;
    }
}
