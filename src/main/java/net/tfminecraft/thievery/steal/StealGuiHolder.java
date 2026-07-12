package net.tfminecraft.thievery.steal;

import java.util.UUID;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class StealGuiHolder implements InventoryHolder {

    public enum Kind {
        ROBBERY,
        PICKPOCKET,
        CHEST
    }

    private final UUID playerId;
    private final Kind kind;

    public StealGuiHolder(UUID playerId, Kind kind) {
        this.playerId = playerId;
        this.kind = kind;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
