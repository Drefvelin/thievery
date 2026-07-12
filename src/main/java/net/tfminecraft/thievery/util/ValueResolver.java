package net.tfminecraft.thievery.util;

import java.util.List;

import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.Socket.GemSocketsNbtEditor;
import net.Indyuce.mmoitems.stat.data.GemstoneData;
import net.tfminecraft.AdvancedCrafting.Objects.Alloys.Alloy;
import net.tfminecraft.AdvancedCrafting.Objects.Crafting.CraftingRecipe;
import net.tfminecraft.AdvancedCrafting.Objects.Crafting.Quality;
import net.tfminecraft.AdvancedCrafting.Objects.Data.CraftInput;
import net.tfminecraft.AdvancedCrafting.Objects.Data.CraftProvenance;
import net.tfminecraft.AdvancedCrafting.Objects.Ingredients.Ingredient;
import net.tfminecraft.AdvancedCrafting.Utils.ThieveryBridge;
import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.data.ItemCategory;
import net.tfminecraft.thievery.loader.CategoryLoader;

public final class ValueResolver {

    private ValueResolver() {
    }

    public static double compute(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        if (ClueChecker.isClueItem(item)) {
            return 0;
        }
        return categoryBase(item) + acAddon(item) + gemAddon(item);
    }

    public static double computeIngredientExampleValue(Ingredient ingredient, double categoryBase) {
        if (ingredient == null) {
            return categoryBase;
        }
        double addon = ingredient.getIngredientData().getValue();
        if (ingredient.getIngredientData().hasTier()) {
            addon += tierBonus(ingredient.getIngredientData().getTier());
        }
        return categoryBase + addon;
    }

    public static double tierBonus(int tier) {
        if (tier <= 0) {
            return 0;
        }
        return Cache.tierValues.getOrDefault(tier, 0.0);
    }

    private static double categoryBase(ItemStack item) {
        ItemCategory category = CategoryMatcher.resolveFirstMatch(item);
        if (category == null) {
            return Cache.defaultItemValue;
        }
        return switch (category.getMatch().getType()) {
            case PATH -> category.getPathWeightFor(item);
            default -> category.getValue();
        };
    }

    private static double acAddon(ItemStack item) {
        if (!ThieveryBridge.isPluginReady()) {
            return 0;
        }

        CraftProvenance provenance = ThieveryBridge.readProvenance(item);
        if (provenance != null) {
            return craftedAddon(provenance);
        }

        Alloy alloy = ThieveryBridge.resolveAlloy(item);
        if (alloy != null) {
            return alloyAddon(alloy);
        }

        Ingredient ingredient = ThieveryBridge.resolveIngredient(item);
        if (ingredient != null) {
            return ingredientAddon(ingredient);
        }

        return 0;
    }

    private static double ingredientAddon(Ingredient ingredient) {
        double total = ingredient.getIngredientData().getValue();
        if (ingredient.getIngredientData().hasTier()) {
            total += tierBonus(ingredient.getIngredientData().getTier());
        }
        return total;
    }

    private static double alloyAddon(Alloy alloy) {
        double total = ThieveryBridge.sumAlloyIngredientValues(alloy);
        if (alloy.getData() != null && alloy.getData().getTier() > 0) {
            total += tierBonus(alloy.getData().getTier());
        }
        return total;
    }

    private static double craftedAddon(CraftProvenance provenance) {
        double total = sumProvenanceInputs(provenance.getInputs());

        Quality quality = ThieveryBridge.getQualityById(provenance.getQualityId());
        if (quality != null) {
            total += quality.getValue();
        }

        CraftingRecipe recipe = ThieveryBridge.getRecipeById(provenance.getRecipeId());
        if (recipe != null) {
            int majorityTier = ThieveryBridge.resolveMajorityTier(recipe, provenance.getInputs());
            total += tierBonus(majorityTier);
        }

        return total;
    }

    private static double sumProvenanceInputs(List<CraftInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (CraftInput input : inputs) {
            String kind = input.getKind().toLowerCase();
            if (kind.equals("ingredient")) {
                Ingredient ing = ThieveryBridge.getIngredientById(input.getId());
                if (ing != null) {
                    total += ing.getIngredientData().getValue() * input.getAmount();
                }
            } else if (kind.equals("alloy")) {
                Alloy alloy = net.tfminecraft.AdvancedCrafting.Managers.AlloyManager.getAlloyById(input.getId());
                if (alloy != null) {
                    total += alloyAddon(alloy) * input.getAmount();
                }
            }
        }
        return total;
    }

    private static double gemAddon(ItemStack item) {
        if (!MmoItemUtil.hasType(item)) {
            return 0;
        }
        var sockets = GemSocketsNbtEditor.getSockets(item);
        if (sockets == null) {
            return 0;
        }
        double total = 0;
        for (GemstoneData gem : sockets.getGems()) {
            String path = "m." + gem.getMMOItemType().toLowerCase() + "." + gem.getMMOItemID().toLowerCase();
            total += CategoryLoader.getWeightForPath(path);
        }
        return total;
    }
}
