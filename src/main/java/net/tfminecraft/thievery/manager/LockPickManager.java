package net.tfminecraft.thievery.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.cache.Parameters;

public class LockPickManager {

    private final String breakBar = "§c-";
    private final String successBar = "§a-";
    private final String failBar = "§e-";
    private final String currentBar = "§f=";
    private final Random random = new Random();

    private final Map<UUID, LockpickSession> sessions = new HashMap<>();
    private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

    public enum SelectResult { SUCCESS, FAIL, BREAK, NOT_IN_SESSION }

    private static class LockpickSession {
        final Location doorLocation;
        final char[] layout; // 's' = success, 'f' = fail, 'b' = break
        double position = 0;
        final double speed;
        int direction = 1;
        BukkitRunnable task;

        LockpickSession(Location doorLocation, char[] layout, double speed) {
            this.doorLocation = doorLocation;
            this.layout = layout;
            this.speed = speed;
        }
    }

    /**
     * Starts a lockpicking session for the given player on the given door.
     * effectiveStrength: 0 = easy (many success bars), 1 = hard (few success, many break bars)
     * dexterity: 0-40, each level slows bar movement by 2%
     */
    public void startSession(Player player, Location doorLoc, double effectiveStrength, int dexterity) {
        cancelSession(player.getUniqueId());

        int barLength = Parameters.barLength;
        // successCount: maxSuccessSlots at strength 0, min 1 at strength 1
        // At s=0.1 -> 3, at s=1 -> 1
        int successCount = Math.max(1, (int) Math.round(1 + (Parameters.maxSuccessSlots - 1) * (1.0 - effectiveStrength)));
        // breakCount: minBreakSlots at strength 0, maxBreakSlots at strength 1
        // At s=0.1 -> ~5, at s=1 -> 19
        int breakCount = (int) Math.round(Parameters.minBreakSlots + (Parameters.maxBreakSlots - Parameters.minBreakSlots) * effectiveStrength);
        if (successCount + breakCount > barLength) breakCount = barLength - successCount;
        int failCount = barLength - successCount - breakCount;

        // Debuff: convert fail bars to break bars proportionally based on remaining debuff time
        double debuffFactor = getDebuffFactor(player.getUniqueId());
        int debuffBreaks = (int) Math.round(failCount * debuffFactor);
        failCount -= debuffBreaks;
        breakCount += debuffBreaks;

        List<Character> layoutList = new ArrayList<>(barLength);
        for (int i = 0; i < successCount; i++) layoutList.add('s');
        for (int i = 0; i < breakCount; i++) layoutList.add('b');
        for (int i = 0; i < failCount; i++) layoutList.add('f');
        Collections.shuffle(layoutList);

        char[] layout = new char[barLength];
        for (int i = 0; i < barLength; i++) layout[i] = layoutList.get(i);

        // Find all success slot indices so we can start on one
        List<Integer> successIndices = new ArrayList<>();
        for (int i = 0; i < barLength; i++) {
            if (layout[i] == 's') successIndices.add(i);
        }
        double startPosition = successIndices.isEmpty() ? 0
                : successIndices.get(random.nextInt(successIndices.size()));

        // Multiplicative reduction: 2% of base speed per dex level
        double speed = Math.max(Parameters.minBarSpeed, Parameters.baseBarSpeed * (1.0 - dexterity * Parameters.dexSpeedReductionPerLevel));

        UUID uuid = player.getUniqueId();
        LockpickSession session = new LockpickSession(doorLoc, layout, speed);
        session.position = startPosition;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    sessions.remove(uuid);
                    this.cancel();
                    return;
                }

                // Random direction flip for unpredictability
                if (random.nextDouble() < Parameters.randomFlipChance) {
                    session.direction *= -1;
                }

                // Speed jitter: vary speed each tick around the base
                double jitter = (random.nextDouble() * 2 - 1) * Parameters.baseBarSpeed * Parameters.speedJitterFraction;
                double tickSpeed = Math.max(Parameters.minBarSpeed, session.speed + jitter);

                session.position += tickSpeed * session.direction;

                if (session.position >= barLength - 1) {
                    session.position = barLength - 1;
                    session.direction = -1;
                } else if (session.position <= 0) {
                    session.position = 0;
                    session.direction = 1;
                }

                int currentSlot = (int) Math.round(session.position);
                if (currentSlot >= barLength) currentSlot = barLength - 1;

                StringBuilder bar = new StringBuilder("§8[");
                for (int i = 0; i < barLength; i++) {
                    if (i == currentSlot) {
                        bar.append(currentBar);
                    } else {
                        bar.append(switch (session.layout[i]) {
                            case 's' -> successBar;
                            case 'b' -> breakBar;
                            default -> failBar;
                        });
                    }
                }
                bar.append("§8]");

                player.sendTitle("", bar.toString(), 0, 3, 0);
            }
        };

        session.task = task;
        sessions.put(uuid, session);
        task.runTaskTimer(Thievery.getInstance(), 0L, 1L);
    }

    /** Called when the player right-clicks during a session to select the current slot. */
    public SelectResult handleSelect(Player player) {
        UUID uuid = player.getUniqueId();
        LockpickSession session = sessions.get(uuid);
        if (session == null) return SelectResult.NOT_IN_SESSION;

        int slot = (int) Math.round(session.position);
        if (slot >= Parameters.barLength) slot = Parameters.barLength - 1;
        char type = session.layout[slot];

        cancelSession(uuid);

        return switch (type) {
            case 's' -> SelectResult.SUCCESS;
            case 'b' -> {
                applyCooldown(uuid);
                yield SelectResult.BREAK;
            }
            default -> {
                applyCooldown(uuid);
                yield SelectResult.FAIL;
            }
        };
    }

    public void cancelSession(UUID uuid) {
        LockpickSession session = sessions.remove(uuid);
        if (session != null && session.task != null) {
            session.task.cancel();
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendTitle("", "", 0, 1, 0);
        }
    }

    public boolean isInSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public Location getSessionDoor(UUID uuid) {
        LockpickSession session = sessions.get(uuid);
        return session != null ? session.doorLocation : null;
    }

    public boolean isOnCooldown(UUID uuid) {
        return getDebuffFactor(uuid) > 0;
    }

    /** Returns a value from 1.0 (full debuff, just applied) down to 0.0 (debuff expired). */
    public double getDebuffFactor(UUID uuid) {
        Long expiry = cooldownExpiry.get(uuid);
        if (expiry == null) return 0.0;
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) return 0.0;
        return remaining / (double) Parameters.lockpickFailCooldownMs;
    }

    public long getCooldownRemainingSeconds(UUID uuid) {
        Long expiry = cooldownExpiry.get(uuid);
        if (expiry == null) return 0;
        return Math.max(0, (expiry - System.currentTimeMillis()) / 1000);
    }

    private void applyCooldown(UUID uuid) {
        cooldownExpiry.put(uuid, System.currentTimeMillis() + Parameters.lockpickFailCooldownMs);
    }
}
