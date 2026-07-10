package net.tfminecraft.thievery.loader;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.cache.Parameters;

public class ConfigLoader {
    public void loadConfig(File configFile) {
        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            return;
        }

        KeyLoader.load(config);
        LockpickLoader.load(config);
        MaskLoader.load(config);

        Cache.cooldown = config.getInt("cooldown", 3);
        Cache.radius = config.getInt("lockpick-range", config.getInt("radius", 4));
        Cache.categoryPoints = config.getInt("category_points", 30);
        Cache.pointGainIntervalHours = config.getInt("point_gain_interval", 24);
        Cache.defaultValue = config.getDouble("default_value", 0.1);

        if (config.contains("traits")) Cache.traits = config.getStringList("traits");

        Cache.recentClueMax = config.getInt("clues.recent-max", 6);
        Cache.recentClueCooldownHours = config.getInt("clues.recent-cooldown-hours", 72);
        Cache.criticalCooldownHours = config.getInt("clues.critical-cooldown-hours", 24);

        double legacyRiskMin = config.getDouble("clues.risk-gain-min", 0.05);
        double legacyRiskMax = config.getDouble("clues.risk-gain-max", 0.15);
        Cache.riskGainDoorMin = config.getDouble("clues.risk-gain-door.min", legacyRiskMin);
        Cache.riskGainDoorMax = config.getDouble("clues.risk-gain-door.max", legacyRiskMax);
        if (config.contains("clues.risk-gain-chest.min") || config.contains("clues.risk-gain-chest.max")) {
            Cache.riskGainChestMin = config.getDouble("clues.risk-gain-chest.min", legacyRiskMin * 0.5);
            Cache.riskGainChestMax = config.getDouble("clues.risk-gain-chest.max", legacyRiskMax * 0.5);
        } else {
            Cache.riskGainChestMin = Cache.riskGainDoorMin * 0.5;
            Cache.riskGainChestMax = Cache.riskGainDoorMax * 0.5;
        }

        Cache.riskPickReduction = config.getDouble("clues.risk-pick-reduction", 0.5);
        Cache.riskDecayPerHour = config.getDouble("clues.risk-decay-per-hour", 0.08);
        Cache.criticalBase = config.getDouble("clues.critical-base", 0.0);
        Cache.criticalRiskWeight = config.getDouble("clues.critical-risk-weight", 0.5);
        Cache.criticalDexReduction = config.getDouble("clues.critical-dex-reduction", 0.15);
        Cache.criticalStrengthReduction = config.getDouble("clues.critical-strength-reduction", 0.2);
        Cache.criticalClue = config.getString("clues.critical-clue",
                "§7This seems to be the work of {character_name}");

        Parameters.chestBaseChance = config.getDouble("lockpicking.chest.base-chance", 0.5);
        Parameters.doorMaxDistance = config.getDouble("lockpicking.door-max-distance", 3.0);
        Parameters.lockpickMaxReduction = config.getDouble("lockpicking.lockpick-max-reduction", 0.5);
        Parameters.lockpickFailCooldownMs = config.getLong("lockpicking.fail-cooldown-ms", 60_000L);
        Parameters.doorUnlockWindowMs = config.getLong("lockpicking.door-unlock-window-minutes", 60L)
                * 60L * 1000L;

        Parameters.barLength = config.getInt("lockpicking.bar.length", 20);
        Parameters.maxSuccessSlots = config.getInt("lockpicking.bar.max-success-slots", 3);
        Parameters.minBreakSlots = config.getInt("lockpicking.bar.min-break-slots", 3);
        Parameters.maxBreakSlots = config.getInt("lockpicking.bar.max-break-slots", 19);
        Parameters.baseBarSpeed = config.getDouble("lockpicking.bar.base-speed", 2.5);
        Parameters.dexSpeedReductionPerLevel = config.getDouble("lockpicking.bar.dex-speed-reduction-per-level", 0.02);
        Parameters.minBarSpeed = config.getDouble("lockpicking.bar.min-speed", 0.4);
        Parameters.speedJitterFraction = config.getDouble("lockpicking.bar.speed-jitter-fraction", 0.3);
        Parameters.randomFlipChance = config.getDouble("lockpicking.bar.random-flip-chance", 0.03);
        Parameters.lockpickAttribute = config.getString("lockpicking.attribute", "dexterity");
    }
}
