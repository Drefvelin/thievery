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

    public static double computeValueRisk(double stolenValue) {
        if (stolenValue <= 0 || Cache.takeValueScale <= 0) {
            return 0.0;
        }
        double factor = 1.0 - Math.exp(-stolenValue / Cache.takeValueScale);
        return Cache.takeValueMaxBonus * factor;
    }

    public static double computeTakeClueChanceRaw(double sessionRisk, double stolenValue) {
        double valueRisk = computeValueRisk(stolenValue);
        double sessionCap = computeTakeSessionCap();
        return clamp01(clamp01(sessionRisk) * sessionCap + valueRisk);
    }

    public static double computeTakeSessionCap() {
        return 1.0 - Cache.takeValueMaxBonus;
    }

    public static double computeTakeClueChance(double sessionRisk, double stolenValue) {
        double chance = computeTakeClueChanceRaw(sessionRisk, stolenValue);
        if (Cache.takeClueDivisor > 1.0) {
            chance /= Cache.takeClueDivisor;
        }
        return clamp01(chance);
    }

    public record TakeCluePreview(double clueChance, double criticalOnTake) {}

    public static TakeCluePreview computeTakeCluePreview(double sessionRisk, double stolenValue,
            int dexterity, double lockpickStrength) {
        return computeTakeCluePreview(sessionRisk, stolenValue, dexterity, lockpickStrength, false);
    }

    public static TakeCluePreview computeTakeCluePreview(double sessionRisk, double stolenValue,
            int dexterity, double lockpickStrength, boolean guaranteedClue) {
        double criticalIfClue = computeCritical(sessionRisk, dexterity, lockpickStrength);
        if (guaranteedClue) {
            return new TakeCluePreview(1.0, criticalIfClue);
        }
        double clueChance = computeTakeClueChance(sessionRisk, stolenValue);
        return new TakeCluePreview(clueChance, clueChance * criticalIfClue);
    }

    public static String formatPercent(double value) {
        return String.format("%.2f%%", value * 100.0);
    }

    public static String formatPercentWhole(double value) {
        return Math.round(value * 100.0) + "%";
    }

    public static List<String> formatRiskLore(double risk, double critical) {
        List<String> lore = new ArrayList<>();
        if (risk > 0) {
            lore.add(ThieveryTexts.format(ThieveryTexts.ERROR + "Risk: " + formatPercent(risk)));
        }
        if (critical > 0) {
            lore.add(ThieveryTexts.format(ThieveryTexts.CRITICAL + "Critical: " + formatPercent(critical)));
        }
        return lore;
    }

    public static String formatRiskTitle(double risk, double critical) {
        StringBuilder title = new StringBuilder();
        if (risk > 0) {
            title.append(ThieveryTexts.ERROR).append("Risk: ").append(formatPercentWhole(risk));
        }
        if (critical > 0) {
            if (title.length() > 0) {
                title.append(" ");
            }
            title.append(ThieveryTexts.CRITICAL).append("Crit: ").append(formatPercentWhole(critical));
        }
        return title.length() > 0 ? ThieveryTexts.format(title.toString()) : "";
    }

    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }
}
