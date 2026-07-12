package net.tfminecraft.thievery.door;

import java.util.UUID;

import org.bukkit.Location;

public class DoorData {
    private Location location;
    private String key;
    private double strength;
    private UUID ownerUUID;
    private Long unlockExpiryMs;

    public DoorData(Location location, String key, double strength, UUID ownerUUID) {
        this.location = location;
        this.key = key;
        this.strength = strength;
        this.ownerUUID = ownerUUID;
    }

    public Location getLocation() {
        return location;
    }

    public String getKey() {
        return key;
    }

    public double getStrength() {
        return strength;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public Long getUnlockExpiryMs() {
        return unlockExpiryMs;
    }

    public void setUnlockExpiryMs(Long unlockExpiryMs) {
        this.unlockExpiryMs = unlockExpiryMs;
    }
}
