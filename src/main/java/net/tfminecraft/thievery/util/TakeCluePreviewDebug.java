package net.tfminecraft.thievery.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.data.ChestLockpickSession;

public final class TakeCluePreviewDebug {

    private TakeCluePreviewDebug() {}

    public static void log(String reason, Player player, ChestLockpickSession session, int guiSlot,
            ItemStack realItem, StealTakePreview.TakeValues values,
            StealItemDisplay.ChestCluePreviewContext cluePreview,
            RiskCalculator.TakeCluePreview one, RiskCalculator.TakeCluePreview all,
            boolean guaranteed) {
        if (!Cache.debugCluePreview) {
            return;
        }

        double perItem = realItem == null || realItem.getType().isAir()
                ? 0.0
                : CategoryResolver.getPerItemValue(realItem);
        String itemLabel = realItem == null || realItem.getType().isAir()
                ? "AIR"
                : realItem.getType().name() + " x" + realItem.getAmount();

        double valueRiskOne = RiskCalculator.computeValueRisk(values.valueOne());
        double valueRiskAll = RiskCalculator.computeValueRisk(values.valueAll());
        double clueRawOne = RiskCalculator.computeTakeClueChanceRaw(
                cluePreview.sessionRisk(), values.valueOne());
        double clueRawAll = RiskCalculator.computeTakeClueChanceRaw(
                cluePreview.sessionRisk(), values.valueAll());
        boolean showAllLine = values.valueAll() > values.valueOne();

        Thievery.getInstance().getLogger().info(String.format(
                "[Thievery:CluePreview] reason=%s guaranteed=%s sessionCap=%.4f player=%s guiSlot=%d item=%s perItem=%.4f "
                        + "amountAll=%d maxByBudget=%d maxFit=%d valueOne=%.4f valueAll=%.4f "
                        + "budget=%.2f risk=%.4f cluesDropped=%d valueRiskOne=%.4f valueRiskAll=%.4f "
                        + "clueRawOne=%.4f clueRawAll=%.4f previewOne=%s previewAll=%s showAll=%s",
                reason,
                guaranteed,
                RiskCalculator.computeTakeSessionCap(),
                player.getName(),
                guiSlot,
                itemLabel,
                perItem,
                values.amountAll(),
                values.maxByBudget(),
                values.maxFit(),
                values.valueOne(),
                values.valueAll(),
                session.getBudget().getRemaining(),
                cluePreview.sessionRisk(),
                cluePreview.successfulClueDrops(),
                valueRiskOne,
                valueRiskAll,
                clueRawOne,
                clueRawAll,
                formatPreviewLine(one),
                formatPreviewLine(all),
                showAllLine));
    }

    private static String formatPreviewLine(RiskCalculator.TakeCluePreview preview) {
        return RiskCalculator.formatPercentWhole(preview.clueChance()) + " (critical "
                + RiskCalculator.formatPercentWhole(preview.criticalOnTake()) + ")";
    }
}
