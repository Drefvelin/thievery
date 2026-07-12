package net.tfminecraft.thievery.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerInteractCooldown {

    private static final long COOLDOWN_MS = 200L;
    private static final Map<UUID, Long> lastInteractMs = new HashMap<>();

    private PlayerInteractCooldown() {}

    public static boolean tryAcquire(UUID playerId) {
        if (playerId == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long last = lastInteractMs.get(playerId);
        if (last != null && now - last < COOLDOWN_MS) {
            return false;
        }
        lastInteractMs.put(playerId, now);
        return true;
    }

    public static void clear(UUID playerId) {
        if (playerId != null) {
            lastInteractMs.remove(playerId);
        }
    }
}
