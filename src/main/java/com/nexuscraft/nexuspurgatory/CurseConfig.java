package com.nexuscraft.nexuspurgatory;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Parses config.yml into typed fields -- who's targeted, and every severity/pacing knob for
 *  what happens to them. A malformed {@code targets} entry is skipped individually (with a
 *  warning), the same defensive stance {@code PrisonConfig}'s own {@code prisoners} parsing takes
 *  in NexusPrison -- one typo'd line shouldn't cost every other configured target. */
public final class CurseConfig {

    private final Plugin plugin;

    public List<CurseTargetConfig> targets = new ArrayList<>();

    public double capHealthPoints = 2.0; // one heart

    public int hungerDrainIntervalSeconds = 45;
    public int hungerDrainToLevel = 1;

    public double mobDamageLethalityChance = 1.0;

    public int slownessAmplifier = 0; // Slowness I
    public int slownessRefreshIntervalSeconds = 5;

    public int inventoryJumbleIntervalSeconds = 45;

    public int itemDrainIntervalSeconds = 300; // five minutes
    public String itemDrainTauntMessage = "&5&lYour %item% &5&lcrumbles to dust in your hands.";

    public boolean cursePulseEnabled = true;
    public int cursePulseIntervalSeconds = 90;
    public int cursePulseDurationSeconds = 5;

    public boolean escalationEnabled = true;
    public List<Long> escalationStageThresholdSeconds = List.of(5L * 60, 15L * 60, 30L * 60);
    public int escalationPulseIntervalReductionPerStageSeconds = 15;
    public int escalationMinPulseIntervalSeconds = 20;

    public boolean bossBarEnabled = true;
    public String bossBarColor = "RED";
    public String bossBarStyle = "SEGMENTED_10";

    public CurseConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration c = plugin.getConfig();
        Logger logger = plugin.getLogger();

        capHealthPoints = Math.max(0.5, c.getDouble("health.capHealthPoints", capHealthPoints));

        ConfigurationSection hunger = c.getConfigurationSection("hunger");
        if (hunger != null) {
            hungerDrainIntervalSeconds = Math.max(1, hunger.getInt("drainIntervalSeconds", hungerDrainIntervalSeconds));
            hungerDrainToLevel = Math.max(0, hunger.getInt("drainToLevel", hungerDrainToLevel));
        }

        mobDamageLethalityChance = clamp01(c.getDouble("mobDamage.lethalityChance", mobDamageLethalityChance));

        ConfigurationSection slowness = c.getConfigurationSection("slowness");
        if (slowness != null) {
            slownessAmplifier = Math.max(0, slowness.getInt("amplifier", slownessAmplifier));
            slownessRefreshIntervalSeconds = Math.max(1, slowness.getInt("refreshIntervalSeconds", slownessRefreshIntervalSeconds));
        }

        inventoryJumbleIntervalSeconds = Math.max(1, c.getInt("inventory.jumbleIntervalSeconds", inventoryJumbleIntervalSeconds));

        ConfigurationSection itemDrain = c.getConfigurationSection("itemDrain");
        if (itemDrain != null) {
            itemDrainIntervalSeconds = Math.max(1, itemDrain.getInt("intervalSeconds", itemDrainIntervalSeconds));
            itemDrainTauntMessage = itemDrain.getString("tauntMessage", itemDrainTauntMessage);
        }

        ConfigurationSection cursePulse = c.getConfigurationSection("cursePulse");
        if (cursePulse != null) {
            cursePulseEnabled = cursePulse.getBoolean("enabled", cursePulseEnabled);
            cursePulseIntervalSeconds = Math.max(1, cursePulse.getInt("intervalSeconds", cursePulseIntervalSeconds));
            cursePulseDurationSeconds = Math.max(1, cursePulse.getInt("durationSeconds", cursePulseDurationSeconds));
        }

        ConfigurationSection escalation = c.getConfigurationSection("escalation");
        if (escalation != null) {
            escalationEnabled = escalation.getBoolean("enabled", escalationEnabled);
            List<Integer> minutes = escalation.getIntegerList("stageThresholdMinutes");
            if (minutes != null && !minutes.isEmpty()) {
                List<Long> seconds = new ArrayList<>();
                for (int m : minutes) {
                    seconds.add(m * 60L);
                }
                escalationStageThresholdSeconds = seconds;
            }
            escalationPulseIntervalReductionPerStageSeconds = Math.max(0,
                    escalation.getInt("pulseIntervalReductionPerStageSeconds", escalationPulseIntervalReductionPerStageSeconds));
            escalationMinPulseIntervalSeconds = Math.max(1,
                    escalation.getInt("minPulseIntervalSeconds", escalationMinPulseIntervalSeconds));
        }

        ConfigurationSection bossBar = c.getConfigurationSection("bossBar");
        if (bossBar != null) {
            bossBarEnabled = bossBar.getBoolean("enabled", bossBarEnabled);
            bossBarColor = bossBar.getString("color", bossBarColor);
            bossBarStyle = bossBar.getString("style", bossBarStyle);
        }

        targets = new ArrayList<>();
        for (Map<?, ?> entry : c.getMapList("targets")) {
            try {
                Object javaUsername = entry.get("javaUsername");
                Object xboxGamertag = entry.get("xboxGamertag");
                targets.add(new CurseTargetConfig(
                        javaUsername != null ? javaUsername.toString() : null,
                        xboxGamertag != null ? xboxGamertag.toString() : null));
            } catch (RuntimeException malformed) {
                logger.warning("[NexusPurgatory] Skipping a malformed 'targets' entry in config.yml: " + malformed.getMessage());
            }
        }
    }

    public BarColor resolveBossBarColor() {
        try {
            return BarColor.valueOf(bossBarColor.trim().toUpperCase());
        } catch (RuntimeException invalid) {
            return BarColor.RED;
        }
    }

    public BarStyle resolveBossBarStyle() {
        try {
            return BarStyle.valueOf(bossBarStyle.trim().toUpperCase());
        } catch (RuntimeException invalid) {
            return BarStyle.SEGMENTED_10;
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
