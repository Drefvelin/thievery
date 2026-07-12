package net.tfminecraft.thievery.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.thievery.data.PlayerData;

public final class BundleHandler {

    public enum BundleTakeMode {
        ONE,
        GREEDY
    }

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
            return canRevealContainer(thiefData, bundle);
        }
        for (ItemStack inner : meta.getItems()) {
            if (inner == null || inner.getType().isAir()) {
                continue;
            }
            if (CategoryResolver.canRevealItem(thiefData, inner)) {
                return true;
            }
        }
        return canRevealContainer(thiefData, bundle);
    }

    public static double getInnerContentsValue(ItemStack bundle) {
        if (!isBundle(bundle)) {
            return CategoryResolver.getPerItemValue(bundle) * bundle.getAmount();
        }
        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            return 0;
        }
        double total = 0;
        for (ItemStack inner : meta.getItems()) {
            if (inner == null || inner.getType().isAir()) {
                continue;
            }
            total += CategoryResolver.getPerItemValue(inner) * inner.getAmount();
        }
        return total;
    }

    public static double getContentsValue(ItemStack bundle) {
        if (!isBundle(bundle)) {
            return CategoryResolver.getTotalValue(bundle);
        }
        return CategoryResolver.getPerItemValue(bundle) + getInnerContentsValue(bundle);
    }

    public static double getRevealableContentsValue(PlayerData thiefData, ItemStack bundle) {
        if (!isBundle(bundle)) {
            return CategoryResolver.canRevealItem(thiefData, bundle)
                    ? CategoryResolver.getTotalValue(bundle) : 0;
        }
        double total = canRevealContainer(thiefData, bundle) ? CategoryResolver.getPerItemValue(bundle) : 0;
        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            return total;
        }
        for (ItemStack inner : meta.getItems()) {
            if (inner == null || inner.getType().isAir()) {
                continue;
            }
            if (!CategoryResolver.canRevealItem(thiefData, inner)) {
                continue;
            }
            total += CategoryResolver.getPerItemValue(inner) * inner.getAmount();
        }
        return total;
    }

    public static ItemStack buildDisplayBundle(PlayerData thiefData, ItemStack realBundle) {
        if (!isBundle(realBundle)) {
            return null;
        }
        BundleMeta sourceMeta = (BundleMeta) realBundle.getItemMeta();
        if (sourceMeta == null) {
            return null;
        }

        List<ItemStack> revealable = new ArrayList<>();
        if (sourceMeta.hasItems()) {
            for (ItemStack inner : sourceMeta.getItems()) {
                if (inner == null || inner.getType().isAir()) {
                    continue;
                }
                if (!CategoryResolver.canRevealItem(thiefData, inner)) {
                    continue;
                }
                revealable.add(inner.clone());
            }
        }
        if (revealable.isEmpty() && !canRevealContainer(thiefData, realBundle)) {
            return null;
        }

        ItemStack display = realBundle.clone();
        BundleMeta displayMeta = (BundleMeta) display.getItemMeta();
        if (displayMeta == null) {
            return null;
        }
        displayMeta.setItems(revealable.isEmpty() ? null : revealable);
        display.setItemMeta(displayMeta);
        return display;
    }

    public static List<String> getRevealableContentsLore(PlayerData thiefData, ItemStack bundle) {
        List<String> lore = new ArrayList<>();
        if (!isBundle(bundle)) {
            return lore;
        }
        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            return lore;
        }
        for (ItemStack inner : meta.getItems()) {
            if (inner == null || inner.getType().isAir()) {
                continue;
            }
            if (!CategoryResolver.canRevealItem(thiefData, inner)) {
                continue;
            }
            lore.add(ThieveryTexts.format(ThieveryTexts.WHITE + "- " + StringFormatter.getName(inner)
                    + " " + ThieveryTexts.MUTED + "x" + inner.getAmount()));
        }
        return lore;
    }

    public static boolean hasStealableContents(PlayerData thiefData, ItemStack bundle, double capacityRemaining) {
        return canStealAnything(thiefData, bundle, capacityRemaining);
    }

    public static boolean canStealAnything(PlayerData thiefData, ItemStack bundle, double capacityRemaining) {
        if (!isBundle(bundle)) {
            return false;
        }
        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            return false;
        }
        for (ItemStack inner : meta.getItems()) {
            if (inner == null || inner.getType().isAir()) {
                continue;
            }
            if (!CategoryResolver.canRevealItem(thiefData, inner)) {
                continue;
            }
            double perItem = CategoryResolver.getPerItemValue(inner);
            if (perItem <= 0 || perItem <= capacityRemaining) {
                return true;
            }
        }
        return false;
    }

    public static boolean allInnersStealable(PlayerData thiefData, ItemStack bundle) {
        if (!isBundle(bundle)) {
            return false;
        }
        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            return true;
        }
        for (ItemStack inner : meta.getItems()) {
            if (inner == null || inner.getType().isAir()) {
                continue;
            }
            if (!CategoryResolver.canRevealItem(thiefData, inner)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canFitBundleItem(Player player, ItemStack bundle) {
        return StealTakeHandler.maxFitInPlayerInventory(player, bundle, 1) >= 1;
    }

    public static double estimateOneTakeValue(ItemStack bundle, PlayerData thiefData, double capacityRemaining) {
        if (!isBundle(bundle)) {
            return CategoryResolver.getPerItemValue(bundle);
        }

        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            return 0;
        }

        double sum = 0;
        int count = 0;
        for (ItemStack inner : meta.getItems()) {
            if (inner == null || inner.getType().isAir()) {
                continue;
            }
            if (!CategoryResolver.canRevealItem(thiefData, inner)) {
                continue;
            }
            double perItem = CategoryResolver.getPerItemValue(inner);
            if (perItem > 0 && perItem > capacityRemaining) {
                continue;
            }
            sum += perItem > 0 ? perItem : 0;
            count++;
        }
        return count > 0 ? sum / count : 0;
    }

    public static double estimateGreedyTakeValue(ItemStack bundle, Player player, PlayerData thiefData,
            double capacityRemaining) {
        if (!isBundle(bundle)) {
            return CategoryResolver.getTotalValue(bundle);
        }

        if (allInnersStealable(thiefData, bundle)
                && CategoryResolver.getTotalValue(bundle) <= capacityRemaining
                && canFitBundleItem(player, bundle)) {
            return CategoryResolver.getTotalValue(bundle);
        }

        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null || !meta.hasItems()) {
            return 0;
        }

        List<ItemStack> contents = new ArrayList<>();
        for (ItemStack inner : meta.getItems()) {
            contents.add(inner == null ? null : inner.clone());
        }

        double budget = capacityRemaining;
        double valueTaken = 0;
        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < contents.size(); i++) {
                ItemStack inner = contents.get(i);
                if (inner == null || inner.getType().isAir()) {
                    continue;
                }
                if (!CategoryResolver.canRevealItem(thiefData, inner)) {
                    continue;
                }

                double perItem = CategoryResolver.getPerItemValue(inner);
                if (perItem > 0 && perItem > budget) {
                    continue;
                }

                if (StealTakeHandler.maxFitInPlayerInventory(player, inner, 1) < 1) {
                    return valueTaken;
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
                progress = true;
            }
        }

        return valueTaken;
    }

    public static BundleTakeResult takeFromBundle(ItemStack bundle, Player player, PlayerData thiefData,
            double capacityRemaining, BundleTakeMode mode) {
        if (!isBundle(bundle)) {
            return BundleTakeResult.none(bundle);
        }

        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        if (meta == null) {
            return BundleTakeResult.none(bundle);
        }

        if (mode == BundleTakeMode.GREEDY
                && allInnersStealable(thiefData, bundle)
                && CategoryResolver.getTotalValue(bundle) <= capacityRemaining
                && canFitBundleItem(player, bundle)) {
            ItemStack toGive = bundle.clone();
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(toGive);
            if (!leftovers.isEmpty()) {
                return BundleTakeResult.none(bundle);
            }
            double valueTaken = CategoryResolver.getTotalValue(bundle);
            return BundleTakeResult.removedFromSource(valueTaken);
        }

        if (!meta.hasItems()) {
            return BundleTakeResult.none(bundle);
        }

        List<ItemStack> contents = new ArrayList<>();
        for (ItemStack inner : meta.getItems()) {
            contents.add(inner == null ? null : inner.clone());
        }

        if (mode == BundleTakeMode.ONE) {
            return takeOneRandomInner(bundle, contents, player, thiefData, capacityRemaining);
        }

        return takeGreedyInners(bundle, contents, player, thiefData, capacityRemaining);
    }

    private static BundleTakeResult takeOneRandomInner(ItemStack bundle, List<ItemStack> contents, Player player,
            PlayerData thiefData, double capacityRemaining) {
        List<Integer> affordable = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            ItemStack inner = contents.get(i);
            if (inner == null || inner.getType().isAir()) {
                continue;
            }
            if (!CategoryResolver.canRevealItem(thiefData, inner)) {
                continue;
            }
            double perItem = CategoryResolver.getPerItemValue(inner);
            if (perItem > 0 && perItem > capacityRemaining) {
                continue;
            }
            affordable.add(i);
        }
        if (affordable.isEmpty()) {
            return BundleTakeResult.none(bundle);
        }

        int index = affordable.get(ThreadLocalRandom.current().nextInt(affordable.size()));
        ItemStack inner = contents.get(index);
        double perItem = CategoryResolver.getPerItemValue(inner);

        ItemStack toGive = inner.clone();
        toGive.setAmount(1);
        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(toGive);
        if (!leftovers.isEmpty()) {
            return BundleTakeResult.none(bundle);
        }

        if (inner.getAmount() <= 1) {
            contents.set(index, null);
        } else {
            inner.setAmount(inner.getAmount() - 1);
        }

        double valueTaken = perItem > 0 ? perItem : 0;
        return finish(bundle, contents, valueTaken, true);
    }

    private static BundleTakeResult takeGreedyInners(ItemStack bundle, List<ItemStack> contents, Player player,
            PlayerData thiefData, double capacityRemaining) {
        double budget = capacityRemaining;
        double valueTaken = 0;
        boolean anyTaken = false;

        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < contents.size(); i++) {
                ItemStack inner = contents.get(i);
                if (inner == null || inner.getType().isAir()) {
                    continue;
                }
                if (!CategoryResolver.canRevealItem(thiefData, inner)) {
                    continue;
                }

                double perItem = CategoryResolver.getPerItemValue(inner);
                if (perItem > 0 && perItem > budget) {
                    continue;
                }

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
            return new BundleTakeResult(bundle, valueTaken, anyTaken, false);
        }

        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack inner : contents) {
            if (inner != null && !inner.getType().isAir()) {
                remaining.add(inner);
            }
        }
        meta.setItems(remaining.isEmpty() ? null : remaining);
        updated.setItemMeta(meta);
        return new BundleTakeResult(updated, valueTaken, anyTaken, false);
    }

    private static boolean canRevealContainer(PlayerData thiefData, ItemStack bundle) {
        if (ClueChecker.isClueItem(bundle)) {
            return false;
        }
        var category = CategoryResolver.resolveCategory(bundle);
        if (category == null) {
            return true;
        }
        return thiefData.isCategoryActive(category.getId());
    }

    public static final class BundleTakeResult {
        private final ItemStack updatedBundle;
        private final double valueTaken;
        private final boolean anyTaken;
        private final boolean removedFromSource;

        private BundleTakeResult(ItemStack updatedBundle, double valueTaken, boolean anyTaken,
                boolean removedFromSource) {
            this.updatedBundle = updatedBundle;
            this.valueTaken = valueTaken;
            this.anyTaken = anyTaken;
            this.removedFromSource = removedFromSource;
        }

        public static BundleTakeResult none(ItemStack bundle) {
            return new BundleTakeResult(bundle, 0, false, false);
        }

        public static BundleTakeResult removedFromSource(double valueTaken) {
            return new BundleTakeResult(null, valueTaken, true, true);
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

        public boolean isRemovedFromSource() {
            return removedFromSource;
        }
    }
}
