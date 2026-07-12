package net.tfminecraft.thievery.player;

import java.util.UUID;

import net.tfminecraft.thievery.steal.session.HiddenStealSession;
import net.tfminecraft.thievery.steal.StealBudget;
import net.tfminecraft.thievery.steal.StealGui;
import net.tfminecraft.thievery.player.TargetKeyResolver;

public class PickpocketSession extends HiddenStealSession {

    private final UUID pickpocketId;
    private final UUID victimId;

    public PickpocketSession(UUID pickpocketId, UUID victimId, StealBudget budget, StealGui.Layout layout) {
        super(budget, layout, TargetKeyResolver.resolve(victimId));
        this.pickpocketId = pickpocketId;
        this.victimId = victimId;
    }

    public UUID getPickpocketId() {
        return pickpocketId;
    }

    public UUID getVictimId() {
        return victimId;
    }
}
