package net.tfminecraft.thievery.category;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.AdvancedCrafting.Objects.Alloys.Alloy;
import net.tfminecraft.AdvancedCrafting.Objects.Crafting.CraftingRecipe;
import net.tfminecraft.AdvancedCrafting.Objects.Data.CraftProvenance;
import net.tfminecraft.AdvancedCrafting.Objects.Ingredients.Ingredient;
import net.tfminecraft.AdvancedCrafting.Objects.Ingredients.IngredientType;
import net.tfminecraft.AdvancedCrafting.Objects.Stats.StatTemplate;
import net.tfminecraft.AdvancedCrafting.Utils.ThieveryBridge;
import net.tfminecraft.thievery.category.AcCraftRef;
import net.tfminecraft.thievery.category.CategoryMatchType;
import net.tfminecraft.thievery.category.ItemCategory;
import net.tfminecraft.thievery.player.PlayerData;
import net.tfminecraft.thievery.loader.CategoryLoader;
import net.tfminecraft.thievery.steal.StealItemDisplay;
import net.tfminecraft.thievery.clue.ClueChecker;
import net.tfminecraft.thievery.utils.ThieveryTexts;

public final class CategoryHandler {

    private CategoryHandler() {
    }

    // --- CategoryMatcher ---

    public static boolean matches(ItemCategory category, ItemStack item) {
        if (category == null || item == null || item.getType().isAir()) {
            return false;
        }
        return switch (category.getMatch().getType()) {
            case PATH -> category.matchesPath(item);
            case AC_MATERIAL -> matchesAcMaterial(category, item);
            case COMPOSITE -> false;
        };
    }

    public static boolean matchesAnyActive(PlayerData playerData, ItemStack item) {
        if (ClueChecker.isClueItem(item)) {
            return false;
        }

        Set<String> activeCategoryIds = new HashSet<>();
        Set<AcCraftRef> activeCraftRefs = new HashSet<>();
        for (String activeId : playerData.getActiveCategories()) {
            ItemCategory category = CategoryLoader.getById(activeId);
            if (category == null) {
                continue;
            }
            if (category.getMatch().getType() == CategoryMatchType.COMPOSITE) {
                activeCraftRefs.addAll(category.getAcCraftRefs());
            } else {
                activeCategoryIds.add(activeId);
            }
        }

        for (ItemCategory category : CategoryLoader.getAsList()) {
            if (category.getMatch().getType() == CategoryMatchType.COMPOSITE) {
                continue;
            }
            if (!activeCategoryIds.contains(category.getId())) {
                continue;
            }
            if (matches(category, item)) {
                return true;
            }
        }

        if (matchesCraftRef(item, activeCraftRefs)) {
            return true;
        }

        return resolveFirstMatch(item) == null && resolveCraftedMatch(item) == null;
    }

    public static ItemCategory resolveFirstMatch(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        for (ItemCategory category : CategoryLoader.getAsList()) {
            if (category.getMatch().getType() == CategoryMatchType.COMPOSITE) {
                continue;
            }
            if (matches(category, item)) {
                return category;
            }
        }
        return null;
    }

    public static AcCraftRef resolveCraftedMatch(ItemStack item) {
        if (!ThieveryBridge.isPluginReady() || item == null || item.getType().isAir()) {
            return null;
        }
        CraftProvenance provenance = ThieveryBridge.readProvenance(item);
        if (provenance == null) {
            return null;
        }
        CraftingRecipe recipe = ThieveryBridge.getRecipeById(provenance.getRecipeId());
        if (recipe == null || recipe.getStatTemplateId() == null) {
            return null;
        }
        int tier = ThieveryBridge.resolveMajorityTier(recipe, provenance.getInputs());
        String templateId = recipe.getStatTemplateId();
        return AcCraftRef.parse("ac_" + templateId + "_tier_" + tier).orElse(null);
    }

