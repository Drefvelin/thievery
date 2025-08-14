package net.tfminecraft.thievery.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ContainerData {

    private Location location;
    private UUID owner;
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
        if(owner == null) return false;
        if(owner.equals(p.getUniqueId())) return true;
        OfflinePlayer o = Bukkit.getOfflinePlayer(owner);
        if(o == null) return false;
        Faction f = FactionManager.getByMember(o.getName());
        if(f == null) return false;
        if(!f.getMembers().contains(p.getName())) return false;
        return true;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
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
