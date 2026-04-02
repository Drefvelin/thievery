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
        }
        Cache.minSuccess = config.getDouble("lockpicking.chest.success-rate-min", 0.4);
        Cache.maxSuccess = config.getDouble("lockpicking.chest.success-rate-max", 0.9);

        Cache.minBreak = config.getDouble("lockpicking.chest.break-chance-min", 0.01);
        Cache.maxBreak = config.getDouble("lockpicking.chest.break-chance-max", 0.15);

        Parameters.chestStrengthSuccessBonus = config.getDouble("lockpicking.chest.strength-success-bonus", 0.2);
        Parameters.chestStrengthBreakReduction = config.getDouble("lockpicking.chest.strength-break-reduction", 0.08);

        Cache.cooldown = config.getInt("cooldown", 3)-1;

        Cache.radius = config.getInt("radius", 4);

        if(config.contains("traits")) Cache.traits = config.getStringList("traits");

        if(config.contains("key-items")) Cache.keyItems = config.getStringList("key-items");
        if(config.contains("lockpick-items")) Cache.lockPickItems = config.getStringList("lockpick-items");

        Parameters.defaultKeyStrength = config.getDouble("lockpicking.default-key-strength", 1.0);
        Parameters.defaultLockpickStrength = config.getDouble("lockpicking.default-lockpick-strength", 0.0);
        Parameters.lockpickMaxReduction = config.getDouble("lockpicking.lockpick-max-reduction", 0.5);
        Parameters.lockpickFailCooldownMs = config.getLong("lockpicking.fail-cooldown-ms", 60_000L);

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
