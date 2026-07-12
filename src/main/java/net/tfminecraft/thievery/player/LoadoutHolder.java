package net.tfminecraft.thievery.player;

import java.util.UUID;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LoadoutHolder implements InventoryHolder {

    private final UUID playerId;
    private int page;

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

    public void setPage(int page) {
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
