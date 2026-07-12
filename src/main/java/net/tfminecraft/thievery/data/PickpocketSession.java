package net.tfminecraft.thievery.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.tfminecraft.thievery.util.StealBudget;
import net.tfminecraft.thievery.util.StealGuiLayout;
import net.tfminecraft.thievery.util.TargetKeyResolver;

public class PickpocketSession {

    private final UUID pickpocketId;
    private final UUID victimId;
    private final StealBudget budget;
    private final StealGuiLayout layout;
    private final Set<Integer> revealedGuiSlots = new HashSet<>();
    private final String targetKey;

    public PickpocketSession(UUID pickpocketId, UUID victimId, StealBudget budget, StealGuiLayout layout) {
        this.pickpocketId = pickpocketId;
        this.victimId = victimId;
        this.budget = budget;
        this.layout = layout;
        this.targetKey = TargetKeyResolver.resolve(victimId);
    }

    public UUID getPickpocketId() {
        return pickpocketId;
    }

    public UUID getVictimId() {
        return victimId;
    }

    public StealBudget getBudget() {
        return budget;
    }

    public StealGuiLayout getLayout() {
        return layout;
    }

    public String getTargetKey() {
        return targetKey;
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

    public int getGuiSize() {
        return layout.getGuiSize();
    }
}
