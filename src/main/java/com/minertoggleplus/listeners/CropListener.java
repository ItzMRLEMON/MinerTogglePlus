package com.minertoggleplus.listeners;

import com.minertoggleplus.MinerTogglePlus;
import com.minertoggleplus.Toggle;
import com.minertoggleplus.util.ItemUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

/**
 * Handles Auto Replant: when a player breaks a fully-grown crop,
 * it is automatically replanted by consuming one seed from their inventory.
 */
public class CropListener implements Listener {

    private final MinerTogglePlus plugin;

    public CropListener(MinerTogglePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getPlayerData(player.getUniqueId()).isEnabled(Toggle.AUTO_REPLANT)) return;

        Block block = event.getBlock();
        Material type = block.getType();
        if (!ItemUtils.isCrop(type)) return;

        // Only replant fully grown crops
        if (block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() < ageable.getMaximumAge()) return;
        } else {
            return;
        }

        Material seedType = ItemUtils.getSeed(type);
        if (seedType == null) return;

        // Let the normal drop happen, then schedule replant
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (block.getType() == Material.AIR || block.getType() == type) {
                // Block is now air — try to replant
                if (consumeSeed(player, seedType)) {
                    block.setType(type);
                    // Reset age to 0
                    if (block.getBlockData() instanceof Ageable newAgeable) {
                        newAgeable.setAge(0);
                        block.setBlockData(newAgeable);
                    }
                }
            }
        }, 1L);
    }

    /**
     * Removes one seed of the given type from the player's inventory.
     * @return true if a seed was found and consumed
     */
    private boolean consumeSeed(Player player, Material seedType) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == seedType) {
                if (item.getAmount() <= 1) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
                return true;
            }
        }
        return false;
    }
}
