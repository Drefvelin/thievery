package net.tfminecraft.thievery.door;

/** Multipliers on the chest steal bases. Omitted config keys stay at these identity values. */
public record LockTypeProfile(
        double budgetMultiplier,
        double riskMultiplier,
        boolean criticalRisk,
        double breakChanceMultiplier) {

    public static final LockTypeProfile IDENTITY = new LockTypeProfile(1.0, 1.0, true, 1.0);

    public LockTypeProfile {
        budgetMultiplier = Math.max(0.0, budgetMultiplier);
        riskMultiplier = Math.max(0.0, riskMultiplier);
        breakChanceMultiplier = Math.max(0.0, breakChanceMultiplier);
    }
}
