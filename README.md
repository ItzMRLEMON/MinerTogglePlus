# MinerToggle+

> A feature-rich, per-player toggle utility plugin for Paper 1.21.1

MinerToggle+ lets players enable or disable quality-of-life mining and survival features individually — no more fighting over server-wide settings. Admins get global controls for ClearLagg, mob drops, leaf decay, and more.

---

## Features

### Player Toggles

> All toggles are **OFF by default**. Players enable only what they want with `/mt toggle <feature>`.

| Feature | Command Key | Description |
|---|---|---|
| ⛏️ Vein Miner | `vein_miner` | Break an entire ore vein at once (sneak to activate) |
| 🌲 Tree Capitator | `tree_capitator` | Chop a whole tree by breaking one log |
| 🔥 Auto Smelt | `auto_smelt` | Ores smelt instantly on break — Fortune compatible |
| 🌾 Auto Replant | `auto_replant` | Fully-grown crops replant themselves automatically |
| 🏮 Silk Spawners | `silk_spawners` | Collect spawners with a Silk Touch pickaxe |
| 💎 Double Drops | `double_drops` | 50% chance to receive double drops from ores |
| 🧲 Auto Pickup | `auto_pickup` | Mined drops go straight to your inventory |
| 👁️ Cave Vision | `cave_vision` | Silent Night Vision when underground (below Y=64) |
| 🕯️ Auto Torch | `auto_torch` | Automatically places torches in dark caves |
| 🪂 No Fall Damage | `no_fall_damage` | Disables all fall damage |
| 📡 Ore Radar | `ore_radar` | Actionbar alerts for nearby rare ores |
| ✨ XP Boost | `xp_boost` | Bonus XP multiplier when mining ores |

### Global Admin Controls
| Toggle | Description |
|---|---|
| ClearLagg | Timed entity/item clearer with broadcast countdown |
| Mob Drops | Enable or disable all mob loot globally |
| Leaf Decay | Enable or disable natural leaf decay |

---

## Commands

```
/mt toggle <feature>        Toggle a personal feature on/off
/mt status                  View all your current toggle states
/mt help                    List all features with descriptions

/mt admin clearlagg         Toggle the ClearLagg system on/off
/mt admin clearlagg <sec>   Set the ClearLagg interval (seconds)
/mt admin mobdrops          Toggle global mob drops
/mt admin leafdecay         Toggle global leaf decay
/mt admin reload            Reload config.yml
```

Aliases: `/mt`, `/mtp`

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `minertoggleplus.use` | op | Access to all standard player toggles |
| `minertoggleplus.toggle.vein_miner` | op | Use Vein Miner |
| `minertoggleplus.toggle.tree_capitator` | op | Use Tree Capitator |
| `minertoggleplus.toggle.auto_smelt` | op | Use Auto Smelt |
| `minertoggleplus.toggle.auto_replant` | op | Use Auto Replant |
| `minertoggleplus.toggle.silk_spawners` | op | Use Silk Spawners |
| `minertoggleplus.toggle.double_drops` | op | Use Double Drops |
| `minertoggleplus.toggle.auto_pickup` | op | Use Auto Pickup |
| `minertoggleplus.toggle.cave_vision` | op | Use Cave Vision |
| `minertoggleplus.toggle.auto_torch` | op | Use Auto Torch |
| `minertoggleplus.toggle.no_fall_damage` | op | Use No Fall Damage |
| `minertoggleplus.toggle.ore_radar` | op | Use Ore Radar |
| `minertoggleplus.toggle.xp_boost` | op | Use XP Boost |
| `minertoggleplus.admin` | op | Access all admin commands |

---

## Auto Updater

MinerToggle+ automatically checks [ItzMRLEMON/MinerTogglePlus](https://github.com/ItzMRLEMON/MinerTogglePlus) for new releases on startup and every 60 minutes. No configuration needed — it's always on.

- If a newer release is found, the new `.jar` is **automatically downloaded** to `plugins/update/MinerTogglePlus.jar`
- Paper will swap it in on the next server restart
- Admins with `minertoggleplus.admin` receive an in-game notification on join
- If the download fails, a fallback notice is printed to console with the release URL

---

1. Download the latest release jar from [Releases](../../releases)
2. Drop it into your server's `/plugins/` folder
3. Restart or reload your server
4. Edit `plugins/MinerTogglePlus/config.yml` to your liking
5. Run `/mt admin reload` to apply config changes live

**Requirements:**
- Java 21+
- Paper 1.21.1+

---

## Building from Source

```bash
git clone https://github.com/ItzR_L3MON/MinerTogglePlus.git
cd MinerTogglePlus
mvn clean package
```

The compiled jar will be at `target/MinerTogglePlus.jar`.

---

## Configuration

Key settings in `config.yml`:

```yaml
clearlagg:
  enabled: true
  interval: 300          # seconds between clears
  warnings: [60,30,10,5,3,2,1]
  clear-items: true
  clear-hostiles: true
  clear-passives: false
  keep-named: true

vein-miner:
  require-sneak: true
  max-blocks: 64
  damage-tool: true

tree-capitator:
  max-logs: 200
  break-leaves: true
  damage-tool: true

auto-smelt:
  give-smelt-xp: true

double-drops:
  chance: 0.5            # 0.0 – 1.0

ore-radar:
  radius: 16
  scan-interval: 100     # ticks
  detect-ores:
    - DIAMOND_ORE
    - DEEPSLATE_DIAMOND_ORE
    - EMERALD_ORE
    - ANCIENT_DEBRIS

xp-boost:
  multiplier: 1.5
```

---

## Project Structure

```
MinerTogglePlus/
├── src/main/java/com/minertoggleplus/
│   ├── MinerTogglePlus.java          # Main plugin class
│   ├── Toggle.java                   # Toggle enum (all features)
│   ├── PlayerToggleData.java         # Per-player toggle state
│   ├── GlobalSettings.java           # Admin-controlled globals
│   ├── commands/
│   │   └── MinerToggleCommand.java   # /mt command + tab complete
│   ├── listeners/
│   │   ├── BlockBreakListener.java   # VeinMiner, TreeCap, AutoSmelt, etc.
│   │   ├── CropListener.java         # AutoReplant
│   │   └── PlayerListener.java       # FallDmg, MobDrops, LeafDecay
│   ├── managers/
│   │   ├── ClearLaggManager.java     # Timed entity clear system
│   │   ├── CaveVisionManager.java    # Underground Night Vision
│   │   └── OreRadarManager.java      # Nearby ore scanner
│   └── util/
│       ├── ItemUtils.java            # Smelt maps, drop helpers, etc.
│       └── UpdateChecker.java        # GitHub Releases auto-updater
└── src/main/resources/
    ├── plugin.yml
    └── config.yml
```

---

## License

MIT — see [LICENSE](LICENSE)
