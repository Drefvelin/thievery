package net.tfminecraft.thievery.robbery;

import java.util.UUID;

import net.tfminecraft.thievery.steal.session.StealSession;
import net.tfminecraft.thievery.steal.StealBudget;
import net.tfminecraft.thievery.steal.StealGui;

public class RobberySession extends StealSession {

    public enum State {
        PENDING_ACCEPT,
        ACTIVE
    }

    private final UUID robberId;
    private final UUID victimId;
    private State state;
    private long acceptDeadlineMs;
    private long activeEndMs;

    public RobberySession(UUID robberId, UUID victimId, StealBudget budget, StealGui.Layout layout) {
        super(budget, layout);
        this.robberId = robberId;
        this.victimId = victimId;
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
}
