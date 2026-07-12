package net.tfminecraft.thievery.data;

import java.util.UUID;

import net.tfminecraft.thievery.util.StealBudget;
import net.tfminecraft.thievery.util.StealGuiLayout;

public class RobberySession {

    public enum State {
        PENDING_ACCEPT,
        ACTIVE
    }

    private final UUID robberId;
    private final UUID victimId;
    private State state;
    private final StealBudget budget;
    private final StealGuiLayout layout;
    private long acceptDeadlineMs;
    private long activeEndMs;

    public RobberySession(UUID robberId, UUID victimId, StealBudget budget, StealGuiLayout layout) {
        this.robberId = robberId;
        this.victimId = victimId;
        this.budget = budget;
        this.layout = layout;
        this.state = State.PENDING_ACCEPT;
    }

    public UUID getRobberId() {
        return robberId;
    }

    public UUID getVictimId() {
        return victimId;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public StealBudget getBudget() {
        return budget;
    }

    public StealGuiLayout getLayout() {
        return layout;
    }

    public long getAcceptDeadlineMs() {
        return acceptDeadlineMs;
    }

    public void setAcceptDeadlineMs(long acceptDeadlineMs) {
        this.acceptDeadlineMs = acceptDeadlineMs;
    }

    public long getActiveEndMs() {
        return activeEndMs;
    }

    public void setActiveEndMs(long activeEndMs) {
        this.activeEndMs = activeEndMs;
    }

    public int getGuiSize() {
        return layout.getGuiSize();
    }
}
