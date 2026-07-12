package net.tfminecraft.thievery.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;

import me.Plugins.TLibs.Socket.GemSocketsNbtEditor;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.Indyuce.mmoitems.stat.data.GemstoneData;
import net.tfminecraft.AdvancedCrafting.Managers.AlloyManager;
import net.tfminecraft.AdvancedCrafting.Objects.Alloys.Alloy;
import net.tfminecraft.AdvancedCrafting.Objects.Crafting.CraftingRecipe;
import net.tfminecraft.AdvancedCrafting.Objects.Crafting.Quality;
import net.tfminecraft.AdvancedCrafting.Objects.Data.AlloyRecipe;
import net.tfminecraft.AdvancedCrafting.Objects.Data.CraftInput;
import net.tfminecraft.AdvancedCrafting.Objects.Data.CraftProvenance;
import net.tfminecraft.AdvancedCrafting.Objects.Ingredients.Ingredient;
import net.tfminecraft.AdvancedCrafting.Utils.ThieveryBridge;
import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.data.AcCraftRef;
import net.tfminecraft.thievery.data.CategoryMatchType;
import net.tfminecraft.thievery.data.ItemCategory;
import net.tfminecraft.thievery.loader.CategoryLoader;

public final class ItemValueInspector {

    private static final String DIVIDER = ThieveryTexts.DARK + "§m" + repeat('─', 28);
    private static final String HEADER = ThieveryTexts.DARK + "§m" + repeat('━', 28);

    private ItemValueInspector() {
    }

