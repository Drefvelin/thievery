package net.tfminecraft.thievery.steal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.category.CategoryHandler;
import net.tfminecraft.thievery.category.ItemValue;
import net.tfminecraft.thievery.player.PlayerData;

public final class DisplayLoot {

    private static final ThreadLocal<Boolean> DUMPING = ThreadLocal.withInitial(() -> false);

    public interface DisplaySlot {
        ItemStack get();

        boolean take(ItemStack taken);
    }

    private DisplayLoot() {}

    public static boolean isDumping() {
        return Boolean.TRUE.equals(DUMPING.get());
    }

    public static boolean isEligible(ItemStack item, PlayerData thiefData, double remaining) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (ItemValue.isBundle(item)) {
            if (!ItemValue.hasStealableContents(thiefData, item, remaining)
                    && !CategoryHandler.canRevealItem(thiefData, item)) {
                return false;
            }
        } else if (!CategoryHandler.canRevealItem(thiefData, item)) {
            return false;
        }
        return StealBudget.computeTakeableAmount(item, remaining) > 0;
    }

    public static boolean hasAnything(List<DisplaySlot> slots, PlayerData thiefData, double capacity) {
        if (slots == null || thiefData == null) {
            return false;
        }
        for (DisplaySlot slot : slots) {
            if (isEligible(slot.get(), thiefData, capacity)) {
                return true;
            }
        }
        return false;
    }

    public static void dump(Player player, List<DisplaySlot> slots, StealBudget budget, PlayerData thiefData) {
        if (player == null || slots == null || budget == null || thiefData == null) {
            return;
        }
        List<DisplaySlot> order = new ArrayList<>(slots);
        Collections.shuffle(order);
        DUMPING.set(true);
        try {
            for (DisplaySlot slot : order) {
                ItemStack current = slot.get();
                if (!isEligible(current, thiefData, budget.getRemaining())) {
                    continue;
                }
                int takeable = StealBudget.computeTakeableAmount(current, budget.getRemaining());
                if (takeable <= 0) {
                    continue;
                }
                ItemStack toGive = current.clone();
                toGive.setAmount(takeable);
                HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(toGive);
                int leftoverAmount = 0;
                for (ItemStack leftover : leftovers.values()) {
                    if (leftover != null) {
                        leftoverAmount += leftover.getAmount();
                    }
                }
                int added = takeable - leftoverAmount;
                if (added <= 0) {
                    continue;
                }
                ItemStack taken = current.clone();
                taken.setAmount(added);
                if (!slot.take(taken)) {
                    HashMap<Integer, ItemStack> rollback = player.getInventory().removeItem(taken);
                    if (!rollback.isEmpty()) {
                        for (ItemStack extra : rollback.values()) {
                            player.getInventory().addItem(extra);
                        }
                    }
                    continue;
                }
                budget.addUsed(CategoryHandler.getTotalValue(taken));
            }
        } finally {
            DUMPING.remove();
        }
    }
}
