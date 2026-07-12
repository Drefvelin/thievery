package net.tfminecraft.thievery.util;

import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.data.PlayerData;

public final class StealGuiReveal {

    public enum ResultType {
        EMPTY,
        IGNORED,
        ITEM
    }

    public record Result(ResultType type, ItemStack display) {}

    private StealGuiReveal() {}

    public static Result revealSlot(ItemStack realItem, StealBudget budget, PlayerData thiefData) {
        return revealSlot(realItem, budget, thiefData, null);
    }

    public static Result revealSlot(ItemStack realItem, StealBudget budget, PlayerData thiefData,
            StealItemDisplay.ChestCluePreviewContext cluePreview) {
        if (realItem == null || realItem.getType().isAir()) {
            return new Result(ResultType.EMPTY, StealGuiPanes.createNothingPane());
        }
        if (StealIgnoreRules.isIgnored(realItem)) {
            return new Result(ResultType.IGNORED, StealGuiPanes.createNothingPane());
        }
        ItemStack display = StealItemDisplay.buildRepresentation(realItem, budget.getRemaining(), thiefData,
                cluePreview);
        if (display == null) {
            return new Result(ResultType.EMPTY, StealGuiPanes.createNothingPane());
        }
        return new Result(ResultType.ITEM, display);
    }
}
