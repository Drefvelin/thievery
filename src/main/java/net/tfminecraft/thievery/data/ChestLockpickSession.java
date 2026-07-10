package net.tfminecraft.thievery.data;



import java.util.ArrayList;

import java.util.Collections;

import java.util.HashMap;

import java.util.HashSet;

import java.util.LinkedList;

import java.util.List;

import java.util.Map;

import java.util.Queue;

import java.util.Set;



import org.bukkit.block.Block;

import org.bukkit.inventory.Inventory;

import org.bukkit.inventory.ItemStack;



import net.tfminecraft.RPCharacters.Utils.ClueGiver;

import net.tfminecraft.thievery.cache.Parameters;

import net.tfminecraft.thievery.util.CategoryResolver;



public class ChestLockpickSession {



    public static final int SEARCH_GUI_SLOT = 8;

    public static final int MASK_CHEST_SLOT = 8;



    private final Block chestBlock;

    private final LockpickDefinition lockpickDef;

    private final double successChance;

    private final int inventorySize;

    private final Queue<Integer> searchOrder;

    private final int maskProxyChestSlot;

    private final Map<Integer, Integer> guiSlotToChestSlot = new HashMap<>();

    private boolean slot8Revealed;

    private double capacityUsed;
    private int successfulClueDrops;
    private final String targetKey;

    public ChestLockpickSession(Block chestBlock, LockpickDefinition lockpickDef, double successChance,
            Inventory chestInventory, String targetKey) {

        this.chestBlock = chestBlock;

        this.lockpickDef = lockpickDef;

        this.successChance = successChance;

        this.inventorySize = chestInventory.getSize();

        this.maskProxyChestSlot = chooseMaskProxySlot(chestInventory);

        this.searchOrder = buildSearchOrder(chestInventory);

        this.targetKey = targetKey;

    }



    public static double computeSuccessChance(int dexterity, double lockpickStrength) {

        double dexBonus = dexterity / 40.0;

        return Math.min(1.0, Math.max(0.0, Parameters.chestBaseChance * (1.0 + dexBonus) * lockpickStrength));

    }



    public static int chestSlotToGui(int chestSlot) {

        if (chestSlot < SEARCH_GUI_SLOT) return chestSlot;

        return chestSlot + 1;

    }



    public static int computeTakeableAmount(ItemStack realItem, double capacityRemaining) {

        if (realItem == null || realItem.getType().isAir()) return 0;

        if (ClueGiver.isClueItem(realItem)) return 0;

        double perItem = CategoryResolver.getPerItemValue(realItem);

        if (perItem <= 0) return realItem.getAmount();

        int maxByBudget = (int) Math.floor(capacityRemaining / perItem);

        if (maxByBudget <= 0) return 0;

        return Math.min(realItem.getAmount(), maxByBudget);

    }



    public int getRevealGuiSlot(int chestSlot) {

        if (chestSlot == MASK_CHEST_SLOT) {

            return chestSlotToGui(maskProxyChestSlot);

        }

        return chestSlotToGui(chestSlot);

    }



    public boolean hasMoreSearches() {

        return !searchOrder.isEmpty();

    }



    public Integer pollNextChestSlot() {

        return searchOrder.poll();

    }



    public void markRevealed(int chestSlot, int guiSlot) {

        if (chestSlot == MASK_CHEST_SLOT) {

            slot8Revealed = true;

        }

        guiSlotToChestSlot.put(guiSlot, chestSlot);

    }



    public Integer getChestSlotForGui(int guiSlot) {

        return guiSlotToChestSlot.get(guiSlot);

    }



    public Map<Integer, Integer> getGuiSlotMappings() {

        return guiSlotToChestSlot;

    }



    public Set<Integer> getRevealedChestSlots() {

        return new HashSet<>(guiSlotToChestSlot.values());

    }



    public double getCapacityRemaining() {

        return Math.max(0.0, lockpickDef.getCapacity() - capacityUsed);

    }



    public void addCapacityUsed(double value) {

        capacityUsed += value;

    }

    public int getSuccessfulClueDrops() {
        return successfulClueDrops;
    }

    public void incrementSuccessfulClueDrops() {
        successfulClueDrops++;
    }

    public Block getChestBlock() {

        return chestBlock;

    }



    public LockpickDefinition getLockpickDef() {

        return lockpickDef;

    }



    public double getSuccessChance() {

        return successChance;

    }



    public int getInventorySize() {

        return inventorySize;

    }



    public int getMaskProxyChestSlot() {

        return maskProxyChestSlot;

    }



    public boolean isSlot8Revealed() {

        return slot8Revealed;

    }



    public String getTargetKey() {

        return targetKey;

    }



    private static Queue<Integer> buildSearchOrder(Inventory inventory) {

        List<Integer> slots = new ArrayList<>();

        for (int i = 0; i < inventory.getSize(); i++) {

            ItemStack item = inventory.getItem(i);

            if (ClueGiver.isClueItem(item)) {

                continue;

            }

            slots.add(i);

        }

        Collections.shuffle(slots);

        return new LinkedList<>(slots);

    }



    private static int chooseMaskProxySlot(Inventory inventory) {

        int size = inventory.getSize();

        if (size <= MASK_CHEST_SLOT) return 0;



        for (int i = 0; i < size; i++) {

            if (i == MASK_CHEST_SLOT) continue;

            ItemStack item = inventory.getItem(i);

            if (ClueGiver.isClueItem(item)) continue;

            if (item == null || item.getType().isAir()) {

                return i;

            }

        }



        int bestSlot = 0;

        double bestValue = Double.MAX_VALUE;

        for (int i = 0; i < size; i++) {

            if (i == MASK_CHEST_SLOT) continue;

            ItemStack item = inventory.getItem(i);

            if (ClueGiver.isClueItem(item)) continue;

            double value = CategoryResolver.getTotalValue(item);

            if (value < bestValue) {

                bestValue = value;

                bestSlot = i;

            }

        }

        return bestSlot;

    }

}

