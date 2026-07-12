package net.tfminecraft.thievery.util;

import java.util.UUID;

import org.bukkit.Location;

import net.tfminecraft.thievery.data.LockState;

public final class ClearCluesTarget {

    public enum Kind {
        DOOR,
        CONTAINER
    }

    private final Kind kind;
    private final Location canonicalLocation;
    private final UUID ownerUuid;
    private final LockState lockState;

    public ClearCluesTarget(Kind kind, Location canonicalLocation, UUID ownerUuid, LockState lockState) {
        this.kind = kind;
        this.canonicalLocation = canonicalLocation;
        this.ownerUuid = ownerUuid;
        this.lockState = lockState;
    }

    public Kind getKind() {
        return kind;
    }

    public Location getCanonicalLocation() {
        return canonicalLocation;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public LockState getLockState() {
        return lockState;
    }
}
