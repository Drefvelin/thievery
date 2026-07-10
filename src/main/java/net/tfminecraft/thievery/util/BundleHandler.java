package net.tfminecraft.thievery.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;

import net.tfminecraft.thievery.data.PlayerData;

public final class BundleHandler {

    private BundleHandler() {}

    public static boolean isBundle(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getItemMeta() instanceof BundleMeta;
    }

    public static boolean canRevealBundle(PlayerData thiefData, ItemStack bundle) {
        if (!isBundle(bundle)) {
            return CategoryResolver.canRevealItem(thiefData, bundle);
        }
        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            return false;
        }
        for (ItemStack inner : meta.getItems()) {
            if (inner == null || inner.getType().isAir()) continue;
            if (CategoryResolver.canRevealItem(thiefData, inner)) {
                return true;
            }
        }
        return false;
    }

    public static double getContentsValue(ItemStack bundle) {
        if (!isBundle(bundle)) {
            return CategoryResolver.getTotalValue(bundle);
        }
        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            return 0;
        }
        double total = 0;
        for (ItemStack inner : meta.getItems()) {
            if (inner == null || inner.getType().isAir()) continue;
            total += CategoryResolver.getTotalValue(inner);
        }
        return total;
    }

    public static double getRevealableContentsValue(PlayerData thiefData, ItemStack bundle) {
        if (!isBundle(bundle)) {
            return CategoryResolver.canRevealItem(thiefData, bundle)
                    ? CategoryResolver.getTotalValue(bundle) : 0;
        }
        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            return 0;
        }
        double total = 0;
        for (ItemStack inner : meta.getItems()) {
            if (inner == null || inner.getType().isAir()) continue;
            if (!CategoryResolver.canRevealItem(thiefData, inner)) continue;
            total += CategoryResolver.getTotalValue(inner);
        }
        return total;
    }

    public static boolean canStealAnything(PlayerData thiefData, ItemStack bundle, double capacityRemaining) {
        if (!canRevealBundle(thiefData, bundle)) {
            return false;
        }
        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            return false;
        }
        for (ItemStack inner : meta.getItems()) {
            if (inner == null || inner.getType().isAir()) continue;
            if (!CategoryResolver.canRevealItem(thiefData, inner)) continue;
            double perItem = CategoryResolver.getPerItemValue(inner);
            if (perItem <= 0 || perItem <= capacityRemaining) {
                return true;
            }
        }
        return false;
    }

    public static BundleTakeResult takeFromBundle(ItemStack bundle, Player player, PlayerData thiefData,
            double capacityRemaining) {
        if (!isBundle(bundle)) {
            return BundleTakeResult.none(bundle);
        }

        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            return BundleTakeResult.none(bundle);
        }

        List<ItemStack> contents = new ArrayList<>();
        for (ItemStack inner : meta.getItems()) {
            contents.add(inner == null ? null : inner.clone());
        }

        double budget = capacityRemaining;
        double valueTaken = 0;
        boolean anyTaken = false;

        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < contents.size(); i++) {
                ItemStack inner = contents.get(i);
                if (inner == null || inner.getType().isAir()) continue;
                if (!CategoryResolver.canRevealItem(thiefData, inner)) continue;

                double perItem = CategoryResolver.getPerItemValue(inner);
                if (perItem > 0 && perItem > budget) continue;

                ItemStack toGive = inner.clone();
                toGive.setAmount(1);

                HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(toGive);
                if (!leftovers.isEmpty()) {
                    return finish(bundle, contents, valueTaken, anyTaken);
                }

                if (inner.getAmount() <= 1) {
                    contents.set(i, null);
                } else {
                    inner.setAmount(inner.getAmount() - 1);
                }

                if (perItem > 0) {
                    budget -= perItem;
                }
                valueTaken += perItem > 0 ? perItem : 0;
                anyTaken = true;
                progress = true;
            }
        }

        return finish(bundle, contents, valueTaken, anyTaken);
    }

    private static BundleTakeResult finish(ItemStack bundle, List<ItemStack> contents, double valueTaken,
            boolean anyTaken) {
        ItemStack updated = bundle.clone();
        BundleMeta meta = (BundleMeta) updated.getItemMeta();
        if (meta == null) {
            return new BundleTakeResult(bundle, valueTaken, anyTaken);
        }

        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack inner : contents) {
            if (inner != null && !inner.getType().isAir()) {
                remaining.add(inner);
            }
        }
        meta.setItems(remaining.isEmpty() ? null : remaining);
        updated.setItemMeta(meta);
        return new BundleTakeResult(updated, valueTaken, anyTaken);
    }

    public static final class BundleTakeResult {
        private final ItemStack updatedBundle;
        private final double valueTaken;
        private final boolean anyTaken;

        private BundleTakeResult(ItemStack updatedBundle, double valueTaken, boolean anyTaken) {
            this.updatedBundle = updatedBundle;
            this.valueTaken = valueTaken;
            this.anyTaken = anyTaken;
        }

        public static BundleTakeResult none(ItemStack bundle) {
            return new BundleTakeResult(bundle, 0, false);
        }

        public ItemStack getUpdatedBundle() {
            return updatedBundle;
        }

        public double getValueTaken() {
            return valueTaken;
        }

        public boolean isAnyTaken() {
            return anyTaken;
        }
    }
}
