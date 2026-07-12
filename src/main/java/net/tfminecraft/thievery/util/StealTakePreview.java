package net.tfminecraft.thievery.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.data.PlayerData;

public final class StealTakePreview {

    public record TakeValues(double valueOne, double valueAll, int amountAll, int maxByBudget, int maxFit) {}

    private static final TakeValues EMPTY = new TakeValues(0, 0, 0, 0, 0);

    private StealTakePreview() {}

    public static TakeValues estimate(Player player, PlayerData thiefData, ItemStack realItem,
            double budgetRemaining) {
        if (realItem == null || realItem.getType().isAir()) {
            return EMPTY;
        }
        if (BundleHandler.isBundle(realItem)) {
            return new TakeValues(
                    BundleHandler.estimateOneTakeValue(realItem, thiefData, budgetRemaining),
                    BundleHandler.estimateGreedyTakeValue(realItem, player, thiefData, budgetRemaining),
                    0, 0, 0);
        }

        double perItem = CategoryResolver.getPerItemValue(realItem);
        double valueOne = perItem;

        int maxByBudget = StealBudget.computeTakeableAmount(realItem, budgetRemaining);
        if (maxByBudget <= 0) {
            return new TakeValues(valueOne, 0, 0, maxByBudget, 0);
        }
        int maxFit = StealTakeHandler.maxFitInPlayerInventory(player, realItem, maxByBudget);
        int amountAll = Math.min(realItem.getAmount(), Math.min(maxByBudget, maxFit));
        double valueAll = perItem > 0 ? perItem * amountAll : amountAll;

        return new TakeValues(valueOne, valueAll, amountAll, maxByBudget, maxFit);
    }
}
