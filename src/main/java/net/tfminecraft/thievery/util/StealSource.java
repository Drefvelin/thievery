package net.tfminecraft.thievery.util;

import org.bukkit.inventory.ItemStack;

public interface StealSource {

    ItemStack getItem(int logicalSlot);

    void setItem(int logicalSlot, ItemStack item);
}
