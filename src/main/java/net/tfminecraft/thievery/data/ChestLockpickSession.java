package net.tfminecraft.thievery.data;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;

import net.tfminecraft.thievery.cache.Parameters;
import net.tfminecraft.thievery.util.LockpickChance;
import net.tfminecraft.thievery.util.StealBudget;
import net.tfminecraft.thievery.util.StealGuiLayout;

public class ChestLockpickSession {

    private final Block chestBlock;
    private final LockpickDefinition lockpickDef;
    private final double successChance;
    private final StealGuiLayout layout;
    private final Set<Integer> revealedGuiSlots = new HashSet<>();
    private final StealBudget budget;
    private int successfulClueDrops;
    private final String targetKey;
    private boolean lockpickBroken;

    public ChestLockpickSession(Block chestBlock, LockpickDefinition lockpickDef, double successChance,
            Inventory chestInventory, String targetKey) {
        this.chestBlock = chestBlock;
        this.lockpickDef = lockpickDef;
        this.successChance = successChance;
        this.layout = StealGuiLayout.create(chestInventory.getSize());
        this.budget = new StealBudget(lockpickDef.getCapacity());
        this.targetKey = targetKey;
    }

    public static double computeSuccessChance(int dexterity, double lockpickStrength) {
        return LockpickChance.computeSuccessChance(dexterity, lockpickStrength, Parameters.chestBaseSuccessChance);
    }

    public boolean isRevealed(int guiSlot) {
        return revealedGuiSlots.contains(guiSlot);
    }

    public void markRevealed(int guiSlot) {
        revealedGuiSlots.add(guiSlot);
    }

    public Set<Integer> getRevealedGuiSlots() {
        return new HashSet<>(revealedGuiSlots);
    }

    public Set<Integer> getRevealedChestSlots() {
        Set<Integer> chestSlots = new HashSet<>();
        for (int guiSlot : revealedGuiSlots) {
            Integer chestSlot = layout.getLogicalForGui(guiSlot);
            if (chestSlot != null) {
                chestSlots.add(chestSlot);
            }
        }
        return chestSlots;
    }

    public double getCapacityRemaining() {
        return budget.getRemaining();
    }

    public void addCapacityUsed(double value) {
        budget.addUsed(value);
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

    public int getNextRevealAttempt() {
        return revealedGuiSlots.size() + 1;
    }

    public double getNextRevealBreakChance() {
        return LockpickChance.computeRampedBreakChance(successChance, getNextRevealAttempt(),
                Parameters.chestBreakChanceRampPerSlot);
    }

    public double getNextRevealSuccessChance() {
        return LockpickChance.computeRampedSuccessChance(successChance, getNextRevealAttempt(),
                Parameters.chestBreakChanceRampPerSlot);
    }

    public StealGuiLayout getLayout() {
        return layout;
    }

    public StealBudget getBudget() {
        return budget;
    }

    public int getGuiSize() {
        return layout.getGuiSize();
    }

    public String getTargetKey() {
        return targetKey;
    }

    public boolean isLockpickBroken() {
        return lockpickBroken;
    }

    public void markLockpickBroken() {
        lockpickBroken = true;
    }
}
