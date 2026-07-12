package net.tfminecraft.thievery.util;

import net.tfminecraft.thievery.data.PlayerData;

public final class StealGuiTitle {

    private StealGuiTitle() {}

    public record TitleOptions(Double risk, Double critical, Long timerMs, StealBudget budget, Double breakChance) {}

    public static String format(TitleOptions opts) {
        StringBuilder title = new StringBuilder();

        if (opts.risk() != null && opts.critical() != null) {
            String riskPart = RiskCalculator.formatRiskTitle(opts.risk(), opts.critical());
            if (!riskPart.isEmpty()) {
                title.append(riskPart);
            }
        }

        if (opts.breakChance() != null) {
            if (title.length() > 0) {
                title.append(" ");
            }
            title.append(ThieveryTexts.ACCENT).append("Break: ")
                    .append(RiskCalculator.formatPercentWhole(opts.breakChance()));
        }

        if (opts.timerMs() != null) {
            if (title.length() > 0) {
                title.append(" ");
            }
            title.append(ThieveryTexts.WARN).append(StealItemDisplay.formatTimeRemaining(opts.timerMs()));
        }

        if (opts.budget() != null) {
            if (title.length() > 0) {
                title.append(" ");
            }
            title.append(ThieveryTexts.SUCCESS)
                    .append(formatTitleValue(opts.budget().getUsed()))
                    .append("/")
                    .append(formatTitleValue(opts.budget().getCapacity()));
        }

        if (title.length() == 0) {
            return " ";
        }
        String raw = title.toString();
        return raw.contains("#") ? ThieveryTexts.format(raw) : raw;
    }

    public static String forPickpocket(PlayerData thiefData, int dexterity, StealBudget budget) {
        return format(new TitleOptions(
                thiefData.getRisk(),
                thiefData.getCriticalChance(dexterity, 0),
                null,
                budget,
                null));
    }

    public static String forChest(PlayerData thiefData, int dexterity, double lockpickStrength, StealBudget budget,
            double successChance, boolean lockpickBroken) {
        Double breakChance = lockpickBroken ? null : (1.0 - successChance);
        return format(new TitleOptions(
                thiefData.getRisk(),
                thiefData.getCriticalChance(dexterity, lockpickStrength),
                null,
                budget,
                breakChance));
    }

    public static String forRobbery(long remainingMs, StealBudget budget) {
        return format(new TitleOptions(null, null, remainingMs, budget, null));
    }

    private static String formatTitleValue(double value) {
        return String.valueOf(Math.round(value));
    }
}
