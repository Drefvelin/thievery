package net.tfminecraft.thievery.steal.session;

import java.util.HashSet;
import java.util.Set;

import net.tfminecraft.thievery.steal.StealBudget;
import net.tfminecraft.thievery.steal.StealGui;

public class HiddenStealSession extends StealSession {

    private final Set<Integer> revealedGuiSlots = new HashSet<>();
    private final String targetKey;

    public HiddenStealSession(StealBudget budget, StealGui.Layout layout, String targetKey) {
        super(budget, layout);
        this.targetKey = targetKey;
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
}
