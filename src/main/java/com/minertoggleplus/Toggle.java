package com.minertoggleplus;

/**
 * All player-toggleable features in MinerToggle+.
 */
public enum Toggle {

    VEIN_MINER(
            "vein_miner",
            "§aVein Miner",
            "Mine an entire ore vein at once. Sneak while breaking to activate.",
            "minertoggleplus.toggle.vein_miner"
    ),
    TREE_CAPITATOR(
            "tree_capitator",
            "§2Tree Capitator",
            "Chop an entire tree by breaking one log.",
            "minertoggleplus.toggle.tree_capitator"
    ),
    AUTO_SMELT(
            "auto_smelt",
            "§6Auto Smelt",
            "Ores are automatically smelted when mined. Fortune compatible.",
            "minertoggleplus.toggle.auto_smelt"
    ),
    AUTO_REPLANT(
            "auto_replant",
            "§2Auto Replant",
            "Crops are automatically replanted when harvested.",
            "minertoggleplus.toggle.auto_replant"
    ),
    SILK_SPAWNERS(
            "silk_spawners",
            "§5Silk Spawners",
            "Mine spawners using a Silk Touch pickaxe.",
            "minertoggleplus.toggle.silk_spawners"
    ),
    DOUBLE_DROPS(
            "double_drops",
            "§eDouble Drops",
            "50% chance to receive double drops from ores.",
            "minertoggleplus.toggle.double_drops"
    ),
    AUTO_PICKUP(
            "auto_pickup",
            "§bAuto Pickup",
            "Mined items are instantly added to your inventory.",
            "minertoggleplus.toggle.auto_pickup"
    ),
    CAVE_VISION(
            "cave_vision",
            "§3Cave Vision",
            "Grants Night Vision when underground (below Y=64).",
            "minertoggleplus.toggle.cave_vision"
    ),
    AUTO_TORCH(
            "auto_torch",
            "§6Auto Torch",
            "Automatically places a torch when mining in darkness.",
            "minertoggleplus.toggle.auto_torch"
    ),
    NO_FALL_DAMAGE(
            "no_fall_damage",
            "§cNo Fall Damage",
            "Disables fall damage entirely.",
            "minertoggleplus.toggle.no_fall_damage"
    ),
    ORE_RADAR(
            "ore_radar",
            "§dOre Radar",
            "Notifies you of nearby rare ores via actionbar.",
            "minertoggleplus.toggle.ore_radar"
    ),
    XP_BOOST(
            "xp_boost",
            "§aXP Boost",
            "Receive bonus experience when mining ores.",
            "minertoggleplus.toggle.xp_boost"
    );

    private final String key;
    private final String displayName;
    private final String description;
    private final String permission;

    Toggle(String key, String displayName, String description, String permission) {
        this.key = key;
        this.displayName = displayName;
        this.description = description;
        this.permission = permission;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getPermission() {
        return permission;
    }

    public static Toggle fromKey(String key) {
        for (Toggle t : values()) {
            if (t.key.equalsIgnoreCase(key)) return t;
        }
        return null;
    }
}
