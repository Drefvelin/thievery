package net.tfminecraft.thievery.cache;

/**
 * Central tuning parameters for the door locking and lockpicking system.
 * Edit these values to adjust difficulty without touching logic code.
 */
public class Parameters {

    // -------------------------------------------------------------------------
    // Key defaults
    // -------------------------------------------------------------------------

    /** Strength assigned to a key that carries no keyStrength PDC tag (0–1). */
    public static double defaultKeyStrength = 1.0;

    // -------------------------------------------------------------------------
    // Lockpick defaults
    // -------------------------------------------------------------------------

    /** Strength assigned to a lockpick that carries no lockpickStrength PDC tag (0–1). */
    public static double defaultLockpickStrength = 0.0;

    /**
     * Maximum reduction a lockpick can apply to the effective lock strength (0–1).
     * 0.5 means a lockpick at full strength halves the lock's effective strength.
     */
    public static double lockpickMaxReduction = 0.5;

    // -------------------------------------------------------------------------
    // Lockpick fail cooldown
    // -------------------------------------------------------------------------

    /** Cooldown in milliseconds applied on FAIL or BREAK result. */
    public static long lockpickFailCooldownMs = 60_000L;

    // -------------------------------------------------------------------------
    // Minigame bar layout
    // -------------------------------------------------------------------------

    /** Total number of slots in the lockpick bar. */
    public static int barLength = 20;

    /**
     * Maximum number of success slots at strength 0 (easiest lock).
     * Scales linearly down to a minimum of 1 at strength 1.
     * Target: 3 success slots at strength 0.1
     */
    public static int maxSuccessSlots = 3;

    /**
     * Number of break slots at strength 0 (floor).
     * Target: 5 break slots at strength 0.1, 19 at strength 1.
     */
    public static int minBreakSlots = 3;

    /**
     * Number of break slots at strength 1 (ceiling).
     * At strength 1 with 1 success slot this fills the rest of the bar.
     */
    public static int maxBreakSlots = 19;

    // -------------------------------------------------------------------------
    // Bar movement speed
    // -------------------------------------------------------------------------

    /**
     * Base bar speed in slots per tick at dexterity 0.
     * Higher = harder.
     */
    public static double baseBarSpeed = 2.5;

    /**
     * Speed reduction per dexterity level as a fraction of baseBarSpeed.
     * 0.02 = 2% slower per level regardless of base speed.
     * e.g. dex 5 at base 2.5 => 2.5 * (1 - 5*0.02) = 2.25 (10% slower)
     */
    public static double dexSpeedReductionPerLevel = 0.02;

    /** Minimum bar speed regardless of dexterity (prevents the bar from stopping). */
    public static double minBarSpeed = 0.4;

    /**
     * Max random speed jitter added/subtracted each tick (as a fraction of baseBarSpeed).
     * 0.3 means the bar can vary ±30% of base each tick, making timing harder.
     */
    public static double speedJitterFraction = 0.3;

    /**
     * Probability per tick that the bar randomly reverses direction.
     * 0.03 = 3% chance each tick, making the movement erratic.
     */
    public static double randomFlipChance = 0.03;

    // -------------------------------------------------------------------------
    // Attribute
    // -------------------------------------------------------------------------

    /** MMOCore attribute ID used for lockpick speed/success scaling. */
    public static String lockpickAttribute = "dexterity";

    // -------------------------------------------------------------------------
    // Chest lockpicking — strength scaling
    // -------------------------------------------------------------------------

    /**
     * Maximum bonus added to the item reveal success rate when lockpick strength = 1.
     * Scales linearly with strength. At strength 0 no bonus is applied.
     */
    public static double chestStrengthSuccessBonus = 0.2;

    /**
     * Maximum reduction applied to the break chance per tick when lockpick strength = 1.
     * Scales linearly with strength. At strength 0 no reduction is applied.
     */
    public static double chestStrengthBreakReduction = 0.08;
}
