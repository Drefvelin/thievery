package net.tfminecraft.thievery;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.command.CommandManager;
import net.tfminecraft.thievery.loader.ConfigLoader;
import net.tfminecraft.thievery.manager.ContainerManager;

public class Thievery extends JavaPlugin {

    private static Thievery instance;
    private ContainerManager containerManager;
    private final ConfigLoader configLoader = new ConfigLoader();

    @Override
    public void onEnable() {
        instance = this;
        createConfigs();
        loadConfigs();
        setPlugins();
        containerManager = new ContainerManager();
        getCommand("thievery").setExecutor(new CommandManager(containerManager));
        getCommand("thievery").setTabCompleter(new CommandManager(containerManager));

        // Register events
        getServer().getPluginManager().registerEvents(containerManager, this);
        // Enable feedback for all currently online admins
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("thievery.admin")) {
                containerManager.enableFeedback(player);
            }
        });

        getLogger().info("Thievery Plugin Enabled!");
        }

    @Override
    public void onDisable() {

    }

    public static Thievery getInstance() {
        return instance;
    }

    public ContainerManager getContainerManager() {
        return containerManager;
    }

    public void createConfigs() {
		String[] files = {
				"config.yml"
				};
		for(String s : files) {
			File newConfigFile = new File(getDataFolder(), s);
	        if (!newConfigFile.exists()) {
	        	newConfigFile.getParentFile().mkdirs();
	            saveResource(s, false);
	        }
		}
	}

    public void loadConfigs() {
		configLoader.loadConfig(new File(getDataFolder(), "config.yml"));
	}

    public void setPlugins() {
		Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");

		if (plugin != null && plugin.isEnabled() && plugin instanceof CoreProtect) {
			Cache.coreProtect = true;
		}
	}

    public static CoreProtectAPI getCoreProtect() {
        Plugin coreProtect = getInstance().getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (coreProtect == null || !(coreProtect instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) coreProtect).getAPI();
        if (CoreProtect.isEnabled() == false) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 10) {
            return null;
        }

        return CoreProtect;
	}
}
