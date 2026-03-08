package com.minertoggleplus.managers;

import com.minertoggleplus.MinerTogglePlus;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Periodically clears dropped items and hostile/passive mobs from the world.
 * Broadcasts countdown warnings before each clear.
 */
public class ClearLaggManager {

    private final MinerTogglePlus plugin;
    private BukkitTask task;

    public ClearLaggManager(MinerTogglePlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        scheduleNextCycle();
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public void restart() {
        stop();
        scheduleNextCycle();
    }

    private void scheduleNextCycle() {
        if (!plugin.getGlobalSettings().isClearLaggEnabled()) return;

        int intervalSeconds = plugin.getGlobalSettings().getClearLaggInterval();
        List<Integer> warnings = plugin.getConfig().getIntegerList("clearlagg.warnings");

        AtomicInteger countdown = new AtomicInteger(intervalSeconds);

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int remaining = countdown.getAndDecrement();

            if (remaining <= 0) {
                executeClear();
                stop();
                scheduleNextCycle(); // schedule next cycle
                return;
            }

            if (warnings.contains(remaining)) {
                String warnMsg = plugin.getConfig().getString(
                        "messages.clearlagg-warning",
                        "&e&lWARNING: &eClearing entities in &c%time% &esecond(s)!"
                ).replace("%time%", String.valueOf(remaining));
                Bukkit.broadcastMessage(colorize(warnMsg));
            }

        }, 0L, 20L); // runs every second
    }

    private void executeClear() {
        boolean clearItems = plugin.getConfig().getBoolean("clearlagg.clear-items", true);
        boolean clearHostiles = plugin.getConfig().getBoolean("clearlagg.clear-hostiles", true);
        boolean clearPassives = plugin.getConfig().getBoolean("clearlagg.clear-passives", false);
        boolean keepNamed = plugin.getConfig().getBoolean("clearlagg.keep-named", true);
        List<String> worldNames = plugin.getConfig().getStringList("clearlagg.worlds");

        int itemsCleared = 0;
        int entitiesCleared = 0;

        for (World world : Bukkit.getWorlds()) {
            if (!worldNames.isEmpty() && !worldNames.contains(world.getName())) continue;

            for (Entity entity : world.getEntities()) {
                if (keepNamed && entity.getCustomName() != null) continue;

                if (clearItems && entity instanceof Item) {
                    entity.remove();
                    itemsCleared++;
                } else if (clearHostiles && entity instanceof Monster) {
                    entity.remove();
                    entitiesCleared++;
                } else if (clearPassives && entity instanceof Animals) {
                    entity.remove();
                    entitiesCleared++;
                }
            }
        }

        String clearedMsg = plugin.getConfig().getString(
                "messages.clearlagg-cleared",
                "&aCleared &e%items% &aitems and &e%entities% &aentities."
        ).replace("%items%", String.valueOf(itemsCleared))
         .replace("%entities%", String.valueOf(entitiesCleared));

        Bukkit.broadcastMessage(colorize(
                plugin.getConfig().getString("messages.prefix", "&8[&6MinerToggle+&8] &r") + clearedMsg));
    }

    private String colorize(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}
