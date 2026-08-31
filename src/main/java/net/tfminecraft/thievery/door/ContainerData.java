package net.tfminecraft.thievery.door;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ContainerData {

    private Location location;
    private UUID owner;
    private LockState lockState = LockState.PRIVATE;
    private Map<UUID, String> accessMap = new HashMap<>();

    public ContainerData(Location location) {
        this.location = location;
    }

    // Optionally provide owner at creation
    public ContainerData(Location location, UUID owner) {
        this.location = location;
        this.owner = owner;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean owns(Player p) {
        return owner != null && owner.equals(p.getUniqueId());
    }

    public boolean canAccess(Player p) {
        return LockAccess.canAccess(p, owner, lockState);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
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

    public void updateAccess(UUID playerUUID, String date) {
        accessMap.put(playerUUID, date);
    }

    public String getLastAccess(UUID playerUUID) {
        return accessMap.get(playerUUID);
    }

    public Map<UUID, String> getAccessMap() {
        return accessMap;
    }

    public void setAccessMap(Map<UUID, String> accessMap) {
        this.accessMap = accessMap;
    }
}
