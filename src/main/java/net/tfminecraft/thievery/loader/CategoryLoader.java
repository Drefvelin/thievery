package net.tfminecraft.thievery.loader;



import java.io.File;

import java.io.IOException;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

import java.util.Set;



import org.bukkit.configuration.InvalidConfigurationException;

import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.configuration.file.YamlConfiguration;



import me.Plugins.TLibs.Interface.LoaderInterface;

import net.tfminecraft.thievery.Thievery;

import net.tfminecraft.thievery.data.ItemCategory;

import net.tfminecraft.thievery.data.ItemCategory.CategoryItemEntry;



public class CategoryLoader implements LoaderInterface {



    private static final HashMap<String, ItemCategory> categories = new HashMap<>();



    public static HashMap<String, ItemCategory> get() {

        return categories;

    }



    public static List<ItemCategory> getAsList() {

        return new ArrayList<>(categories.values());

    }



    public static ItemCategory getById(String id) {

        return categories.get(id);

    }



    @Override

    public void load(File configFile) {

        categories.clear();



        FileConfiguration config = new YamlConfiguration();

        try {

            config.load(configFile);

        } catch (IOException | InvalidConfigurationException e) {

            e.printStackTrace();

            return;

        }



        Map<String, String> pathToCategory = new HashMap<>();

        Set<String> keys = config.getKeys(false);

        for (String key : keys) {

            ItemCategory category = new ItemCategory(key, config.getConfigurationSection(key));

            for (CategoryItemEntry entry : category.getItems()) {

                String path = entry.getPath();

                String existing = pathToCategory.put(path, key);

                if (existing != null && !existing.equals(key)) {

                    Thievery.getInstance().getLogger().severe(

                            "[Thievery] Item path '" + path + "' is defined in both '"

                                    + existing + "' and '" + key + "'");

                }

            }

            categories.put(key, category);

        }

    }

}

