package net.tfminecraft.thievery.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.AdvancedCrafting.Objects.Crafting.CraftingRecipe;
import net.tfminecraft.AdvancedCrafting.Objects.Ingredients.Ingredient;
import net.tfminecraft.AdvancedCrafting.Objects.Ingredients.IngredientType;
import net.tfminecraft.AdvancedCrafting.Objects.Stats.StatTemplate;
import net.tfminecraft.AdvancedCrafting.Utils.ThieveryBridge;
import net.tfminecraft.thievery.data.AcCraftRef;
import net.tfminecraft.thievery.data.ItemCategory;

public final class CategoryDisplayBuilder {

    private CategoryDisplayBuilder() {
    }

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
            double example = ValueResolver.computeIngredientExampleValue(ingredient, category.getValue());
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
            return categoryBase + ValueResolver.tierBonus(tier);
        }
        return categoryBase + base.getIngredientData().getValue() + ValueResolver.tierBonus(tier);
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
                materialValue += ValueResolver.computeIngredientExampleValue(reference, 0) * amount;
            }
        }
        return categoryBase + materialValue + ValueResolver.tierBonus(tier) + 2;
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
