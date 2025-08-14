package net.tfminecraft.thievery.loader;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.thievery.cache.Cache;

public class ConfigLoader {
    public void loadConfig(File configFile) {
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        Cache.minSuccess = config.getDouble("lockpicking.success-rate.min", 0.4);
        Cache.maxSuccess = config.getDouble("lockpicking.success-rate.max", 0.9);

        Cache.minBreak = config.getDouble("lockpicking.break-chance.min", 0.01);
        Cache.maxBreak = config.getDouble("lockpicking.break-chance.max", 0.15);

        Cache.cooldown = config.getInt("cooldown", 3)-1;

        Cache.lockpick = config.getString("lockpick", "v.tripwire_hook");
        Cache.radius = config.getInt("radius", 4);

        if(config.contains("traits")) Cache.traits = config.getStringList("traits");
	}
}
