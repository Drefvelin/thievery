package net.tfminecraft.thievery.door;

import java.util.UUID;

import org.bukkit.entity.Player;

public class EntityLockData {

    private final UUID entityId;
    private UUID owner;
    private LockState lockState = LockState.PRIVATE;

    public EntityLockData(UUID entityId) {
        this.entityId = entityId;
    }

    public EntityLockData(UUID entityId, UUID owner) {
        this.entityId = entityId;
        this.owner = owner;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public boolean owns(Player player) {
        return owner != null && owner.equals(player.getUniqueId());
    }

    public boolean canAccess(Player player) {
        return LockAccess.canAccess(player, owner, lockState);
    }

    public LockState getLockState() {
        return lockState;
    }

    public void setLockState(LockState lockState) {
        this.lockState = lockState == null ? LockState.PRIVATE : lockState;
    }

    public LockState rotateLockState() {
        lockState = lockState.next();
        return lockState;
    }
}
