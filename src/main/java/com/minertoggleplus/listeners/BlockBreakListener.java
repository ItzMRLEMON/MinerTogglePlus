package com.minertoggleplus.listeners;

import com.minertoggleplus.MinerTogglePlus;
import com.minertoggleplus.PlayerToggleData;
import com.minertoggleplus.Toggle;
import com.minertoggleplus.util.ItemUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Handles all block-break-related toggles:
 * VeinMiner, Tree Capitator, Auto Smelt, Double Drops, Auto Pickup, Auto Torch, Silk Spawners, XP Boost.
 */
public class BlockBreakListener implements Listener {

    private final MinerTogglePlus plugin;
    /** Prevents recursive event firing when breaking vein/tree blocks. */
    private static final Set<UUID> processing = Collections.synchronizedSet(new HashSet<>());

    public BlockBreakListener(MinerTogglePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Prevent recursive calls from vein/tree breaking
        if (processing.contains(uuid)) return;

        Block block = event.getBlock();
        Material type = block.getType();
        PlayerToggleData data = plugin.getPlayerData(uuid);
        ItemStack tool = player.getInventory().getItemInMainHand().clone();

        boolean isOre = ItemUtils.ORES.contains(type);
        boolean isLog = ItemUtils.LOGS.contains(type);

        // ── SILK SPAWNERS ─────────────────────────────────────────────────────
        if (type == Material.SPAWNER && data.isEnabled(Toggle.SILK_SPAWNERS)) {
            if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
                event.setDropItems(false);
                event.setExpToDrop(0);
                ItemUtils.giveItem(player, block.getLocation(), new ItemStack(Material.SPAWNER));
            }
            return;
        }

        // ── VEIN MINER ────────────────────────────────────────────────────────
        if (isOre && data.isEnabled(Toggle.VEIN_MINER) && ItemUtils.PICKAXES.contains(tool.getType())) {
            boolean requireSneak = plugin.getConfig().getBoolean("vein-miner.require-sneak", true);
            if (!requireSneak || player.isSneaking()) {
                event.setDropItems(false);
                event.setExpToDrop(0);
                processing.add(uuid);
                try {
                    handleVeinMiner(player, block, tool, data);
                } finally {
                    processing.remove(uuid);
                }
                return;
            }
        }

        // ── TREE CAPITATOR ────────────────────────────────────────────────────
        if (isLog && data.isEnabled(Toggle.TREE_CAPITATOR) && ItemUtils.AXES.contains(tool.getType())) {
            boolean requireSneak = plugin.getConfig().getBoolean("tree-capitator.require-sneak", false);
            if (!requireSneak || player.isSneaking()) {
                event.setDropItems(false);
                event.setExpToDrop(0);
                processing.add(uuid);
                try {
                    handleTreeCapitator(player, block, tool, data);
                } finally {
                    processing.remove(uuid);
                }
                return;
            }
        }

        // ── SINGLE-BLOCK processing (Auto Smelt, Double Drops, Auto Pickup, XP Boost) ──
        if (isOre) {
            processSingleOreDrop(event, player, block, tool, data);
        }

