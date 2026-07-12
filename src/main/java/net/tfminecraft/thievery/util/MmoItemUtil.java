package net.tfminecraft.thievery.util;

import org.bukkit.inventory.ItemStack;

import io.lumine.mythic.lib.api.item.NBTItem;

public final class MmoItemUtil {

    private MmoItemUtil() {
    }

    public static boolean hasType(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return NBTItem.get(item).hasType();
    }
}
