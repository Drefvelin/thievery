package net.tfminecraft.thievery.steal.source;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.steal.source.StealSource;

public final class ContainerStealSource implements StealSource {

    private final Inventory inventory;

    public ContainerStealSource(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public ItemStack getItem(int logicalSlot) {
        return inventory.getItem(logicalSlot);
    }

    @Override
    public void setItem(int logicalSlot, ItemStack item) {
        inventory.setItem(logicalSlot, item);
    }

    public Inventory getInventory() {
        return inventory;
    }
}
