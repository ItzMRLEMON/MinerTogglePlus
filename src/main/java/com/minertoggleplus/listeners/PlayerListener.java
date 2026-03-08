package com.minertoggleplus.listeners;

import com.minertoggleplus.MinerTogglePlus;
import com.minertoggleplus.Toggle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.LeavesDecayEvent;

/**
 * Handles miscellaneous player and world toggles:
 * No Fall Damage, Mob Drops (global), Leaf Decay (global), and player data cleanup.
 */
public class PlayerListener implements Listener {

    private final MinerTogglePlus plugin;

    public PlayerListener(MinerTogglePlus plugin) {
        this.plugin = plugin;
    }

    // ── NO FALL DAMAGE ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (plugin.getPlayerData(player.getUniqueId()).isEnabled(Toggle.NO_FALL_DAMAGE)) {
            event.setCancelled(true);
        }
    }

    // ── GLOBAL MOB DROPS ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        // Don't affect players
        if (entity instanceof Player) return;

        if (!plugin.getGlobalSettings().isMobDropsEnabled()) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    // ── GLOBAL LEAF DECAY ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!plugin.getGlobalSettings().isLeafDecayEnabled()) {
            event.setCancelled(true);
        }
    }

    // ── CLEANUP ON QUIT ───────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Remove cave vision effect if it was applied by us
        plugin.getCaveVisionManager().removeCaveVision(player);
        // We keep toggle data in memory for the session; remove to save memory
        plugin.removePlayerData(player.getUniqueId());
    }
}
