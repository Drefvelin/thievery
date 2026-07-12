package net.tfminecraft.thievery.util;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.inventory.ItemStack;

import net.tfminecraft.AdvancedCrafting.Objects.Alloys.Alloy;
import net.tfminecraft.AdvancedCrafting.Objects.Crafting.CraftingRecipe;
import net.tfminecraft.AdvancedCrafting.Objects.Data.CraftProvenance;
import net.tfminecraft.AdvancedCrafting.Objects.Ingredients.Ingredient;
import net.tfminecraft.AdvancedCrafting.Utils.ThieveryBridge;
import net.tfminecraft.thievery.data.AcCraftRef;
import net.tfminecraft.thievery.data.CategoryMatchType;
import net.tfminecraft.thievery.data.ItemCategory;
import net.tfminecraft.thievery.data.PlayerData;
import net.tfminecraft.thievery.loader.CategoryLoader;

public final class CategoryMatcher {

    private CategoryMatcher() {
    }

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
}
