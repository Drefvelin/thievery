package net.tfminecraft.thievery.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.tfminecraft.util.Keys;

public final class StealGuiPanes {

    private StealGuiPanes() {}

    public static ItemStack createUnknownPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName("???");
        meta.getPersistentDataContainer().set(Keys.stealUnknown, PersistentDataType.BYTE, (byte) 1);
        pane.setItemMeta(meta);
        return pane;
    }

    public static ItemStack createFillerPane() {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        meta.getPersistentDataContainer().set(Keys.stealFiller, PersistentDataType.BYTE, (byte) 1);
        filler.setItemMeta(meta);
        return filler;
    }

    public static ItemStack createNothingPane() {
        ItemStack pane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(ThieveryTexts.format(ThieveryTexts.ERROR + "Nothing found."));
        meta.getPersistentDataContainer().set(Keys.stealNothing, PersistentDataType.BYTE, (byte) 1);
        pane.setItemMeta(meta);
        return pane;
    }

    public static ItemStack createHiddenPane() {
        ItemStack hidden = new ItemStack(Material.BARRIER);
        ItemMeta meta = hidden.getItemMeta();
        meta.setDisplayName(ThieveryTexts.format(ThieveryTexts.MUTED + "HIDDEN!"));
        meta.getPersistentDataContainer().set(Keys.stealHidden, PersistentDataType.BYTE, (byte) 1);
        hidden.setItemMeta(meta);
        return hidden;
    }

    public static boolean isUnknownPane(ItemStack item) {
        return hasKey(item, Keys.stealUnknown);
    }

    public static boolean isFillerPane(ItemStack item) {
        return hasKey(item, Keys.stealFiller);
    }

    public static boolean isNothingPane(ItemStack item) {
        return hasKey(item, Keys.stealNothing);
    }

    public static boolean isHiddenPane(ItemStack item) {
        return hasKey(item, Keys.stealHidden);
    }

    public static boolean isNonInteractivePane(ItemStack item) {
        return isFillerPane(item) || isNothingPane(item);
    }

    private static boolean hasKey(ItemStack item, org.bukkit.NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
