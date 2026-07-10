package net.tfminecraft.thievery.holder;

import java.util.UUID;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LoadoutHolder implements InventoryHolder {

    private final UUID playerId;
    private final int page;

    public LoadoutHolder(UUID playerId, int page) {
        this.playerId = playerId;
        this.page = page;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
