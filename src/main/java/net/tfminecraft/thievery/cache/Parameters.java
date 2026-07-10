package net.tfminecraft.thievery.cache;

/**
 * Central tuning parameters for the door locking and lockpicking system.
 */
public class Parameters {

    public static double lockpickMaxReduction = 0.5;

    public static long lockpickFailCooldownMs = 60_000L;

    public static int barLength = 20;
    public static int maxSuccessSlots = 3;
    public static int minBreakSlots = 3;
    public static int maxBreakSlots = 19;

    public static double baseBarSpeed = 2.5;
    public static double dexSpeedReductionPerLevel = 0.02;
    public static double minBarSpeed = 0.4;
    public static double speedJitterFraction = 0.3;
    public static double randomFlipChance = 0.03;

    public static String lockpickAttribute = "dexterity";

    public static double chestBaseChance = 0.5;

    public static double doorMaxDistance = 3.0;

    public static long doorUnlockWindowMs = 60L * 60L * 1000L;
}
