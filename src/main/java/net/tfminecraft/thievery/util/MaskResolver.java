package net.tfminecraft.thievery.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.loader.MaskLoader;

public final class MaskResolver {

    private MaskResolver() {}

    public static boolean isMaskItem(ItemStack item) {
        return MaskLoader.resolveMask(item) != null;
    }

    public static boolean isWearingMask(Player player) {
        return isMaskItem(player.getInventory().getHelmet());
    }
}
