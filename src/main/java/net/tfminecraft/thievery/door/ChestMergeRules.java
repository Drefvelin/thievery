package net.tfminecraft.thievery.door;

import java.util.UUID;

public final class ChestMergeRules {

    private ChestMergeRules() {}

    /** Unowned neighbors are allowed. Guild/access does not count. */
    public static boolean personallyOwns(UUID placer, UUID neighborOwner) {
        return neighborOwner == null || placer.equals(neighborOwner);
    }
}
