package net.tfminecraft.thievery.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import net.tfminecraft.thievery.cache.Cache;

public final class RiskCalculator {

    private RiskCalculator() {}

    public static double computeGain(int dexterity, double lockpickStrength) {
        return computeGain(dexterity, lockpickStrength, Cache.riskGainDoorMin, Cache.riskGainDoorMax);
    }

    public static double computeGain(int dexterity, double lockpickStrength, double min, double max) {
        double raw = min + ThreadLocalRandom.current().nextDouble() * (max - min);
        double dexFactor = 1.0 / (1.0 + dexterity / 40.0);
        double strength = clamp01(lockpickStrength);
        double pickFactor = 1.0 - strength * Cache.riskPickReduction;
        return raw * dexFactor * pickFactor;
    }

    public static double computeDecay(int dexterity, long lastDecayMs, long nowMs) {
        if (lastDecayMs <= 0 || nowMs <= lastDecayMs) return 0.0;
        double hours = (nowMs - lastDecayMs) / 3_600_000.0;
        return Cache.riskDecayPerHour * hours * (1.0 + dexterity / 40.0);
    }

    public static double computeCritical(double risk, int dexterity, double lockpickStrength) {
        double strength = clamp01(lockpickStrength);
        double critical = Cache.criticalBase
                + risk * Cache.criticalRiskWeight
                - (dexterity / 40.0) * Cache.criticalDexReduction
                - strength * Cache.criticalStrengthReduction;
        return clamp01(critical);
    }

    public static String formatPercent(double value) {
        double percent = value * 100.0;
        if (Math.abs(percent - Math.rint(percent)) < 0.05) {
            return (long) Math.rint(percent) + "%";
        }
        return String.format("%.1f%%", percent);
    }

    public static List<String> formatRiskLore(double risk, double critical) {
        List<String> lore = new ArrayList<>();
        if (risk > 0) {
            lore.add("§cRisk: " + formatPercent(risk));
        }
        if (critical > 0) {
            lore.add("§4Critical: " + formatPercent(critical));
        }
        return lore;
    }

    public static String formatRiskTitle(double risk, double critical) {
        StringBuilder title = new StringBuilder();
        if (risk > 0) {
            title.append("§cRisk: ").append(formatPercent(risk));
        }
        if (critical > 0) {
            if (title.length() > 0) {
                title.append(" ");
            }
            title.append("§4Critical: ").append(formatPercent(critical));
        }
        return title.toString();
    }

    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }
}
