package net.tfminecraft.thievery.data;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.util.Keys;

public class ItemCategory {

    public static final class CategoryItemEntry {
        private final String path;
        private final double weight;

        public CategoryItemEntry(String path, double weight) {
            this.path = path;
            this.weight = weight;
        }

        public String getPath() {
            return path;
        }

        public double getWeight() {
            return weight;
        }
    }

    private final String id;
    private final String name;
    private final String icon;
    private final int cost;
    private final double value;
    private final List<CategoryItemEntry> items = new ArrayList<>();

    public ItemCategory(String key, ConfigurationSection config) {
        id = key;
        name = StringFormatter.formatHex(config.getString("name", key));
        icon = config.getString("icon", "v.paper");
        cost = config.getInt("cost", 1);
        value = config.getDouble("value", 1.0);

        for (String entry : config.getStringList("items")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;

            String[] parts = trimmed.split("\\s+");
            String path = parts[0];
            double weight = value;
            if (parts.length > 1) {
                try {
                    weight = Double.parseDouble(parts[1]);
                } catch (NumberFormatException ignored) {
                }
            }
            items.add(new CategoryItemEntry(path, weight));
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public int getCost() {
        return cost;
    }

    public double getValue() {
        return value;
    }

    public List<CategoryItemEntry> getItems() {
        return items;
    }

    public ItemStack getIconItem(boolean active) {
        return getIconItem(active, false);
    }

    public ItemStack getIconItem(boolean active, boolean unlockedThisSession) {
        ItemStack item = TLibs.getItemAPI().getCreator().getItemFromPath(icon);
        if (item == null) return null;

        item = item.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);
        meta.getPersistentDataContainer().set(Keys.categoryId, PersistentDataType.STRING, id);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("§eCost: §a" + cost);
        lore.add("§eItem weight: §a" + value);
        lore.add(" ");
        if (active) {
            lore.add("§aActive");
        } else if (unlockedThisSession) {
            lore.add("§7Inactive");
            lore.add("§7Unlocked this session");
        } else {
            lore.add("§7Inactive");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public boolean matches(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        for (CategoryItemEntry entry : items) {
            if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, entry.getPath())) {
                return true;
            }
        }
        return false;
    }

    public double getWeightFor(ItemStack item) {
        if (item == null || item.getType().isAir()) return value;
        for (CategoryItemEntry entry : items) {
            if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, entry.getPath())) {
                return entry.getWeight();
            }
        }
        return value;
    }
}
