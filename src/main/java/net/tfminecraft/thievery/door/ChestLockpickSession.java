package net.tfminecraft.thievery.door;

import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;

import net.tfminecraft.thievery.cache.Parameters;
import net.tfminecraft.thievery.player.LockpickDefinition;
import net.tfminecraft.thievery.steal.session.HiddenStealSession;
import net.tfminecraft.thievery.door.DoorLockpick;
import net.tfminecraft.thievery.steal.StealBudget;
import net.tfminecraft.thievery.steal.StealGui;

public class ChestLockpickSession extends HiddenStealSession {

    private final UUID thiefId;
    private final Block chestBlock;
    private final LockpickDefinition lockpickDef;
    private final double successChance;
    private int successfulClueDrops;
    private boolean lockpickBroken;

    public ChestLockpickSession(UUID thiefId, Block chestBlock, LockpickDefinition lockpickDef, double successChance,
            Inventory chestInventory, String targetKey) {
        super(new StealBudget(lockpickDef.getCapacity()), StealGui.Layout.create(chestInventory.getSize()), targetKey);
        this.thiefId = thiefId;
        this.chestBlock = chestBlock;
        this.lockpickDef = lockpickDef;
        this.successChance = successChance;
    }

    public UUID getThiefId() {
        return thiefId;
    }

    public static double computeSuccessChance(int dexterity, double lockpickStrength) {
        return DoorLockpick.computeSuccessChance(dexterity, lockpickStrength, Parameters.chestBaseSuccessChance);
    }

    public java.util.Set<Integer> getRevealedChestSlots() {
        java.util.Set<Integer> chestSlots = new java.util.HashSet<>();
        for (int guiSlot : getRevealedGuiSlots()) {
            Integer chestSlot = getLayout().getLogicalForGui(guiSlot);
            if (chestSlot != null) {
                chestSlots.add(chestSlot);
            }
        }
        return chestSlots;
    }

    public double getCapacityRemaining() {
        return getBudget().getRemaining();
    }

    public void addCapacityUsed(double value) {
        getBudget().addUsed(value);
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
        return getRevealedGuiSlots().size() + 1;
    }

    public double getNextRevealBreakChance() {
        return DoorLockpick.computeRampedBreakChance(successChance, getNextRevealAttempt(),
                Parameters.chestBreakChanceRampPerSlot);
    }

    public double getNextRevealSuccessChance() {
        return DoorLockpick.computeRampedSuccessChance(successChance, getNextRevealAttempt(),
                Parameters.chestBreakChanceRampPerSlot);
    }

    public boolean isLockpickBroken() {
        return lockpickBroken;
    }

    public void markLockpickBroken() {
        lockpickBroken = true;
    }
}
