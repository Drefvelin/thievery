package net.tfminecraft.thievery.util;

import org.bukkit.entity.Player;

public final class PlayerProximityAnchor implements ProximityAnchor {

    private final Player pickpocket;
    private final Player victim;
    private final double maxDistance;

    public PlayerProximityAnchor(Player pickpocket, Player victim, double maxDistance) {
        this.pickpocket = pickpocket;
        this.victim = victim;
        this.maxDistance = maxDistance;
    }

    @Override
    public boolean isInRange(Player actor) {
        if (victim == null || !victim.isOnline()) {
            return false;
        }
        return RobberyUtil.isWithinRange(pickpocket, victim, maxDistance);
    }

    @Override
    public void onOutOfRange(Player actor) {
        actor.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Pickpocketing cancelled — your target moved too far away."));
    }

    public Player getVictim() {
        return victim;
    }
}
