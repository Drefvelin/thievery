package net.tfminecraft.thievery.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class PlayerStealSource implements StealSource {

    private final Player victim;

    public PlayerStealSource(Player victim) {
        this.victim = victim;
    }

    @Override
    public ItemStack getItem(int logicalSlot) {
        return PlayerSlotMap.getItem(victim, logicalSlot);
    }

    @Override
    public void setItem(int logicalSlot, ItemStack item) {
        PlayerSlotMap.setItem(victim, logicalSlot, item);
    }

    public Player getVictim() {
        return victim;
    }
}
