package net.tfminecraft.thievery.util;



import java.util.Set;

import java.util.stream.Collectors;



import org.bukkit.inventory.ItemStack;



import net.tfminecraft.thievery.cache.Cache;

import net.tfminecraft.thievery.data.ItemCategory;

import net.tfminecraft.thievery.data.PlayerData;

import net.tfminecraft.thievery.loader.CategoryLoader;



public final class CategoryResolver {



    private CategoryResolver() {}



    public static ItemCategory resolveCategory(ItemStack item) {

        if (item == null || item.getType().isAir()) return null;

        for (ItemCategory category : CategoryLoader.getAsList()) {

            if (category.matches(item)) {

                return category;

            }

        }

        return null;

    }



    public static boolean canRevealItem(PlayerData playerData, ItemStack item) {
        if (ClueChecker.isClueItem(item)) {
            return false;
        }
        if (BundleHandler.isBundle(item)) {
            return BundleHandler.canRevealBundle(playerData, item);
        }

        ItemCategory category = resolveCategory(item);

        if (category == null) return true;

        return playerData.isCategoryActive(category.getId());
    }



    public static Set<String> getActiveCategoryIds(PlayerData playerData) {

        return playerData.getActiveCategories().stream().collect(Collectors.toSet());

    }



    public static double getPerItemValue(ItemStack item) {

        if (item == null || item.getType().isAir()) return 0;
        if (ClueChecker.isClueItem(item)) return 0;

        ItemCategory category = resolveCategory(item);

        if (category == null) return Cache.defaultValue;

        return category.getWeightFor(item);

    }



    public static double getTotalValue(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;

        if (BundleHandler.isBundle(item)) {
            return BundleHandler.getContentsValue(item);
        }

        return getPerItemValue(item) * item.getAmount();
    }

}

