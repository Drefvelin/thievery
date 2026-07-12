package net.tfminecraft.thievery.util;

import org.bukkit.entity.Player;

public interface ProximityAnchor {

    boolean isInRange(Player actor);

    void onOutOfRange(Player actor);
}
