package net.tfminecraft.thievery.steal;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class RobberyUtil {

    private RobberyUtil() {}

    public static boolean isWithinRange(Player a, Player b, double maxDistance) {
        if (a == null || b == null || !a.getWorld().equals(b.getWorld())) {
            return false;
        }
        if (maxDistance < 0) {
            return true;
        }
        Location locA = a.getLocation();
        Location locB = b.getLocation();
        return locA.distanceSquared(locB) <= maxDistance * maxDistance;
    }
}
