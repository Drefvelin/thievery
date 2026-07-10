package net.tfminecraft.thievery;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.command.CommandManager;
import net.tfminecraft.thievery.loader.CategoryLoader;
import net.tfminecraft.thievery.loader.ConfigLoader;
import net.tfminecraft.thievery.manager.ContainerManager;
import net.tfminecraft.thievery.manager.DoorManager;
import net.tfminecraft.thievery.manager.InventoryManager;
import net.tfminecraft.thievery.manager.MaskChatListener;
import net.tfminecraft.thievery.manager.MaskProfileBlockListener;
import net.tfminecraft.thievery.manager.PlayerManager;

public class Thievery extends JavaPlugin {

    private static Thievery instance;
    private ContainerManager containerManager;
    private DoorManager doorManager;
    private final ConfigLoader configLoader = new ConfigLoader();
    private final CategoryLoader categoryLoader = new CategoryLoader();
    private final InventoryManager inventoryManager = new InventoryManager();
    private static final PlayerManager playerManager = new PlayerManager();

    @Override
    public void onEnable() {
        instance = this;
        createConfigs();
        loadConfigs();
        setPlugins();
        containerManager = new ContainerManager();
        doorManager = new DoorManager();

        getCommand("thievery").setExecutor(new CommandManager(containerManager, inventoryManager));
        getCommand("thievery").setTabCompleter(new CommandManager(containerManager, inventoryManager));

        getServer().getPluginManager().registerEvents(containerManager, this);
        getServer().getPluginManager().registerEvents(doorManager, this);
        getServer().getPluginManager().registerEvents(playerManager, this);
        getServer().getPluginManager().registerEvents(inventoryManager, this);
        getServer().getPluginManager().registerEvents(new MaskChatListener(), this);
        getServer().getPluginManager().registerEvents(new MaskProfileBlockListener(), this);

        playerManager.start();

        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("thievery.admin")) {
                containerManager.enableFeedback(player);
            }
        });

        getLogger().info("Thievery Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        playerManager.unloadAll();
        playerManager.stop();
    }

    public void loadPlayers() {
        playerManager.loadAll();
    }

    public void reload() {
        playerManager.unloadAll();
        loadConfigs();
        setPlugins();
        playerManager.start();
    }

    public void reloadMessage(Player player) {
        player.sendMessage(ChatColor.GREEN + "[Thievery]" + ChatColor.YELLOW + " Reloading plugin...");
        reload();
        player.sendMessage(ChatColor.GREEN + "[Thievery]" + ChatColor.YELLOW + " Reloading complete!");
    }

    public static Thievery getInstance() {
        return instance;
    }

    public ContainerManager getContainerManager() {
        return containerManager;
    }

    public static PlayerManager getPlayerManager() {
        return playerManager;
    }

    public void createConfigs() {
        String[] files = {
                "config.yml",
                "categories.yml"
        };
        for (String s : files) {
            File newConfigFile = new File(getDataFolder(), s);
            if (!newConfigFile.exists()) {
                newConfigFile.getParentFile().mkdirs();
                saveResource(s, false);
            }
        }
    }

    public void loadConfigs() {
        configLoader.loadConfig(new File(getDataFolder(), "config.yml"));
        categoryLoader.load(new File(getDataFolder(), "categories.yml"));
    }

    public void setPlugins() {
        Cache.coreProtect = false;
        Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");

        if (plugin != null && plugin.isEnabled() && plugin instanceof CoreProtect) {
            Cache.coreProtect = true;
        }
    }

    public static CoreProtectAPI getCoreProtect() {
        Plugin coreProtect = getInstance().getServer().getPluginManager().getPlugin("CoreProtect");

        if (coreProtect == null || !(coreProtect instanceof CoreProtect)) {
            return null;
        }

        CoreProtectAPI CoreProtect = ((CoreProtect) coreProtect).getAPI();
        if (CoreProtect.isEnabled() == false) {
            return null;
        }

        if (CoreProtect.APIVersion() < 10) {
            return null;
        }

        return CoreProtect;
    }
}
