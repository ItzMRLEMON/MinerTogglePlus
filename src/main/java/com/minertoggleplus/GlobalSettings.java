package com.minertoggleplus;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Holds global (admin-controlled) toggle states loaded from config.
 */
public class GlobalSettings {

    private final MinerTogglePlus plugin;

    private boolean clearLaggEnabled;
    private int clearLaggInterval;
    private boolean mobDropsEnabled;
    private boolean leafDecayEnabled;

    public GlobalSettings(MinerTogglePlus plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        clearLaggEnabled = cfg.getBoolean("clearlagg.enabled", true);
        clearLaggInterval = cfg.getInt("clearlagg.interval", 300);
        mobDropsEnabled = cfg.getBoolean("mob-drops.enabled", true);
        leafDecayEnabled = cfg.getBoolean("leaf-decay.enabled", true);
    }

    // ── ClearLagg ──────────────────────────────────────────
    public boolean isClearLaggEnabled() { return clearLaggEnabled; }
    public void setClearLaggEnabled(boolean v) { clearLaggEnabled = v; }

    public int getClearLaggInterval() { return clearLaggInterval; }
    public void setClearLaggInterval(int v) { clearLaggInterval = v; }

    // ── Mob Drops ──────────────────────────────────────────
    public boolean isMobDropsEnabled() { return mobDropsEnabled; }
    public void setMobDropsEnabled(boolean v) { mobDropsEnabled = v; }

    // ── Leaf Decay ─────────────────────────────────────────
    public boolean isLeafDecayEnabled() { return leafDecayEnabled; }
    public void setLeafDecayEnabled(boolean v) { leafDecayEnabled = v; }
}