        // ── AUTO TORCH ────────────────────────────────────────────────────────
        if (data.isEnabled(Toggle.AUTO_TORCH)) {
            // Delay by 1 tick so the block is already air when we check
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> tryPlaceTorch(player, block.getLocation()), 1L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VEIN MINER
    // ─────────────────────────────────────────────────────────────────────────

    private void handleVeinMiner(Player player, Block startBlock, ItemStack tool, PlayerToggleData data) {
        int maxBlocks = plugin.getConfig().getInt("vein-miner.max-blocks", 64);
        boolean damageTool = plugin.getConfig().getBoolean("vein-miner.damage-tool", true);
        Material targetType = startBlock.getType();
        Set<Block> vein = collectConnected(startBlock, targetType, maxBlocks, true);

        for (Block block : vein) {
            if (player.getInventory().getItemInMainHand().getType().isAir()) break;

            List<ItemStack> drops = getProcessedDrops(player, block, tool, data);
            block.setType(Material.AIR);

            for (ItemStack drop : drops) {
                if (data.isEnabled(Toggle.AUTO_PICKUP)) {
                    ItemUtils.giveItem(player, block.getLocation(), drop);
                } else {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }

            // XP drop
            spawnOreXp(block, tool, data);

            if (damageTool) {
                if (!ItemUtils.damageTool(player, 1)) break; // tool broke
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TREE CAPITATOR
    // ─────────────────────────────────────────────────────────────────────────

    private void handleTreeCapitator(Player player, Block startBlock, ItemStack tool, PlayerToggleData data) {
        int maxLogs = plugin.getConfig().getInt("tree-capitator.max-logs", 200);
        boolean damageTool = plugin.getConfig().getBoolean("tree-capitator.damage-tool", true);
        boolean breakLeaves = plugin.getConfig().getBoolean("tree-capitator.break-leaves", true);
        Material targetType = startBlock.getType();

        Set<Block> logs = collectConnected(startBlock, targetType, maxLogs, false);

        // Optionally also collect adjacent leaves
        Set<Block> leaves = new HashSet<>();
        if (breakLeaves) {
            for (Block log : logs) {
                for (int x = -2; x <= 2; x++) {
                    for (int y = -1; y <= 2; y++) {
                        for (int z = -2; z <= 2; z++) {
                            Block adj = log.getRelative(x, y, z);
                            if (ItemUtils.LEAVES.contains(adj.getType())) leaves.add(adj);
                        }
                    }
                }
            }
        }

        for (Block block : logs) {
            if (player.getInventory().getItemInMainHand().getType().isAir()) break;

            Collection<ItemStack> drops = block.getDrops(tool);
            block.setType(Material.AIR);
            for (ItemStack drop : drops) {
                if (data.isEnabled(Toggle.AUTO_PICKUP)) {
                    ItemUtils.giveItem(player, block.getLocation(), drop);
                } else {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }
            if (damageTool) {
                if (!ItemUtils.damageTool(player, 1)) break;
            }
        }

        // Remove leaves silently (no drops for performance)
        for (Block leaf : leaves) {
            if (ItemUtils.LEAVES.contains(leaf.getType())) {
                leaf.setType(Material.AIR);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SINGLE BLOCK DROP HANDLING
    // ─────────────────────────────────────────────────────────────────────────

    private void processSingleOreDrop(BlockBreakEvent event, Player player, Block block,
                                      ItemStack tool, PlayerToggleData data) {

        boolean autoSmelt = data.isEnabled(Toggle.AUTO_SMELT) && ItemUtils.hasSmeltResult(block.getType());
        boolean doubleDrops = data.isEnabled(Toggle.DOUBLE_DROPS);
        boolean autoPickup = data.isEnabled(Toggle.AUTO_PICKUP);
        boolean xpBoost = data.isEnabled(Toggle.XP_BOOST);

        // Only take control of drops if a toggle requires it
        if (!autoSmelt && !doubleDrops && !autoPickup && !xpBoost) return;

        event.setDropItems(false);

        List<ItemStack> drops;
        if (autoSmelt) {
            drops = ItemUtils.getSmeltedDrops(block, tool);
            int smeltXp = ItemUtils.getSmeltXp(block.getType());
            if (smeltXp > 0) {
                int xp = xpBoost ? (int)(smeltXp * plugin.getConfig().getDouble("xp-boost.multiplier", 1.5)) : smeltXp;
                spawnXp(block.getLocation(), xp);
            }
            event.setExpToDrop(0);
        } else {
            drops = new ArrayList<>(block.getDrops(tool));

            // XP Boost
            if (xpBoost && event.getExpToDrop() > 0) {
                double mult = plugin.getConfig().getDouble("xp-boost.multiplier", 1.5);
                event.setExpToDrop((int)(event.getExpToDrop() * mult));
            }
        }

        // Double Drops (50% chance)
        if (doubleDrops) {
            double chance = plugin.getConfig().getDouble("double-drops.chance", 0.5);
            if (Math.random() < chance) {
                List<ItemStack> extra = new ArrayList<>();
                for (ItemStack drop : drops) {
                    extra.add(drop.clone());
                }
                drops.addAll(extra);
            }
        }

        // Deliver drops
        for (ItemStack drop : drops) {
            if (autoPickup) {
                ItemUtils.giveItem(player, block.getLocation(), drop);
            } else {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * BFS to collect connected blocks of the same material.
     * @param diagonal if true, checks all 26 neighbours (ores); if false, 6-directional + up/sideways (trees).
     */
    private Set<Block> collectConnected(Block start, Material type, int maxBlocks, boolean diagonal) {
        Set<Block> result = new LinkedHashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(start);
        result.add(start);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block current = queue.poll();

            int range = diagonal ? 1 : 1;
            for (int x = -range; x <= range; x++) {
                for (int y = -range; y <= range; y++) {
                    for (int z = -range; z <= range; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        if (!diagonal && Math.abs(x) + Math.abs(y) + Math.abs(z) > 1 && y < 0) continue;
                        Block neighbor = current.getRelative(x, y, z);
                        if (!result.contains(neighbor) && neighbor.getType() == type) {
                            result.add(neighbor);
                            queue.add(neighbor);
                            if (result.size() >= maxBlocks) return result;
                        }
                    }
                }
            }
        }
        return result;
    }

    /** Returns processed drops for a vein block, applying Auto Smelt and Double Drops. */
    private List<ItemStack> getProcessedDrops(Player player, Block block, ItemStack tool, PlayerToggleData data) {
        List<ItemStack> drops;

        if (data.isEnabled(Toggle.AUTO_SMELT) && ItemUtils.hasSmeltResult(block.getType())) {
            drops = ItemUtils.getSmeltedDrops(block, tool);
            // Award smelt XP
            int xp = ItemUtils.getSmeltXp(block.getType());
            if (xp > 0 && plugin.getConfig().getBoolean("auto-smelt.give-smelt-xp", true)) {
                if (data.isEnabled(Toggle.XP_BOOST)) {
                    xp = (int)(xp * plugin.getConfig().getDouble("xp-boost.multiplier", 1.5));
                }
                spawnXp(block.getLocation(), xp);
            }
        } else {
            drops = new ArrayList<>(block.getDrops(tool));
        }

        if (data.isEnabled(Toggle.DOUBLE_DROPS)) {
            double chance = plugin.getConfig().getDouble("double-drops.chance", 0.5);
            if (Math.random() < chance) {
                List<ItemStack> extra = new ArrayList<>();
                for (ItemStack d : drops) extra.add(d.clone());
                drops.addAll(extra);
            }
        }

        return drops;
    }

    private void spawnOreXp(Block block, ItemStack tool, PlayerToggleData data) {
        if (data.isEnabled(Toggle.XP_BOOST)) {
            // Vein miner blocks: give a small fixed XP per ore
            double mult = plugin.getConfig().getDouble("xp-boost.multiplier", 1.5);
            spawnXp(block.getLocation(), (int)(2 * mult));
        }
    }

    private void spawnXp(org.bukkit.Location loc, int amount) {
        if (amount <= 0) return;
        loc.getWorld().spawn(loc, ExperienceOrb.class, orb -> orb.setExperience(amount));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AUTO TORCH
    // ─────────────────────────────────────────────────────────────────────────

    private void tryPlaceTorch(Player player, Location loc) {
        Block block = loc.getBlock();
        if (block.getType() != Material.AIR) return;

        int minLight = plugin.getConfig().getInt("auto-torch.min-light-level", 7);
        if (block.getLightLevel() > minLight) return;

        // Need a solid block below
        Block below = block.getRelative(0, -1, 0);
        if (!below.getType().isSolid()) return;

        // Find a torch in player's inventory
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.TORCH) {
                block.setType(Material.TORCH);
                if (item.getAmount() <= 1) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
                return;
            }
        }
    }
}
