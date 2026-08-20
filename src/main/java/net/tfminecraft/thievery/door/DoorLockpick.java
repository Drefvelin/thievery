package net.tfminecraft.thievery.door;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.tfminecraft.thievery.cache.Parameters;
import net.tfminecraft.thievery.player.RiskCalculator;
import net.tfminecraft.thievery.utils.ThieveryTexts;

public final class DoorLockpick {

    private DoorLockpick() {}

    public static Location getDoorCenter(Location doorCanonical) {
        return doorCanonical.clone().add(0.5, 0.5, 0.5);
    }

    public static boolean isWithinDoorRange(Player player, Location doorCanonical, double maxDistance) {
        return player.getLocation().distance(getDoorCenter(doorCanonical)) <= maxDistance;
    }

    public static double computeSuccessChance(int dexterity, double lockpickStrength, double baseChance) {
        double strength = Math.min(1.0, Math.max(0.0, lockpickStrength));
        double value = baseChance * RiskCalculator.getDexterityLerpValue(dexterity) * strength;
        return Math.min(Parameters.maxSuccessChance, Math.max(0.0, value));
    }

    /** Break chance for the given reveal attempt, scaled by ramp (1st = 1×step, 2nd = 2×step, … capped at 1.0). */
    public static double computeRampedBreakChance(double baseSuccessChance, int revealAttempt, double rampPerSlot) {
        double baseBreak = Math.max(0.0, 1.0 - baseSuccessChance);
        int attempt = Math.max(1, revealAttempt);
        double multiplier = Math.min(1.0, attempt * rampPerSlot);
        return Math.min(1.0, baseBreak * multiplier);
    }

    public static double computeRampedSuccessChance(double baseSuccessChance, int revealAttempt, double rampPerSlot) {
        return 1.0 - computeRampedBreakChance(baseSuccessChance, revealAttempt, rampPerSlot);
    }

    public interface ProximityAnchor {

        boolean isInRange(Player actor);

        void onOutOfRange(Player actor);
    }

    public static final class DoorProximityAnchor implements ProximityAnchor {

        private final Location doorLocation;

        public DoorProximityAnchor(Location doorLocation) {
            this.doorLocation = doorLocation;
        }

        @Override
        public boolean isInRange(Player actor) {
            return isWithinDoorRange(actor, doorLocation, Parameters.doorMaxDistance);
        }

        @Override
        public void onOutOfRange(Player actor) {
            actor.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Lockpicking cancelled - you moved too far from the door."));
        }

        public Location getDoorLocation() {
            return doorLocation;
        }
    }
}
