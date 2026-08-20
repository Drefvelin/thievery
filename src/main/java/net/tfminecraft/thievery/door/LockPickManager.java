package net.tfminecraft.thievery.door;

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
import net.tfminecraft.thievery.player.PlayerData;
import net.tfminecraft.thievery.door.DoorLockpick.DoorProximityAnchor;
import net.tfminecraft.thievery.door.DoorLockpick.ProximityAnchor;
import net.tfminecraft.thievery.player.RiskCalculator;
import net.tfminecraft.thievery.utils.ThieveryTexts;

public class LockPickManager {

    public enum SessionKind {
        DOOR
    }

    public enum SelectResult {
        SUCCESS, FAIL, BREAK, NOT_IN_SESSION
    }

    private final String breakBar = ThieveryTexts.ERROR + "-";
    private final String successBar = ThieveryTexts.SUCCESS + "-";
    private final String failBar = ThieveryTexts.WARN + "-";
    private final String currentBar = ThieveryTexts.WHITE + "=";
    private final Random random = new Random();

    private final Map<UUID, LockpickSession> sessions = new HashMap<>();
    private final Map<UUID, Map<Long, Long>> doorCooldownExpiry = new HashMap<>();

    private static class LockpickSession {
        final ProximityAnchor anchor;
        final SessionKind kind;
        final Location doorLocation;
        final char[] layout;
        final double lockpickStrength;
        final int dexterity;
        final Runnable onProximityLost;
        double position = 0;
        final double speed;
        int direction = 1;
        BukkitRunnable task;

        LockpickSession(ProximityAnchor anchor, SessionKind kind, Location doorLocation,
                char[] layout, double speed, double lockpickStrength, int dexterity, Runnable onProximityLost) {
            this.anchor = anchor;
            this.kind = kind;
            this.doorLocation = doorLocation;
            this.layout = layout;
            this.speed = speed;
            this.lockpickStrength = lockpickStrength;
            this.dexterity = dexterity;
            this.onProximityLost = onProximityLost;
        }
    }

    public void startDoorSession(Player player, Location doorLoc, double effectiveStrength, int dexterity,
            double lockpickStrength) {
        DoorProximityAnchor anchor = new DoorLockpick.DoorProximityAnchor(doorLoc);
        startSession(player, anchor, SessionKind.DOOR, doorLoc, effectiveStrength, dexterity, lockpickStrength,
                null);
    }

    private void startSession(Player player, ProximityAnchor anchor, SessionKind kind, Location doorLocation,
            double effectiveStrength, int dexterity, double lockpickStrength,
            Runnable onProximityLost) {
        cancelSession(player.getUniqueId(), true);

        int barLength = Parameters.barLength;
        int successCount = Math.max(1,
                (int) Math.round(1 + (Parameters.maxSuccessSlots - 1) * (1.0 - effectiveStrength)));
        int breakCount = (int) Math.round(
                Parameters.minBreakSlots + (Parameters.maxBreakSlots - Parameters.minBreakSlots) * effectiveStrength);
        if (successCount + breakCount > barLength) {
            breakCount = barLength - successCount;
        }
        int failCount = barLength - successCount - breakCount;

        double debuffFactor = getDebuffFactor(player.getUniqueId(), doorLocation);
        int debuffBreaks = (int) Math.round(failCount * debuffFactor);
        failCount -= debuffBreaks;
        breakCount += debuffBreaks;

        List<Character> layoutList = new ArrayList<>(barLength);
        for (int i = 0; i < successCount; i++) {
            layoutList.add('s');
        }
        for (int i = 0; i < breakCount; i++) {
            layoutList.add('b');
        }
        for (int i = 0; i < failCount; i++) {
            layoutList.add('f');
        }
        Collections.shuffle(layoutList);

        char[] layout = new char[barLength];
        for (int i = 0; i < barLength; i++) {
            layout[i] = layoutList.get(i);
        }

        List<Integer> successIndices = new ArrayList<>();
        for (int i = 0; i < barLength; i++) {
            if (layout[i] == 's') {
                successIndices.add(i);
            }
        }
        double startPosition = successIndices.isEmpty() ? 0
                : successIndices.get(random.nextInt(successIndices.size()));

        double speed = Math.max(Parameters.minBarSpeed,
                Parameters.baseBarSpeed * (1.0 - dexterity * Parameters.dexSpeedReductionPerLevel));

        UUID uuid = player.getUniqueId();
        LockpickSession session = new LockpickSession(anchor, kind, doorLocation, layout, speed,
                lockpickStrength, dexterity, onProximityLost);
        session.position = startPosition;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelSession(uuid, false);
                    return;
                }

                if (!session.anchor.isInRange(player)) {
                    cancelSession(uuid, true);
                    player.sendTitle("", "", 0, 1, 0);
                    session.anchor.onOutOfRange(player);
                    if (session.onProximityLost != null) {
                        session.onProximityLost.run();
                    }
                    return;
                }

