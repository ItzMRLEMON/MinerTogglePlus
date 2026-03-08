package com.minertoggleplus;

import com.minertoggleplus.commands.MinerToggleCommand;
import com.minertoggleplus.listeners.*;
import com.minertoggleplus.managers.*;
import com.minertoggleplus.util.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MinerToggle+ — Feature-rich toggle utility plugin for Paper 1.21.1
 *
 * Player Toggles:
 *   - Vein Miner          (mine entire ore veins at once)
 *   - Tree Capitator      (chop entire trees at once)
 *   - Auto Smelt          (smelt ores on pickup, fortune compatible)
 *   - Auto Replant        (auto replant crops after harvest)
 *   - Silk Spawners       (mine spawners with Silk Touch)
 *   - Double Drops        (50% chance to double ore drops)
 *   - Auto Pickup         (instant item collection to inventory)
 *   - Cave Vision         (night vision when underground)
 *   - Auto Torch          (place torches in dark areas automatically)
 *   - No Fall Damage      (disable all fall damage)
 *   - Ore Radar           (actionbar notification for nearby rare ores)
 *   - XP Boost            (bonus XP from mining ores)
 *
 * Global Admin Toggles:
 *   - ClearLagg           (timed entity/item clearing with countdown warnings)
 *   - Mob Drops           (enable/disable all mob drops globally)
 *   - Leaf Decay          (enable/disable natural leaf decay)
 */
public class MinerTogglePlus extends JavaPlugin {

    private static MinerTogglePlus instance;

    private final Map<UUID, PlayerToggleData> playerData = new HashMap<>();

    private GlobalSettings globalSettings;
    private ClearLaggManager clearLaggManager;
    private CaveVisionManager caveVisionManager;
    private OreRadarManager oreRadarManager;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Settings & managers
        globalSettings = new GlobalSettings(this);
        clearLaggManager = new ClearLaggManager(this);
        caveVisionManager = new CaveVisionManager(this);
        oreRadarManager = new OreRadarManager(this);

        // Listeners
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new CropListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Command
        MinerToggleCommand cmd = new MinerToggleCommand(this);
        getCommand("minertoggle").setExecutor(cmd);
        getCommand("minertoggle").setTabCompleter(cmd);

        // Start background tasks
        clearLaggManager.start();
        caveVisionManager.start();
        oreRadarManager.start();

        // Update checker
        updateChecker = new UpdateChecker(this);
        updateChecker.start();

        getLogger().info("MinerToggle+ v" + getDescription().getVersion() + " enabled! (" +
                Toggle.values().length + " toggles loaded)");
    }

    @Override
    public void onDisable() {
        clearLaggManager.stop();
        caveVisionManager.stop();
        oreRadarManager.stop();
        getLogger().info("MinerToggle+ disabled.");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public static MinerTogglePlus getInstance() {
        return instance;
    }

    public PlayerToggleData getPlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, k -> new PlayerToggleData());
    }

    public void removePlayerData(UUID uuid) {
        playerData.remove(uuid);
    }

    public GlobalSettings getGlobalSettings() { return globalSettings; }
    public ClearLaggManager getClearLaggManager() { return clearLaggManager; }
    public CaveVisionManager getCaveVisionManager() { return caveVisionManager; }
    public OreRadarManager getOreRadarManager() { return oreRadarManager; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
}
