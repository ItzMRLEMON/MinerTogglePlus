package com.minertoggleplus.managers;

import com.minertoggleplus.MinerTogglePlus;
import com.minertoggleplus.Toggle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Scans for rare ores near players who have Ore Radar enabled
 * and sends actionbar notifications.
 */
public class OreRadarManager {

    private final MinerTogglePlus plugin;
    private BukkitTask task;

    public OreRadarManager(MinerTogglePlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int interval = plugin.getConfig().getInt("ore-radar.scan-interval", 100);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::scan, interval, interval);
    }

    public void stop() {
        if (task != null && !task.isCancelled()) task.cancel();
    }

    private void scan() {
        int radius = plugin.getConfig().getInt("ore-radar.radius", 16);
        List<String> detectList = plugin.getConfig().getStringList("ore-radar.detect-ores");
        Set<Material> detectOres = new HashSet<>();
        for (String s : detectList) {
            try { detectOres.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
        }

        if (detectOres.isEmpty()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getPlayerData(player.getUniqueId()).isEnabled(Toggle.ORE_RADAR)) continue;

            Map<Material, Integer> counts = countNearbyOres(player.getLocation(), radius, detectOres);
            if (counts.isEmpty()) continue;

            StringBuilder sb = new StringBuilder("§d[Radar] §e");
            boolean first = true;
            for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
                if (!first) sb.append("§7 | §e");
                sb.append("§6").append(entry.getValue()).append("§e ")
                  .append(friendlyName(entry.getKey()));
                first = false;
            }

            player.sendActionBar(Component.text(sb.toString()).color(NamedTextColor.YELLOW));
        }
    }

    private Map<Material, Integer> countNearbyOres(Location center, int radius, Set<Material> target) {
        Map<Material, Integer> counts = new EnumMap<>(Material.class);
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -radius; x <= radius; x += 2) {
            for (int y = -radius; y <= radius; y += 2) {
                for (int z = -radius; z <= radius; z += 2) {
                    Block block = center.getWorld().getBlockAt(cx + x, cy + y, cz + z);
                    if (target.contains(block.getType())) {
                        counts.merge(block.getType(), 1, Integer::sum);
                    }
                }
            }
        }
        return counts;
    }

    private String friendlyName(Material mat) {
        return mat.name().replace("_ORE", "").replace("DEEPSLATE_", "").replace("_", " ").toLowerCase();
    }
}
