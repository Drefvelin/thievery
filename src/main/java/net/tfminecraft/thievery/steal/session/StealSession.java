package net.tfminecraft.thievery.steal.session;

import net.tfminecraft.thievery.steal.StealBudget;
import net.tfminecraft.thievery.steal.StealGui;

public abstract class StealSession {

    private final StealBudget budget;
    private final StealGui.Layout layout;

    protected StealSession(StealBudget budget, StealGui.Layout layout) {
        this.budget = budget;
        this.layout = layout;
    }

    public StealBudget getBudget() {
        return budget;
    }

    public StealGui.Layout getLayout() {
        return layout;
    }

    public int getGuiSize() {
        return layout.getGuiSize();
    }
}