    public static List<String> buildReport(ItemStack item) {
        List<String> lines = new ArrayList<>();
        lines.add(ThieveryTexts.format(HEADER));
        lines.add(ThieveryTexts.format("#c9a24f§lItem Value Inspector"));
        lines.add(ThieveryTexts.format(HEADER));

        if (item == null || item.getType().isAir()) {
            lines.add(ThieveryTexts.format(ThieveryTexts.ERROR + "No item in hand."));
            lines.add(ThieveryTexts.format(HEADER));
            return lines;
        }

        if (ClueChecker.isClueItem(item)) {
            lines.add(line("Item", StringFormatter.getName(item)));
            lines.add(line("Path", pathOf(item)));
            lines.add(ThieveryTexts.format(DIVIDER));
            lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "Clue items are never stealable."));
            lines.add(valueLine("Total", 0));
            lines.add(ThieveryTexts.format(HEADER));
            return lines;
        }

        if (BundleHandler.isBundle(item)) {
            appendBundleReport(lines, item);
            lines.add(ThieveryTexts.format(HEADER));
            return lines;
        }

        appendItemHeader(lines, item);
        lines.add(ThieveryTexts.format(DIVIDER));

        double categoryBase = appendCategorySection(lines, item);
        double acAddon = appendAcSection(lines, item);
        double gemAddon = appendGemSection(lines, item);

        double perItem = categoryBase + acAddon + gemAddon;
        double computed = ValueResolver.compute(item);
        if (Math.abs(perItem - computed) > 0.001) {
            perItem = computed;
        }

        lines.add(ThieveryTexts.format(DIVIDER));
        lines.add(totalLine("Per item", perItem, true));
        if (item.getAmount() > 1) {
            lines.add(totalLine("Stack total (×" + item.getAmount() + ")", perItem * item.getAmount(), true));
        }
        lines.add(ThieveryTexts.format(HEADER));
        return lines;
    }

    private static void appendBundleReport(List<String> lines, ItemStack bundle) {
        appendItemHeader(lines, bundle);
        lines.add(ThieveryTexts.format(DIVIDER));
        lines.add(sectionTitle("Bundle", CategoryResolver.getPerItemValue(bundle)));

        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  Empty bundle shell only."));
            lines.add(totalLine("Total", CategoryResolver.getTotalValue(bundle), true));
            return;
        }

        double innerTotal = 0;
        for (ItemStack inner : meta.getItems()) {
            if (inner == null || inner.getType().isAir()) {
                continue;
            }
            double innerValue = CategoryResolver.getPerItemValue(inner) * inner.getAmount();
            innerTotal += innerValue;
            lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  • " + ThieveryTexts.WHITE
                    + StringFormatter.getName(inner) + ThieveryTexts.MUTED + " ×" + inner.getAmount()
                    + ThieveryTexts.MUTED + "  →  " + ThieveryTexts.ACCENT
                    + StealItemDisplay.formatValue(innerValue)));
        }
        double shell = CategoryResolver.getPerItemValue(bundle);
        lines.add(ThieveryTexts.format(DIVIDER));
        lines.add(valueLine("Bundle shell", shell));
        lines.add(valueLine("Contents", innerTotal));
        lines.add(totalLine("Total", shell + innerTotal, true));
    }

    private static void appendItemHeader(List<String> lines, ItemStack item) {
        lines.add(line("Item", StringFormatter.getName(item)));
        lines.add(line("Path", pathOf(item)));
        lines.add(line("Type", itemTypeLabel(item)));
        if (item.getAmount() > 1) {
            lines.add(line("Amount", String.valueOf(item.getAmount())));
        }
        appendMatchingCategories(lines, item);
    }

    private static void appendMatchingCategories(List<String> lines, ItemStack item) {
        List<String> matches = new ArrayList<>();
        for (ItemCategory category : CategoryLoader.getAsList()) {
            if (CategoryMatcher.matches(category, item)) {
                matches.add(category.getId());
            }
        }
        AcCraftRef crafted = CategoryMatcher.resolveCraftedMatch(item);
        if (crafted != null) {
            matches.add(crafted.getRawId());
        }
        if (matches.isEmpty()) {
            lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "Categories: "
                    + ThieveryTexts.WHITE + "none (uncategorized)"));
        } else {
            lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "Categories: "
                    + ThieveryTexts.INFO + String.join(ThieveryTexts.MUTED + ", " + ThieveryTexts.INFO, matches)));
        }
    }

    private static double appendCategorySection(List<String> lines, ItemStack item) {
        ItemCategory category = CategoryMatcher.resolveFirstMatch(item);
        AcCraftRef crafted = CategoryMatcher.resolveCraftedMatch(item);

        if (category == null && crafted == null) {
            lines.add(sectionTitle("Category base", Cache.defaultItemValue));
            lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  No category match — using default_item_value"));
            return Cache.defaultItemValue;
        }

        if (category == null) {
            lines.add(sectionTitle("Category base", Cache.defaultItemValue));
            lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  Craft: " + ThieveryTexts.INFO
                    + crafted.getRawId() + ThieveryTexts.MUTED + " ("
                    + crafted.getStatTemplate() + " tier " + crafted.getTier() + ")"));
            lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  Value comes from composition below"));
            return Cache.defaultItemValue;
        }

        double base = switch (category.getMatch().getType()) {
            case PATH -> category.getPathWeightFor(item);
            default -> category.getValue();
        };

        lines.add(sectionTitle("Category base", base));
        lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  Category: " + ThieveryTexts.INFO + category.getId()
                + ThieveryTexts.MUTED + " (" + matchTypeLabel(category) + ")"));

        if (category.getMatch().getType() == CategoryMatchType.PATH) {
            lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  Path-listed item weight"));
        } else if (base == 0) {
            lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  Value comes from composition below"));
        }
        return base;
    }

    private static double appendAcSection(List<String> lines, ItemStack item) {
        if (!ThieveryBridge.isPluginReady()) {
            return 0;
        }

        CraftProvenance provenance = ThieveryBridge.readProvenance(item);
        if (provenance != null) {
            return appendCraftedAc(lines, provenance);
        }

        Alloy alloy = ThieveryBridge.resolveAlloy(item);
        if (alloy != null) {
            return appendAlloyAc(lines, alloy);
        }

        Ingredient ingredient = ThieveryBridge.resolveIngredient(item);
        if (ingredient != null) {
            return appendIngredientAc(lines, ingredient);
        }

        return 0;
    }

    private static double appendIngredientAc(List<String> lines, Ingredient ingredient) {
        lines.add(ThieveryTexts.format(DIVIDER));
        int matValue = ingredient.getIngredientData().getValue();
        int tier = ingredient.getIngredientData().hasTier() ? ingredient.getIngredientData().getTier() : 0;
        double tierBonus = ValueResolver.tierBonus(tier);
        double total = matValue + tierBonus;

        lines.add(sectionTitle("Material", total));
        lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  Ingredient: " + ThieveryTexts.WHITE + ingredient.getId()
                + ThieveryTexts.MUTED + " (" + ingredient.getIngredientData().getType().getId() + ")"));
        lines.add(valueLine("  Material value", matValue));
        if (tier > 0) {
            lines.add(valueLine("  Tier " + toRoman(tier) + " bonus", tierBonus));
        }
        return total;
    }

    private static double appendAlloyAc(List<String> lines, Alloy alloy) {
        lines.add(ThieveryTexts.format(DIVIDER));
        AlloyRecipe recipe = alloy.getData() != null ? alloy.getData().getRecipe() : null;
        int forgeSum = ThieveryBridge.sumForgeInputValues(recipe);
        int tier = alloy.getData() != null ? alloy.getData().getTier() : 0;
        double tierBonus = ValueResolver.tierBonus(tier);
        double total = forgeSum + tierBonus;

        lines.add(sectionTitle("Alloy", total));
        lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  Alloy: " + ThieveryTexts.WHITE + alloy.getId()));
        if (recipe != null) {
            Ingredient base = ThieveryBridge.getIngredientById(recipe.getBaseId());
            if (base != null) {
                lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  • Base " + ThieveryTexts.WHITE
                        + recipe.getBaseId() + ThieveryTexts.MUTED + "  →  " + ThieveryTexts.ACCENT
                        + StealItemDisplay.formatValue(base.getIngredientData().getValue())));
            }
            for (String catalystId : recipe.getCatalystIds()) {
                Ingredient catalyst = ThieveryBridge.getIngredientById(catalystId);
                if (catalyst != null) {
                    lines.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  • Catalyst " + ThieveryTexts.WHITE
                            + catalystId + ThieveryTexts.MUTED + "  →  " + ThieveryTexts.ACCENT
                            + StealItemDisplay.formatValue(catalyst.getIngredientData().getValue())));
                }
            }
        }
        if (tier > 0) {
            lines.add(valueLine("  Tier " + toRoman(tier) + " bonus", tierBonus));
        }
        return total;
    }

    private static double appendCraftedAc(List<String> lines, CraftProvenance provenance) {
        List<String> details = new ArrayList<>();
        double materials = 0;

        CraftingRecipe recipe = ThieveryBridge.getRecipeById(provenance.getRecipeId());
        if (recipe != null) {
            details.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  Recipe: " + ThieveryTexts.WHITE
                    + provenance.getRecipeId() + ThieveryTexts.MUTED + " (" + recipe.getCategoryId() + ")"));
        }

        for (CraftInput input : provenance.getInputs()) {
            String kind = input.getKind().toLowerCase();
            if (kind.equals("ingredient")) {
                Ingredient ing = ThieveryBridge.getIngredientById(input.getId());
                if (ing != null) {
                    double part = ing.getIngredientData().getValue() * input.getAmount();
                    materials += part;
                    details.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  • " + ThieveryTexts.WHITE
                            + input.getId() + " ×" + input.getAmount() + ThieveryTexts.MUTED + "  →  "
                            + ThieveryTexts.ACCENT + StealItemDisplay.formatValue(part)));
                }
            } else if (kind.equals("alloy")) {
                Alloy alloy = AlloyManager.getAlloyById(input.getId());
                if (alloy != null) {
                    double perAlloy = ThieveryBridge.sumAlloyIngredientValues(alloy);
                    if (alloy.getData() != null && alloy.getData().getTier() > 0) {
                        perAlloy += ValueResolver.tierBonus(alloy.getData().getTier());
                    }
                    double part = perAlloy * input.getAmount();
                    materials += part;
                    details.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  • Alloy " + ThieveryTexts.WHITE
                            + input.getId() + " ×" + input.getAmount() + ThieveryTexts.MUTED + "  →  "
                            + ThieveryTexts.ACCENT + StealItemDisplay.formatValue(part)));
                }
            }
        }

        double qualityBonus = 0;
        Quality quality = ThieveryBridge.getQualityById(provenance.getQualityId());
        if (quality != null) {
            qualityBonus = quality.getValue();
            details.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  Quality: " + ThieveryTexts.WHITE
                    + quality.getName() + ThieveryTexts.MUTED + "  →  " + ThieveryTexts.ACCENT
                    + StealItemDisplay.formatValue(qualityBonus)));
        }

        double tierBonus = 0;
        if (recipe != null) {
            int majorityTier = ThieveryBridge.resolveMajorityTier(recipe, provenance.getInputs());
            tierBonus = ValueResolver.tierBonus(majorityTier);
            if (majorityTier > 0) {
                details.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  Majority tier " + ThieveryTexts.WHITE
                        + toRoman(majorityTier) + ThieveryTexts.MUTED + " bonus  →  " + ThieveryTexts.ACCENT
                        + StealItemDisplay.formatValue(tierBonus)));
            }
        }

        double total = materials + qualityBonus + tierBonus;
        lines.add(ThieveryTexts.format(DIVIDER));
        lines.add(sectionTitle("Crafted", total));
        lines.addAll(details);
        return total;
    }

    private static double appendGemSection(List<String> lines, ItemStack item) {
        if (!MmoItemUtil.hasType(item)) {
            return 0;
        }
        var sockets = GemSocketsNbtEditor.getSockets(item);
        if (sockets == null || sockets.getGems().isEmpty()) {
            return 0;
        }

        List<String> details = new ArrayList<>();
        double total = 0;
        for (GemstoneData gem : sockets.getGems()) {
            String path = "m." + gem.getMMOItemType().toLowerCase() + "." + gem.getMMOItemID().toLowerCase();
            double gemValue = CategoryLoader.getWeightForPath(path);
            total += gemValue;

            String gemName = path;
            ItemStack probe = TLibs.getItemAPI().getCreator().getItemFromPath(path);
            if (probe != null) {
                gemName = StringFormatter.getName(probe);
            }

            ItemCategory gemCategory = probe != null ? CategoryMatcher.resolveFirstMatch(probe) : null;
            String categoryNote = gemCategory != null
                    ? ThieveryTexts.MUTED + " [" + ThieveryTexts.INFO + gemCategory.getId() + ThieveryTexts.MUTED + "]"
                    : ThieveryTexts.MUTED + " [default]";

            details.add(ThieveryTexts.format(ThieveryTexts.MUTED + "  • " + ThieveryTexts.WHITE + gemName
                    + categoryNote + ThieveryTexts.MUTED + "  →  " + ThieveryTexts.ACCENT
                    + StealItemDisplay.formatValue(gemValue)));
            details.add(ThieveryTexts.format(ThieveryTexts.DARK + "    " + path));
        }

        lines.add(ThieveryTexts.format(DIVIDER));
        lines.add(sectionTitle("Socketed gems", total));
        lines.addAll(details);
        return total;
    }

    private static String pathOf(ItemStack item) {
        return TLibs.getItemAPI().getChecker().getAsStringPath(item);
    }

    private static String itemTypeLabel(ItemStack item) {
        if (ThieveryBridge.isPluginReady()) {
            if (ThieveryBridge.readProvenance(item) != null) {
                return "Crafted";
            }
            if (ThieveryBridge.resolveAlloy(item) != null) {
                return "Alloy";
            }
            Ingredient ing = ThieveryBridge.resolveIngredient(item);
            if (ing != null) {
                return "Material (" + ing.getIngredientData().getType().getId() + ")";
            }
        }
        if (MmoItemUtil.hasType(item)) {
            var sockets = GemSocketsNbtEditor.getSockets(item);
            if (sockets != null && !sockets.getGems().isEmpty()) {
                return "MMO Item (socketed)";
            }
            return "MMO Item";
        }
        String path = pathOf(item);
        if (path.startsWith("m.")) {
            return "MMO Item";
        }
        if (path.startsWith("ia.")) {
            return "ItemsAdder";
        }
        return "Vanilla / Other";
    }

    private static String matchTypeLabel(ItemCategory category) {
        return switch (category.getMatch().getType()) {
            case PATH -> "path list";
            case AC_MATERIAL -> category.getMatch().getAcType() + " tier "
                    + category.getMatch().getAcTier();
            case COMPOSITE -> "composite";
        };
    }

    private static String line(String label, String value) {
        return ThieveryTexts.format(ThieveryTexts.MUTED + label + ": " + ThieveryTexts.WHITE + value);
    }

    private static String sectionTitle(String label, double value) {
        return ThieveryTexts.format(ThieveryTexts.WARN + label + repeat(' ', Math.max(1, 18 - label.length()))
                + ThieveryTexts.ACCENT + StealItemDisplay.formatValue(value));
    }

    private static String valueLine(String label, double value) {
        return ThieveryTexts.format(ThieveryTexts.MUTED + label + ": " + ThieveryTexts.ACCENT
                + StealItemDisplay.formatValue(value));
    }

    private static String totalLine(String label, double value, boolean bold) {
        String weight = bold ? "§l" : "";
        return ThieveryTexts.format(ThieveryTexts.MUTED + label + ": " + weight + "#e8c170"
                + StealItemDisplay.formatValue(value));
    }

    private static String toRoman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> String.valueOf(tier);
        };
    }

    private static String repeat(char c, int count) {
        return String.valueOf(c).repeat(count);
    }
}