    public static boolean matchesCraftRef(ItemStack item, Set<AcCraftRef> activeCraftRefs) {
        if (activeCraftRefs == null || activeCraftRefs.isEmpty()) {
            return false;
        }
        AcCraftRef match = resolveCraftedMatch(item);
        return match != null && activeCraftRefs.contains(match);
    }

    private static boolean matchesAcMaterial(ItemCategory category, ItemStack item) {
        if (!ThieveryBridge.isPluginReady()) {
            return false;
        }
        String wantedType = category.getMatch().getAcType();
        int wantedTier = category.getMatch().getAcTier();

        Alloy alloy = ThieveryBridge.resolveAlloy(item);
        if (alloy != null && alloy.getData() != null) {
            return alloy.getData().getType().getId().equalsIgnoreCase(wantedType)
                    && alloy.getData().getTier() == wantedTier;
        }

        Ingredient ingredient = ThieveryBridge.resolveIngredient(item);
        if (ingredient == null) {
            return false;
        }
        return ingredient.getIngredientData().getType().getId().equalsIgnoreCase(wantedType)
                && ingredient.getIngredientData().hasTier()
                && ingredient.getIngredientData().getTier() == wantedTier;
    }

    // --- CategoryResolver ---

    public static ItemCategory resolveCategory(ItemStack item) {
        return resolveFirstMatch(item);
    }

    public static boolean canRevealItem(PlayerData playerData, ItemStack item) {
        if (ClueChecker.isClueItem(item)) {
            return false;
        }
        if (ItemValue.isBundle(item)) {
            return ItemValue.canRevealBundle(playerData, item);
        }
        return matchesAnyActive(playerData, item);
    }

    public static Set<String> getActiveCategoryIds(PlayerData playerData) {
        return playerData.getActiveCategories().stream().collect(Collectors.toSet());
    }

    public static double getPerItemValue(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        if (ClueChecker.isClueItem(item)) {
            return 0;
        }
        return ItemValue.compute(item);
    }

    public static double getTotalValue(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }

        if (ItemValue.isBundle(item)) {
            return ItemValue.getContentsValue(item);
        }

