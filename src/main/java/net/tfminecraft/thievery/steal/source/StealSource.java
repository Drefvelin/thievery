package net.tfminecraft.thievery.steal.source;

import org.bukkit.inventory.ItemStack;

public interface StealSource {

    ItemStack getItem(int logicalSlot);

    void setItem(int logicalSlot, ItemStack item);
}
