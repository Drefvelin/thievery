package net.tfminecraft.thievery.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;

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
        if (lockState == LockState.PUBLIC) return true;
        if (owner == null) return true;
        if (owner.equals(p.getUniqueId())) return true;
        if (lockState == LockState.PRIVATE) return false;

        OfflinePlayer o = Bukkit.getOfflinePlayer(owner);
        if (o.getName() == null) return false;

        Guild ownerGuild = FactionManager.getGuildByMember(o.getName());
        Guild openerGuild = FactionManager.getGuildByMember(p.getName());

        if (ownerGuild == null || openerGuild == null) return false;

        return ownerGuild.getId().equals(openerGuild.getId());
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