                if (random.nextDouble() < Parameters.randomFlipChance) {
                    session.direction *= -1;
                }

                double jitter = (random.nextDouble() * 2 - 1) * Parameters.baseBarSpeed
                        * Parameters.speedJitterFraction;
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
                if (currentSlot >= barLength) {
                    currentSlot = barLength - 1;
                }

                StringBuilder bar = new StringBuilder(ThieveryTexts.DARK + "[");
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
                bar.append(ThieveryTexts.DARK).append("]");

                PlayerData thiefData = Thievery.getPlayerManager().get(uuid);
                thiefData.applyRiskDecay(session.dexterity);
                double risk = thiefData.getRisk();
                double critical = RiskCalculator.computeCritical(risk, session.dexterity, session.lockpickStrength);
                String riskTitle = RiskCalculator.formatRiskTitle(risk, critical);

                player.sendTitle(riskTitle, ThieveryTexts.formatDisplay(bar.toString()), 0, 3, 0);
            }
        };

        session.task = task;
        sessions.put(uuid, session);
        task.runTaskTimer(Thievery.getInstance(), 0L, 1L);
    }

    /** @deprecated use startDoorSession */
    @Deprecated
    public void startSession(Player player, Location doorLoc, double effectiveStrength, int dexterity,
            double lockpickStrength) {
        startDoorSession(player, doorLoc, effectiveStrength, dexterity, lockpickStrength);
    }

    public SelectResult handleSelect(Player player) {
        UUID uuid = player.getUniqueId();
        LockpickSession session = sessions.get(uuid);
        if (session == null) {
            return SelectResult.NOT_IN_SESSION;
        }

        int slot = (int) Math.round(session.position);
        if (slot >= Parameters.barLength) {
            slot = Parameters.barLength - 1;
        }
        char type = session.layout[slot];

        boolean penalize = type != 's';
        cancelSession(uuid, penalize);

        return switch (type) {
            case 's' -> SelectResult.SUCCESS;
            case 'b' -> SelectResult.BREAK;
            default -> SelectResult.FAIL;
        };
    }

    public void cancelSession(UUID uuid) {
        cancelSession(uuid, false);
    }

    public void cancelSession(UUID uuid, boolean penalizeDoor) {
        LockpickSession session = sessions.remove(uuid);
        if (session == null) {
            return;
        }
        if (session.task != null) {
            session.task.cancel();
        }
        if (penalizeDoor && session.doorLocation != null) {
            applyDoorCooldown(uuid, session.doorLocation);
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendTitle("", "", 0, 1, 0);
        }
    }

    public boolean isInSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public SessionKind getSessionKind(UUID uuid) {
        LockpickSession session = sessions.get(uuid);
        return session != null ? session.kind : null;
    }

    public Location getSessionDoor(UUID uuid) {
        LockpickSession session = sessions.get(uuid);
        return session != null ? session.doorLocation : null;
    }

    public boolean isOnCooldown(UUID uuid, Location doorLocation) {
        return getDebuffFactor(uuid, doorLocation) > 0;
    }

    public double getDebuffFactor(UUID uuid, Location doorLocation) {
        if (doorLocation == null) {
            return 0.0;
        }
        Map<Long, Long> doorMap = doorCooldownExpiry.get(uuid);
        if (doorMap == null) {
            return 0.0;
        }
        Long expiry = doorMap.get(packDoorKey(doorLocation));
        if (expiry == null) {
            return 0.0;
        }
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) {
            return 0.0;
        }
        return remaining / (double) Parameters.lockpickFailCooldownMs;
    }

    public long getCooldownRemainingSeconds(UUID uuid, Location doorLocation) {
        if (doorLocation == null) {
            return 0;
        }
        Map<Long, Long> doorMap = doorCooldownExpiry.get(uuid);
        if (doorMap == null) {
            return 0;
        }
        Long expiry = doorMap.get(packDoorKey(doorLocation));
        if (expiry == null) {
            return 0;
        }
        return Math.max(0, (expiry - System.currentTimeMillis()) / 1000);
    }

    private void applyDoorCooldown(UUID uuid, Location doorLocation) {
        if (doorLocation == null) {
            return;
        }
        doorCooldownExpiry
                .computeIfAbsent(uuid, ignored -> new HashMap<>())
                .put(packDoorKey(doorLocation), System.currentTimeMillis() + Parameters.lockpickFailCooldownMs);
    }

    public static long packDoorKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return 0L;
        }
        return packDoorKey(location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                location.getWorld().getName().hashCode());
    }

    private static long packDoorKey(int x, int y, int z, int worldHash) {
        return ((long) worldHash & 0xFFFFL) << 48
                | ((long) x & 0x3FFFFFL) << 30
                | ((long) y & 0xFFFL) << 18
                | ((long) z & 0x3FFFFFL);
    }

    public void clearCooldown(UUID uuid) {
        if (uuid != null) {
            doorCooldownExpiry.remove(uuid);
        }
    }

    public void clearAllCooldowns() {
        doorCooldownExpiry.clear();
    }
}
