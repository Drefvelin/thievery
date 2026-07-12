package net.tfminecraft.thievery.util;

import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.data.ItemCategory;
import net.tfminecraft.thievery.data.PlayerData;
import net.tfminecraft.thievery.loader.CategoryLoader;

public final class CategoryResolver {

    private CategoryResolver() {}

    public static ItemCategory resolveCategory(ItemStack item) {
        return CategoryMatcher.resolveFirstMatch(item);
    }

    public static boolean canRevealItem(PlayerData playerData, ItemStack item) {
        if (ClueChecker.isClueItem(item)) {
            return false;
        }
        if (BundleHandler.isBundle(item)) {
            return BundleHandler.canRevealBundle(playerData, item);
        }
        return CategoryMatcher.matchesAnyActive(playerData, item);
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
        return ValueResolver.compute(item);
    }

    public static double getTotalValue(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }

        if (BundleHandler.isBundle(item)) {
            return BundleHandler.getContentsValue(item);
        }

        return getPerItemValue(item) * item.getAmount();
    }
}
