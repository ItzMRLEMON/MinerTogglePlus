package com.minertoggleplus.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Utility methods for item handling, auto-smelt, and inventory management.
 */
public final class ItemUtils {

    private ItemUtils() {}

    // ── Smelt result map: ore block → smelted item ─────────────────────────────

    private static final Map<Material, Material> SMELT_RESULTS = new EnumMap<>(Material.class);
    // XP granted by furnace per smelt operation (scaled to ints for ExperienceOrb)
    private static final Map<Material, Integer> SMELT_XP = new EnumMap<>(Material.class);

    static {
        // Iron
        SMELT_RESULTS.put(Material.IRON_ORE, Material.IRON_INGOT);
        SMELT_RESULTS.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        SMELT_XP.put(Material.IRON_ORE, 1);
        SMELT_XP.put(Material.DEEPSLATE_IRON_ORE, 1);

        // Gold
        SMELT_RESULTS.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SMELT_RESULTS.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_XP.put(Material.GOLD_ORE, 1);
        SMELT_XP.put(Material.DEEPSLATE_GOLD_ORE, 1);

        // Copper
        SMELT_RESULTS.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SMELT_RESULTS.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        SMELT_XP.put(Material.COPPER_ORE, 1);
        SMELT_XP.put(Material.DEEPSLATE_COPPER_ORE, 1);

        // Nether Gold (fortune-compatible: nuggets → ingots)
        SMELT_RESULTS.put(Material.NETHER_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_XP.put(Material.NETHER_GOLD_ORE, 1);

        // Ancient Debris → Netherite Scrap
        SMELT_RESULTS.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        SMELT_XP.put(Material.ANCIENT_DEBRIS, 2);
    }

    public static boolean hasSmeltResult(Material blockType) {
        return SMELT_RESULTS.containsKey(blockType);
    }

    /**
     * Returns the smelted drops for a given ore block, fortune-compatible.
     * For iron/gold/copper: 1 ingot per ore (fortune doesn't affect raw ore count in 1.17+).
     * For nether gold: fortune-multiplied nuggets are converted to ingots (9 nuggets = 1 ingot).
     * For ancient debris: 1 netherite scrap.
     */
    public static List<ItemStack> getSmeltedDrops(Block block, ItemStack tool) {
        Material type = block.getType();
        Material result = SMELT_RESULTS.get(type);
        if (result == null) return List.of();

        // Nether gold: get natural drops (nuggets), convert to ingots
        if (type == Material.NETHER_GOLD_ORE) {
            int nuggets = 0;
            Collection<ItemStack> naturalDrops = block.getDrops(tool);
            for (ItemStack drop : naturalDrops) {
                if (drop.getType() == Material.GOLD_NUGGET) {
                    nuggets += drop.getAmount();
                }
            }
            int ingots = nuggets / 9;
            int leftoverNuggets = nuggets % 9;
            List<ItemStack> drops = new ArrayList<>();
            if (ingots > 0) drops.add(new ItemStack(Material.GOLD_INGOT, ingots));
            if (leftoverNuggets > 0) drops.add(new ItemStack(Material.GOLD_NUGGET, leftoverNuggets));
            return drops;
        }

        // All others: 1 smelted result per ore
        return List.of(new ItemStack(result, 1));
    }

    /** Returns XP to award for auto-smelting a given ore type. */
    public static int getSmeltXp(Material blockType) {
        return SMELT_XP.getOrDefault(blockType, 0);
    }

    // ── Sets used by listeners ──────────────────────────────────────────────────

    public static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    public static final Set<Material> LOGS = EnumSet.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD,
            Material.JUNGLE_WOOD, Material.ACACIA_WOOD, Material.DARK_OAK_WOOD,
            Material.MANGROVE_WOOD, Material.CHERRY_WOOD,
            Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG,
            Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_JUNGLE_LOG,
            Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
            Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG
    );

    public static final Set<Material> LEAVES = EnumSet.of(
            Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES,
            Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
            Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES, Material.AZALEA_LEAVES,
            Material.FLOWERING_AZALEA_LEAVES
    );

    public static final Set<Material> PICKAXES = EnumSet.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
    );

    public static final Set<Material> AXES = EnumSet.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
    );

    // ── Crop-related helpers ────────────────────────────────────────────────────

    /** Maps a crop block to the seed needed to replant it. */
    private static final Map<Material, Material> CROP_SEEDS = new EnumMap<>(Material.class);

    static {
        CROP_SEEDS.put(Material.WHEAT, Material.WHEAT_SEEDS);
        CROP_SEEDS.put(Material.CARROTS, Material.CARROT);
        CROP_SEEDS.put(Material.POTATOES, Material.POTATO);
        CROP_SEEDS.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        CROP_SEEDS.put(Material.NETHER_WART, Material.NETHER_WART);
        CROP_SEEDS.put(Material.COCOA, Material.COCOA_BEANS);
        CROP_SEEDS.put(Material.PITCHER_CROP, Material.PITCHER_POD);
        CROP_SEEDS.put(Material.TORCHFLOWER_CROP, Material.TORCHFLOWER_SEEDS);
    }

    public static boolean isCrop(Material m) {
        return CROP_SEEDS.containsKey(m);
    }

    public static Material getSeed(Material crop) {
        return CROP_SEEDS.get(crop);
    }

    // ── Inventory helpers ───────────────────────────────────────────────────────

    /**
     * Gives an item to a player. Drops naturally at the block location if inventory is full.
     */
    public static void giveItem(Player player, Location dropLoc, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack overflow : leftover.values()) {
            player.getWorld().dropItemNaturally(dropLoc, overflow);
        }
    }

    /**
     * Applies Unbreaking-aware damage to the tool in the player's main hand.
     * Returns {@code false} if the tool broke.
     */
    public static boolean damageTool(Player player, int amount) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType().isAir()) return true;
        if (!(tool.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable)) return true;

        int unbreaking = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        for (int i = 0; i < amount; i++) {
            // Unbreaking: 1/(unbreaking+1) chance to actually take damage
            if (unbreaking > 0 && new Random().nextInt(unbreaking + 1) != 0) continue;
            damageable.setDamage(damageable.getDamage() + 1);
            if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
                player.getInventory().setItemInMainHand(null);
                player.getWorld().playSound(player.getLocation(),
                        org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                return false;
            }
        }
        tool.setItemMeta(damageable);
        player.getInventory().setItemInMainHand(tool);
        return true;
    }
}
