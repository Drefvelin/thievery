package net.tfminecraft.thievery.util;

import net.tfminecraft.thievery.cache.Parameters;

public final class LockpickChance {

    private LockpickChance() {}

    public static double computeSuccessChance(int dexterity, double lockpickStrength, double baseChance) {
        double strength = Math.min(1.0, Math.max(0.0, lockpickStrength));
        double value = baseChance * DexterityLerp.getValue(dexterity) * strength;
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
}
