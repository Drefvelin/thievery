package net.tfminecraft.thievery.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.util.CategoryDisplayBuilder;
import net.tfminecraft.thievery.util.ThieveryTexts;
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
    private final CategoryMatch match;
    private final boolean loadoutVisible;
    private final List<CategoryItemEntry> items = new ArrayList<>();
    private final List<AcCraftRef> acCraftRefs = new ArrayList<>();

    public ItemCategory(String key, ConfigurationSection config) {
        id = key;
        name = StringFormatter.formatHex(config.getString("name", key));
        icon = config.getString("icon", "v.paper");
        cost = config.getInt("cost", 1);
        value = config.getDouble("value", 1.0);
        match = parseMatch(config);
        if (config.contains("loadout")) {
            loadoutVisible = config.getBoolean("loadout");
        } else {
            loadoutVisible = true;
        }

        if (match.getType() == CategoryMatchType.COMPOSITE) {
            for (String entry : config.getStringList("items")) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                AcCraftRef.parse(trimmed).ifPresentOrElse(acCraftRefs::add, () -> Thievery.getInstance()
                        .getLogger().severe("[Thievery] Composite '" + id
                                + "' has malformed AC craft ref '" + trimmed + "'"));
            }
        } else if (match.getType() == CategoryMatchType.PATH) {
            for (String entry : config.getStringList("items")) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
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
    }

    private static CategoryMatch parseMatch(ConfigurationSection config) {
        if (!config.contains("match")) {
            return CategoryMatch.path();
        }
        Object raw = config.get("match");
        if (raw instanceof String matchValue && matchValue.equalsIgnoreCase("composite")) {
            return CategoryMatch.composite();
        }
        if (!(raw instanceof ConfigurationSection matchSection)) {
            return CategoryMatch.path();
        }
        if (matchSection.contains("ac_type")) {
            return CategoryMatch.acMaterial(matchSection.getString("ac_type"), matchSection.getInt("ac_tier"));
        }
        return CategoryMatch.path();
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

    public CategoryMatch getMatch() {
        return match;
    }

    public boolean isLoadoutVisible() {
        return loadoutVisible;
    }

    public List<CategoryItemEntry> getItems() {
        return items;
    }

    public List<AcCraftRef> getAcCraftRefs() {
        return Collections.unmodifiableList(acCraftRefs);
    }

    public boolean isPathCategory() {
        return match.getType() == CategoryMatchType.PATH;
    }

    public ItemStack getIconItem(boolean active) {
        ItemStack item = TLibs.getItemAPI().getCreator().getItemFromPath(icon);
        if (item == null) {
            return null;
        }

        item = item.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(name);
        meta.getPersistentDataContainer().set(Keys.categoryId, PersistentDataType.STRING, id);

        List<String> lore = new ArrayList<>();
        lore.add(ThieveryTexts.format(ThieveryTexts.WARN + "Cost: " + ThieveryTexts.SUCCESS + cost));
        lore.add(" ");
        lore.add(ThieveryTexts.format(ThieveryTexts.MUTED + "------------------------"));
        lore.add(ThieveryTexts.format(ThieveryTexts.MUTED + "Items:"));
        lore.addAll(CategoryDisplayBuilder.buildDisplayLines(this));
        lore.add(ThieveryTexts.format(ThieveryTexts.MUTED + "------------------------"));
        lore.add(" ");
        if (active) {
            lore.add(ThieveryTexts.format(ThieveryTexts.SUCCESS + "Active"));
        } else {
            lore.add(ThieveryTexts.format(ThieveryTexts.MUTED + "Inactive"));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public boolean matchesPath(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        for (CategoryItemEntry entry : items) {
            if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, entry.getPath())) {
                return true;
            }
        }
        return false;
    }

    public double getPathWeightFor(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return value;
        }
        for (CategoryItemEntry entry : items) {
            if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, entry.getPath())) {
                return entry.getWeight();
            }
        }
        return value;
    }
}
