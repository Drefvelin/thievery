package net.tfminecraft.thievery.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.tfminecraft.thievery.cache.Parameters;

public final class DoorProximityAnchor implements ProximityAnchor {

    private final Location doorLocation;

    public DoorProximityAnchor(Location doorLocation) {
        this.doorLocation = doorLocation;
    }

    @Override
    public boolean isInRange(Player actor) {
        return DoorLockpickUtil.isWithinDoorRange(actor, doorLocation, Parameters.doorMaxDistance);
    }

    @Override
    public void onOutOfRange(Player actor) {
        actor.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Lockpicking cancelled — you moved too far from the door."));
    }

    public Location getDoorLocation() {
        return doorLocation;
    }
}