        return getPerItemValue(item) * item.getAmount();
    }

    // --- CategoryDisplayBuilder ---

    public static List<String> buildDisplayLines(ItemCategory category) {
        return switch (category.getMatch().getType()) {
            case PATH -> buildPathLines(category);
            case AC_MATERIAL -> buildAcMaterialLines(category);
            case COMPOSITE -> buildCompositeLines(category);
        };
    }

    private static List<String> buildPathLines(ItemCategory category) {
        List<String> lore = new ArrayList<>();
        for (ItemCategory.CategoryItemEntry entry : category.getItems()) {
            ItemStack preview = TLibs.getItemAPI().getCreator().getItemFromPath(entry.getPath());
            String itemName = preview != null ? StringFormatter.getName(preview) : entry.getPath();
            lore.add(formatLine(itemName, entry.getWeight()));
        }
        return lore;
    }

    private static List<String> buildAcMaterialLines(ItemCategory category) {
        List<String> lore = new ArrayList<>();
        if (!ThieveryBridge.isPluginReady()) {
            lore.add(ThieveryTexts.format(ThieveryTexts.MUTED + "Crafting data unavailable"));
            return lore;
        }

        String wantedType = category.getMatch().getAcType();
        int wantedTier = category.getMatch().getAcTier();
        IngredientType type = ThieveryBridge.getIngredientType(wantedType);

        for (Ingredient ingredient : ThieveryBridge.getAllIngredients()) {
            if (!ingredient.getIngredientData().getType().getId().equalsIgnoreCase(wantedType)) {
                continue;
            }
            if (!ingredient.getIngredientData().hasTier()
                    || ingredient.getIngredientData().getTier() != wantedTier) {
                continue;
            }
            String itemName = resolveIngredientName(ingredient);
            double example = ItemValue.computeIngredientExampleValue(ingredient, category.getValue());
            lore.add(formatLine(itemName, example));
        }

        if (ThieveryBridge.hasBaseIngredientForType(wantedType, wantedTier)) {
            String typeName = type != null ? type.getName() : wantedType;
            String label = "Tier " + toRoman(wantedTier) + " " + typeName + " Alloys";
            lore.add(formatLine(label, estimateAlloyExampleValue(wantedType, wantedTier, category.getValue())));
        }
        return lore;
    }

    private static double estimateAlloyExampleValue(String typeId, int tier, double categoryBase) {
        Ingredient base = findBaseIngredient(typeId, tier);
        if (base == null) {
            return categoryBase + ItemValue.tierBonus(tier);
        }
        return categoryBase + base.getIngredientData().getValue() + ItemValue.tierBonus(tier);
    }

    private static Ingredient findBaseIngredient(String typeId, int tier) {
        for (Ingredient ingredient : ThieveryBridge.getAllIngredients()) {
            if (!ingredient.getIngredientData().getType().getId().equalsIgnoreCase(typeId)) {
                continue;
            }
            if (!ingredient.getIngredientData().canBeBase()) {
                continue;
            }
            if (ingredient.getIngredientData().hasTier()
                    && ingredient.getIngredientData().getTier() == tier) {
                return ingredient;
            }
        }
        return null;
    }

    private static List<String> buildCompositeLines(ItemCategory category) {
        List<String> lore = new ArrayList<>();
        for (AcCraftRef ref : category.getAcCraftRefs()) {
            lore.addAll(buildAcCraftRefLines(ref, category.getValue()));
        }
        return lore;
    }

    public static List<String> buildAcCraftRefLines(AcCraftRef ref, double categoryBase) {
        List<String> lore = new ArrayList<>();
        if (!ThieveryBridge.isPluginReady()) {
            lore.add(ThieveryTexts.format(ThieveryTexts.MUTED + "Crafting data unavailable"));
            return lore;
        }

        StatTemplate template = ThieveryBridge.getStatTemplate(ref.getStatTemplate());
        String displayName = template != null ? template.getName() : formatId(ref.getStatTemplate());

        CraftingRecipe recipe = ThieveryBridge.findRecipeByStatTemplate(ref.getStatTemplate());
        double example = categoryBase;
        if (recipe != null) {
            example = estimateCraftValue(recipe, ref.getTier(), categoryBase);
        }

        lore.add(formatLine(displayName, example));
        return lore;
    }

    private static double estimateCraftValue(CraftingRecipe recipe, int tier, double categoryBase) {
        double materialValue = 0;
        for (var entry : recipe.getRecipe().entrySet()) {
            String typeId = entry.getKey();
            int amount = entry.getValue();
            Ingredient reference = findReferenceIngredient(typeId, tier);
            if (reference != null) {
                materialValue += ItemValue.computeIngredientExampleValue(reference, 0) * amount;
            }
        }
        return categoryBase + materialValue + ItemValue.tierBonus(tier) + 2;
    }

    private static Ingredient findReferenceIngredient(String typeId, int tier) {
        for (Ingredient ingredient : ThieveryBridge.getAllIngredients()) {
            if (!ingredient.getIngredientData().getType().getId().equalsIgnoreCase(typeId)) {
                continue;
            }
            if (ingredient.getIngredientData().hasTier()
                    && ingredient.getIngredientData().getTier() == tier) {
                return ingredient;
            }
        }
        return null;
    }

    private static String resolveIngredientName(Ingredient ingredient) {
        ItemStack preview = TLibs.getItemAPI().getCreator().getItemFromPath(ingredient.getPath());
        if (preview != null) {
            return StringFormatter.getName(preview);
        }
        return formatId(ingredient.getId());
    }

    private static String formatId(String id) {
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private static String formatLine(String itemName, double weight) {
        return ThieveryTexts.format(ThieveryTexts.WHITE + "- " + itemName + " " + ThieveryTexts.MUTED + "("
                + ThieveryTexts.WARN + "value: " + ThieveryTexts.SUCCESS
                + StealItemDisplay.formatValue(weight) + ThieveryTexts.WARN + "/item"
                + ThieveryTexts.MUTED + ")");
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
}
