package net.tfminecraft.thievery.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.data.PlayerData;
import net.tfminecraft.thievery.holder.StealGuiHolder;

public final class StealGuiBuilder {

    private StealGuiBuilder() {}

    public static Inventory buildHiddenGui(StealGuiHolder holder, StealGuiLayout layout, String title) {
        int guiSize = layout.getGuiSize();
        Inventory gui = Bukkit.createInventory(holder, guiSize, title);
        ItemStack unkPane = StealGuiPanes.createUnknownPane();
        ItemStack fillerPane = StealGuiPanes.createFillerPane();
        Set<Integer> assignedLootSlots = new HashSet<>(layout.getLogicalSlotToGuiSlot().values());

        for (Integer guiSlot : layout.getLogicalSlotToGuiSlot().values()) {
            gui.setItem(guiSlot, unkPane);
        }

        for (int guiSlot = 0; guiSlot < guiSize; guiSlot++) {
            if (!assignedLootSlots.contains(guiSlot)) {
                gui.setItem(guiSlot, fillerPane);
            }
        }
        return gui;
    }

    public static Inventory buildRobberyGui(StealGuiHolder holder, StealGuiLayout layout, String title,
            Player victim, StealBudget budget, PlayerData thiefData) {
        int guiSize = layout.getGuiSize();
        Inventory gui = Bukkit.createInventory(holder, guiSize, title);
        ItemStack fillerPane = StealGuiPanes.createFillerPane();
        Set<Integer> assignedLootSlots = new HashSet<>(layout.getLogicalSlotToGuiSlot().values());

        for (Map.Entry<Integer, Integer> entry : layout.getLogicalSlotToGuiSlot().entrySet()) {
            int logicalSlot = entry.getKey();
            int guiSlot = entry.getValue();
            ItemStack realItem = PlayerSlotMap.getItem(victim, logicalSlot);
            if (realItem == null || realItem.getType().isAir() || StealIgnoreRules.isIgnored(realItem)) {
                continue;
            }
            ItemStack display = StealItemDisplay.buildRepresentation(realItem, budget.getRemaining(), thiefData);
            if (display != null) {
                gui.setItem(guiSlot, display);
            }
        }

        for (int guiSlot = 0; guiSlot < guiSize; guiSlot++) {
            if (!assignedLootSlots.contains(guiSlot) || gui.getItem(guiSlot) == null) {
                if (gui.getItem(guiSlot) == null) {
                    gui.setItem(guiSlot, fillerPane);
                }
            }
        }
        return gui;
    }

    public static void placeRevealedSlot(Inventory gui, int guiSlot, ItemStack realItem,
            StealBudget budget, PlayerData thiefData) {
        placeRevealedSlot(gui, guiSlot, realItem, budget, thiefData, null);
    }

    public static void placeRevealedSlot(Inventory gui, int guiSlot, ItemStack realItem,
            StealBudget budget, PlayerData thiefData, StealItemDisplay.ChestCluePreviewContext cluePreview) {
        StealGuiReveal.Result result = StealGuiReveal.revealSlot(realItem, budget, thiefData, cluePreview);
        gui.setItem(guiSlot, result.display());
    }

}
