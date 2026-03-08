package com.minertoggleplus.util;

import com.minertoggleplus.MinerTogglePlus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automatically checks GitHub Releases for MinerToggle+ updates and downloads them.
 * Always enabled — no config required.
 */
public class UpdateChecker implements Listener {

    private static final String GITHUB_REPO = "ItzMRLEMON/MinerTogglePlus";
    private static final String API_URL     = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private static final String RELEASES_URL = "https://github.com/" + GITHUB_REPO + "/releases/latest";

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&',
            "&8[&6MinerToggle+&8] &r");

    // Re-check every 60 minutes
    private static final long CHECK_INTERVAL_TICKS = 60L * 60 * 20;

    private final MinerTogglePlus plugin;
    private String  latestVersion   = null;
    private String  downloadUrl     = null;
    private boolean updateAvailable = false;

    public UpdateChecker(MinerTogglePlus plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void start() {
        // Notify ops on join
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Check on startup, then every 60 minutes
        checkAsync();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkAsync,
                CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    public boolean isUpdateAvailable() { return updateAvailable; }
    public String  getLatestVersion()  { return latestVersion; }

    // ── Core check ────────────────────────────────────────────────────────────

    private void checkAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                fetchLatestRelease();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "[UpdateChecker] Could not reach GitHub: " + e.getMessage());
            }
        });
    }

    private void fetchLatestRelease() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "MinerTogglePlus-UpdateChecker");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int status = conn.getResponseCode();
        if (status != 200) {
            plugin.getLogger().warning("[UpdateChecker] GitHub API returned HTTP " + status);
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        String json    = sb.toString();
        String tagName = extractJsonString(json, "tag_name");
        String jarUrl  = extractBrowserDownloadUrl(json);

        if (tagName == null) return;

        latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
        downloadUrl   = jarUrl;

        String current = plugin.getDescription().getVersion();
        updateAvailable = isNewerVersion(latestVersion, current);

        if (updateAvailable) {
            plugin.getLogger().info("[UpdateChecker] Update found: v" + current + " → v" + latestVersion);
            if (downloadUrl != null) {
                downloadUpdate();
            } else {
                Bukkit.getScheduler().runTask(plugin, this::notifyConsole);
            }
        } else {
            plugin.getLogger().info("[UpdateChecker] Up to date (v" + current + ").");
        }
    }

    // ── Auto-download ─────────────────────────────────────────────────────────

    private void downloadUpdate() {
        try {
            Path updateDir = plugin.getDataFolder().toPath().getParent().resolve("update");
            Files.createDirectories(updateDir);
            Path dest = updateDir.resolve("MinerTogglePlus.jar");

            plugin.getLogger().info("[UpdateChecker] Downloading v" + latestVersion + "...");

            HttpURLConnection conn = (HttpURLConnection) URI.create(downloadUrl).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "MinerTogglePlus-UpdateChecker");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }

            plugin.getLogger().info("[UpdateChecker] v" + latestVersion
                    + " downloaded to plugins/update/ — restart to apply.");

            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.broadcastMessage(PREFIX + ChatColor.GREEN
                            + "MinerToggle+ v" + latestVersion
                            + " downloaded! Restart the server to apply the update."));

        } catch (Exception e) {
            plugin.getLogger().warning("[UpdateChecker] Auto-download failed: " + e.getMessage());
            Bukkit.getScheduler().runTask(plugin, this::notifyConsole);
        }
    }

    // ── Console banner ────────────────────────────────────────────────────────

    private void notifyConsole() {
        plugin.getLogger().info("┌─────────────────────────────────────────┐");
        plugin.getLogger().info("│   MinerToggle+ UPDATE AVAILABLE         │");
        plugin.getLogger().info("│   Current : v" + plugin.getDescription().getVersion());
        plugin.getLogger().info("│   Latest  : v" + latestVersion);
        plugin.getLogger().info("│   " + RELEASES_URL);
        plugin.getLogger().info("└─────────────────────────────────────────┘");
    }

    // ── Join notification ─────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!updateAvailable) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("minertoggleplus.admin")) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "⚡ Update available: "
                    + ChatColor.WHITE + "v" + plugin.getDescription().getVersion()
                    + ChatColor.GRAY + " → "
                    + ChatColor.GREEN + "v" + latestVersion);
            player.sendMessage(PREFIX + ChatColor.GRAY + "Downloading automatically on next restart.");
        }, 20L);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Simple semver comparison. Returns true if latest > current. */
    private boolean isNewerVersion(String latest, String current) {
        try {
            int[] l = parseSemver(latest);
            int[] c = parseSemver(current);
            for (int i = 0; i < 3; i++) {
                if (l[i] > c[i]) return true;
                if (l[i] < c[i]) return false;
            }
            return false;
        } catch (Exception e) {
            return !latest.equals(current);
        }
    }

    private int[] parseSemver(String v) {
        String[] parts = v.split("\\.");
        int[] nums = new int[3];
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            nums[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
        }
        return nums;
    }

    /** Extracts a simple top-level string field from a JSON response. */
    private String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** Extracts the first .jar browser_download_url from a release asset list. */
    private String extractBrowserDownloadUrl(String json) {
        Pattern p = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
