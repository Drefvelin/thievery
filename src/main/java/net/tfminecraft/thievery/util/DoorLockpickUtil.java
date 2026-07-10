package net.tfminecraft.thievery.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class DoorLockpickUtil {

    private DoorLockpickUtil() {}

    public static Location getDoorCenter(Location doorCanonical) {
        return doorCanonical.clone().add(0.5, 0.5, 0.5);
    }

    public static boolean isWithinDoorRange(Player player, Location doorCanonical, double maxDistance) {
        return player.getLocation().distance(getDoorCenter(doorCanonical)) <= maxDistance;
    }
}
