package net.tfminecraft.thievery.category;

import java.util.ArrayList;
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
import net.tfminecraft.thievery.player.PlayerData;
import net.tfminecraft.thievery.loader.CategoryLoader;
import net.tfminecraft.thievery.steal.StealItemDisplay;
import net.tfminecraft.thievery.clue.ClueChecker;
import net.tfminecraft.thievery.utils.ThieveryTexts;

public final class CategoryHandler {

    private CategoryHandler() {
    }

    public static boolean matches(ItemCategory category, ItemStack item) {
        if (category == null || item == null || item.getType().isAir()) {
            return false;
        }
        return matchesDirect(category, item) || matchesCraftInCategory(category, item);
    }

    public static boolean matchesDirect(ItemCategory category, ItemStack item) {
        if (category == null || item == null || item.getType().isAir()) {
            return false;
        }
        if (category.isMoneyType()) {
            return DenarMoney.isMoney(item);
        }
        for (ItemCategory.CategoryItemEntry entry : category.getItems()) {
            if (matchesDirectSlug(entry.getSlug(), item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesCraftInCategory(ItemCategory category, ItemStack item) {
        AcCraftRef crafted = resolveCraftedMatch(item);
        if (crafted == null || category == null) {
            return false;
        }
        for (ItemCategory.CategoryItemEntry entry : category.getItems()) {
            var parsed = CategorySlugs.parseCraftRef(entry.getSlug());
            if (parsed.isPresent() && parsed.get().equals(crafted)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesAnyActive(PlayerData playerData, ItemStack item) {
        if (ClueChecker.isClueItem(item)) {
            return false;
        }

        if (DenarMoney.isMoney(item)) {
            ItemCategory money = CategoryLoader.getMoneyCategory();
            return money != null && playerData.isCategoryActive(money.getId());
        }

        for (String activeId : playerData.getActiveCategories()) {
            ItemCategory category = CategoryLoader.getById(activeId);
            if (category == null) {
                continue;
            }
            if (matches(category, item)) {
                return true;
            }
        }

        return resolveFirstMatch(item) == null && resolveCraftedMatch(item) == null;
    }

    public static ItemCategory resolveFirstMatch(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        if (DenarMoney.isMoney(item)) {
            return CategoryLoader.getMoneyCategory();
        }
        for (ItemCategory category : CategoryLoader.getAsList()) {
            if (matchesDirect(category, item)) {
                return category;
            }
        }
        return null;
    }

    public static ItemCategory resolveCraftCategory(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        for (ItemCategory category : CategoryLoader.getAsList()) {
            if (matchesCraftInCategory(category, item)) {
                return category;
            }
        }
        return null;
    }

    public static double resolveItemWeight(ItemStack item) {
        if (DenarMoney.isMoney(item)) {
            return DenarMoney.stealPerItem(item);
        }
        AcCraftRef crafted = resolveCraftedMatch(item);
        if (crafted != null) {
            return CategoryLoader.getWeightForCraftRef(crafted);
        }
        ItemCategory category = resolveFirstMatch(item);
        if (category == null) {
            return CategoryLoader.getDefaultWeight();
        }
        for (ItemCategory.CategoryItemEntry entry : category.getItems()) {
            if (matchesDirectSlug(entry.getSlug(), item)) {
                return entry.getWeight();
            }
        }
        return category.getValue();
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

    public static boolean matchesDirectSlug(String slug, ItemStack item) {
        if (slug == null || slug.isBlank() || item == null || item.getType().isAir()) {
            return false;
        }
        if (CategorySlugs.isMaterialSlug(slug)) {
            return matchesAcMaterialSlug(slug, item);
        }
        if (CategorySlugs.isPathSlug(slug)) {
            return TLibs.getItemAPI().getChecker().checkItemWithPath(item, slug);
        }
        return false;
    }

    private static boolean matchesAcMaterialSlug(String slug, ItemStack item) {
        if (!ThieveryBridge.isPluginReady()) {
            return false;
        }
        String wantedType = CategorySlugs.materialType(slug);
        int wantedTier = CategorySlugs.materialTier(slug);

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

    public static ItemCategory resolveCategory(ItemStack item) {
        ItemCategory direct = resolveFirstMatch(item);
        return direct != null ? direct : resolveCraftCategory(item);
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

    public static List<String> buildDisplayLines(ItemCategory category) {
        List<String> lore = new ArrayList<>();
        if (category.isMoneyType()) {
            lore.add(ThieveryTexts.formatGui(ThieveryTexts.WHITE + "- Pouch and coins"));
            lore.add(ThieveryTexts.formatGui(ThieveryTexts.MUTED + "  Steal value: "
                    + ThieveryTexts.GUI_SUCCESS + StealItemDisplay.formatValue(category.getAmountPerMoney())
                    + ThieveryTexts.MUTED + " per denar"));
            return lore;
        }
        for (ItemCategory.CategoryItemEntry entry : category.getItems()) {
            String slug = entry.getSlug();
            if (CategorySlugs.isMaterialSlug(slug)) {
                lore.addAll(buildMaterialSlugLines(slug, entry.getWeight()));
            } else if (CategorySlugs.isCraftSlug(slug)) {
                CategorySlugs.parseCraftRef(slug).ifPresent(ref ->
                        lore.addAll(buildAcCraftRefLines(ref, entry.getWeight())));
            } else if (CategorySlugs.isPathSlug(slug)) {
                ItemStack preview = TLibs.getItemAPI().getCreator().getItemFromPath(slug);
                String itemName = preview != null ? StringFormatter.getName(preview) : slug;
                lore.add(formatLine(itemName, entry.getWeight()));
            }
        }
        return lore;
    }

    private static List<String> buildMaterialSlugLines(String slug, double categoryBase) {
        List<String> lore = new ArrayList<>();
        if (!ThieveryBridge.isPluginReady()) {
            lore.add(ThieveryTexts.formatGui(ThieveryTexts.MUTED + "Crafting data unavailable"));
            return lore;
        }

        String wantedType = CategorySlugs.materialType(slug);
        int wantedTier = CategorySlugs.materialTier(slug);
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
            double example = ItemValue.computeIngredientExampleValue(ingredient, categoryBase);
            lore.add(formatLine(itemName, example));
        }

        if (ThieveryBridge.hasBaseIngredientForType(wantedType, wantedTier)) {
            String typeName = type != null ? type.getName() : wantedType;
            String label = "Tier " + toRoman(wantedTier) + " " + typeName + " Alloys";
            lore.add(formatLine(label, estimateAlloyExampleValue(wantedType, wantedTier, categoryBase)));
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

    public static List<String> buildAcCraftRefLines(AcCraftRef ref, double categoryBase) {
        List<String> lore = new ArrayList<>();
        if (!ThieveryBridge.isPluginReady()) {
            lore.add(ThieveryTexts.formatGui(ThieveryTexts.MUTED + "Crafting data unavailable"));
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
        return ThieveryTexts.formatGui(ThieveryTexts.WHITE + "- " + itemName + " " + ThieveryTexts.MUTED + "("
                + ThieveryTexts.GUI_WARN + "value: " + ThieveryTexts.GUI_SUCCESS
                + StealItemDisplay.formatValue(weight) + ThieveryTexts.GUI_WARN + "/item"
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
