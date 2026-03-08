package com.minertoggleplus.managers;

import com.minertoggleplus.MinerTogglePlus;
import com.minertoggleplus.Toggle;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Periodically checks player Y-level and applies/removes Night Vision for Cave Vision toggle.
 */
public class CaveVisionManager {

    private final MinerTogglePlus plugin;
    private BukkitTask task;

    // Duration: 10 seconds (200 ticks) — refreshed every 4s so it never expires visually
    private static final int EFFECT_DURATION = 200;

    public CaveVisionManager(MinerTogglePlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Check every 4 seconds (80 ticks)
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 80L);
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        // Remove effect from all online players who have it from us
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeCaveVision(player);
        }
    }

    private void tick() {
        int undergroundY = plugin.getConfig().getInt("cave-vision.underground-y", 64);
        boolean overworldOnly = plugin.getConfig().getBoolean("cave-vision.overworld-only", true);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;

            boolean caveVisionEnabled = plugin.getPlayerData(player.getUniqueId()).isEnabled(Toggle.CAVE_VISION);
            if (!caveVisionEnabled) {
                removeCaveVision(player);
                continue;
            }

            boolean inOverworld = player.getWorld().getEnvironment() == World.Environment.NORMAL;
            if (overworldOnly && !inOverworld) {
                removeCaveVision(player);
                continue;
            }

            boolean underground = player.getLocation().getY() < undergroundY;
            if (underground) {
                applyCaveVision(player);
            } else {
                removeCaveVision(player);
            }
        }
    }

    private void applyCaveVision(Player player) {
        PotionEffect existing = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
        // Only apply if not already at amplifier 0 from us (avoid potion override)
        if (existing == null || existing.getDuration() < 100) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    EFFECT_DURATION,
                    0,
                    false,  // ambient (no particles)
                    false,  // particles
                    false   // icon (hide from HUD)
            ));
        }
    }

    public void removeCaveVision(Player player) {
        PotionEffect effect = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
        if (effect != null && !effect.hasParticles() && !effect.hasIcon()) {
            // Only remove if it looks like our invisible effect (not a potion the player drank)
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }
}
