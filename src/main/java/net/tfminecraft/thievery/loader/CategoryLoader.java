package net.tfminecraft.thievery.loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.Interface.LoaderInterface;
import me.Plugins.TLibs.TLibs;
import net.tfminecraft.AdvancedCrafting.Utils.ThieveryBridge;
import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.data.AcCraftRef;
import net.tfminecraft.thievery.data.CategoryMatchType;
import net.tfminecraft.thievery.data.ItemCategory;
import net.tfminecraft.thievery.data.ItemCategory.CategoryItemEntry;

public class CategoryLoader implements LoaderInterface {

    private static final LinkedHashMap<String, ItemCategory> categories = new LinkedHashMap<>();

    public static Map<String, ItemCategory> get() {
        return categories;
    }

    public static List<ItemCategory> getAsList() {
        return List.copyOf(categories.values());
    }

    public static List<ItemCategory> getLoadoutCategories() {
        List<ItemCategory> loadout = new ArrayList<>();
        for (ItemCategory category : categories.values()) {
            if (category.isLoadoutVisible()) {
                loadout.add(category);
            }
        }
        return loadout;
    }

    public static ItemCategory getById(String id) {
        return categories.get(id);
    }

    public static double getWeightForPath(String path) {
        if (path == null || path.isBlank()) {
            return Cache.defaultItemValue;
        }
        for (ItemCategory category : getAsList()) {
            if (category.getMatch().getType() != CategoryMatchType.PATH) {
                continue;
            }
            for (CategoryItemEntry entry : category.getItems()) {
                if (entry.getPath().equalsIgnoreCase(path)) {
                    return entry.getWeight();
                }
            }
        }
        ItemStack probe = TLibs.getItemAPI().getCreator().getItemFromPath(path);
        if (probe != null) {
            for (ItemCategory category : getAsList()) {
                if (category.getMatch().getType() == CategoryMatchType.PATH
                        && category.matchesPath(probe)) {
                    return category.getPathWeightFor(probe);
                }
            }
        }
        return Cache.defaultItemValue;
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

        Set<String> keys = config.getKeys(false);
        for (String key : keys) {
            ItemCategory category = new ItemCategory(key, config.getConfigurationSection(key));
            if (category.getMatch().getType() == CategoryMatchType.PATH) {
                for (CategoryItemEntry entry : category.getItems()) {
                    warnDuplicatePath(entry.getPath(), key);
                }
            }
            categories.put(key, category);
        }

        validateComposites();
    }

    private static void validateComposites() {
        for (ItemCategory category : categories.values()) {
            if (category.getMatch().getType() != CategoryMatchType.COMPOSITE) {
                continue;
            }
            for (AcCraftRef ref : category.getAcCraftRefs()) {
                if (ThieveryBridge.isPluginReady()
                        && ThieveryBridge.getStatTemplate(ref.getStatTemplate()) == null) {
                    Thievery.getInstance().getLogger().warning(
                            "[Thievery] Composite '" + category.getId()
                                    + "' references unknown AC stat template '"
                                    + ref.getStatTemplate() + "' in '" + ref.getRawId() + "'");
                }
            }
        }
    }

    private static void warnDuplicatePath(String path, String categoryId) {
        for (ItemCategory existing : categories.values()) {
            if (existing.getMatch().getType() != CategoryMatchType.PATH) {
                continue;
            }
            for (CategoryItemEntry entry : existing.getItems()) {
                if (entry.getPath().equalsIgnoreCase(path)
                        && !existing.getId().equalsIgnoreCase(categoryId)) {
                    Thievery.getInstance().getLogger().severe(
                            "[Thievery] Item path '" + path + "' is defined in both '"
                                    + existing.getId() + "' and '" + categoryId + "'");
                }
            }
        }
    }
}
