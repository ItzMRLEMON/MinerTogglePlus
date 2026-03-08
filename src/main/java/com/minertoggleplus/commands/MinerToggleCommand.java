package com.minertoggleplus.commands;

import com.minertoggleplus.MinerTogglePlus;
import com.minertoggleplus.PlayerToggleData;
import com.minertoggleplus.Toggle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /minertoggle (aliases: /mt, /mtp)
 *
 * Subcommands:
 *   toggle <feature>         — toggle a personal feature on/off
 *   status                   — list your active toggles
 *   help                     — list all features with descriptions
 *   admin clearlagg          — toggle ClearLagg system (global)
 *   admin clearlagg <sec>    — set ClearLagg interval
 *   admin mobdrops           — toggle global mob drops
 *   admin leafdecay          — toggle global leaf decay
 *   admin reload             — reload config
 */
public class MinerToggleCommand implements CommandExecutor, TabCompleter {

    private final MinerTogglePlus plugin;

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&',
            "&8[&6MinerToggle+&8] &r");

    public MinerToggleCommand(MinerTogglePlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "help" -> sendHelp(sender);

            case "status" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(PREFIX + "§cOnly players can view their status.");
                    return true;
                }
                sendStatus(player);
            }

            case "toggle" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(PREFIX + "§cOnly players can use toggle.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(PREFIX + "§cUsage: /mt toggle <feature>");
                    return true;
                }
                handlePlayerToggle(player, args[1]);
            }

            case "admin" -> {
                if (!sender.hasPermission("minertoggleplus.admin")) {
                    sender.sendMessage(PREFIX + "§cYou don't have permission for admin commands.");
                    return true;
                }
                if (args.length < 2) {
                    sendAdminHelp(sender);
                    return true;
                }
                handleAdminCommand(sender, args);
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PLAYER TOGGLE
    // ─────────────────────────────────────────────────────────────────────────

    private void handlePlayerToggle(Player player, String key) {
        Toggle toggle = Toggle.fromKey(key);
        if (toggle == null) {
            player.sendMessage(PREFIX + "§cUnknown feature: §e" + key
                    + "§c. Use §e/mt help §cfor a list.");
            return;
        }
        if (!player.hasPermission(toggle.getPermission())) {
            player.sendMessage(PREFIX + color(plugin.getConfig().getString(
                    "messages.no-permission", "&cYou don't have permission to use this toggle.")));
            return;
        }

        PlayerToggleData data = plugin.getPlayerData(player.getUniqueId());
        boolean newState = data.toggle(toggle);

        String template = newState
                ? plugin.getConfig().getString("messages.toggle-on", "&a%feature% &7has been &aenabled&7.")
                : plugin.getConfig().getString("messages.toggle-off", "&c%feature% &7has been &cdisabled&7.");

        player.sendMessage(PREFIX + color(template.replace("%feature%", toggle.getDisplayName())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ADMIN COMMANDS
    // ─────────────────────────────────────────────────────────────────────────

    private void handleAdminCommand(CommandSender sender, String[] args) {
        String sub = args[1].toLowerCase();

        switch (sub) {
            case "clearlagg" -> {
                if (args.length >= 3) {
                    // Try to set interval
                    try {
                        int seconds = Integer.parseInt(args[2]);
                        if (seconds < 10) {
                            sender.sendMessage(PREFIX + "§cInterval must be at least 10 seconds.");
                            return;
                        }
                        plugin.getGlobalSettings().setClearLaggInterval(seconds);
                        plugin.getClearLaggManager().restart();
                        sender.sendMessage(PREFIX + "§aClearLagg interval set to §e" + seconds + "§a seconds.");
                    } catch (NumberFormatException e) {
                        sender.sendMessage(PREFIX + "§cInvalid number: §e" + args[2]);
                    }
                } else {
                    boolean newState = !plugin.getGlobalSettings().isClearLaggEnabled();
                    plugin.getGlobalSettings().setClearLaggEnabled(newState);
                    if (newState) {
                        plugin.getClearLaggManager().restart();
                    } else {
                        plugin.getClearLaggManager().stop();
                    }
                    broadcastAdminToggle(sender, "ClearLagg", newState);
                }
            }
            case "mobdrops" -> {
                boolean newState = !plugin.getGlobalSettings().isMobDropsEnabled();
                plugin.getGlobalSettings().setMobDropsEnabled(newState);
                broadcastAdminToggle(sender, "Mob Drops", newState);
            }
            case "leafdecay" -> {
                boolean newState = !plugin.getGlobalSettings().isLeafDecayEnabled();
                plugin.getGlobalSettings().setLeafDecayEnabled(newState);
                broadcastAdminToggle(sender, "Leaf Decay", newState);
            }
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getGlobalSettings().reload();
                plugin.getClearLaggManager().restart();
                sender.sendMessage(PREFIX + "§aConfiguration reloaded.");
            }
            default -> sendAdminHelp(sender);
        }
    }

    private void broadcastAdminToggle(CommandSender sender, String feature, boolean enabled) {
        String playerName = sender instanceof Player p ? p.getName() : "Console";
        String template = enabled
                ? plugin.getConfig().getString("messages.admin-toggle-on", "&a[GLOBAL] %feature% &7enabled by &a%player%&7.")
                : plugin.getConfig().getString("messages.admin-toggle-off", "&c[GLOBAL] %feature% &7disabled by &c%player%&7.");
        String msg = PREFIX + color(template.replace("%feature%", feature).replace("%player%", playerName));
        Bukkit.broadcastMessage(msg);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STATUS
    // ─────────────────────────────────────────────────────────────────────────

    private void sendStatus(Player player) {
        PlayerToggleData data = plugin.getPlayerData(player.getUniqueId());
        player.sendMessage(PREFIX + "§7Your active toggles:");

        for (Toggle toggle : Toggle.values()) {
            if (!player.hasPermission(toggle.getPermission())) continue;
            boolean enabled = data.isEnabled(toggle);
            String state = enabled ? "§a✔ ON" : "§c✘ OFF";
            player.sendMessage("  §7" + toggle.getDisplayName() + "§8: " + state);
        }

        player.sendMessage("§8─────────────────────────");
        player.sendMessage("§7Global: ClearLagg " + stateTag(plugin.getGlobalSettings().isClearLaggEnabled())
                + "  §7MobDrops " + stateTag(plugin.getGlobalSettings().isMobDropsEnabled())
                + "  §7LeafDecay " + stateTag(plugin.getGlobalSettings().isLeafDecayEnabled()));
    }

    private String stateTag(boolean on) {
        return on ? "§a[ON]" : "§c[OFF]";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELP
    // ─────────────────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8═══════ §6MinerToggle+ §8═══════");
        sender.sendMessage("§e/mt toggle <feature> §7— Toggle a personal feature");
        sender.sendMessage("§e/mt status §7— View your active toggles");
        sender.sendMessage("§e/mt help §7— Show this help menu");
        if (sender.hasPermission("minertoggleplus.admin")) {
            sender.sendMessage("§e/mt admin §7— Admin commands");
        }
        sender.sendMessage("§8─── §7Features ───");
        for (Toggle t : Toggle.values()) {
            if (sender instanceof Player p && !p.hasPermission(t.getPermission())) continue;
            sender.sendMessage("  §e" + t.getKey() + " §8— §7" + t.getDescription());
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§8═══ §6Admin Commands §8═══");
        sender.sendMessage("§e/mt admin clearlagg §7— Toggle ClearLagg on/off");
        sender.sendMessage("§e/mt admin clearlagg <sec> §7— Set ClearLagg interval");
        sender.sendMessage("§e/mt admin mobdrops §7— Toggle global mob drops");
        sender.sendMessage("§e/mt admin leafdecay §7— Toggle global leaf decay");
        sender.sendMessage("§e/mt admin reload §7— Reload configuration");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TAB COMPLETION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("toggle", "status", "help", "admin"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "toggle" -> filter(
                        Arrays.stream(Toggle.values()).map(Toggle::getKey).collect(Collectors.toList()),
                        args[1]);
                case "admin" -> sender.hasPermission("minertoggleplus.admin")
                        ? filter(List.of("clearlagg", "mobdrops", "leafdecay", "reload"), args[1])
                        : List.of();
                default -> List.of();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("clearlagg")) {
            return filter(List.of("60", "120", "180", "300", "600"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
