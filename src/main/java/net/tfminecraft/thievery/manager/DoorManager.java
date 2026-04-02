package net.tfminecraft.thievery.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.TLibs.TLibs;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.cache.Parameters;
import net.tfminecraft.thievery.data.DoorData;
import net.tfminecraft.util.GuildChecker;
import net.tfminecraft.util.Keys;

public class DoorManager implements Listener {

    private final DoorDataManager doorDataManager = new DoorDataManager();
    private final LockPickManager lockPickManager = new LockPickManager();
    private final Map<UUID, Long> lastInteract = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        if (!isDoor(block)) return;

        Player player = event.getPlayer();
        Location canonical = getCanonicalLocation(block);
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean holdingKey = isKeyItem(heldItem);

        DoorData data = doorDataManager.loadDoorData(canonical);

        if (player.isSneaking() && holdingKey) {
            long now = System.currentTimeMillis();
            if (now - lastInteract.getOrDefault(player.getUniqueId(), 0L) < 200) return;
            lastInteract.put(player.getUniqueId(), now);
            event.setCancelled(true);
            if (data == null) {
                // Lock the door
                String uuid = getOrCreateKeyUUID(heldItem);
                double strength = getOrCreateKeyStrength(heldItem);
                doorDataManager.saveDoorData(new DoorData(canonical, uuid, strength, player.getUniqueId()));
                player.sendTitle(ChatColor.GREEN + "Door locked.", "", 5, 30, 10);
            } else {
                // Try to unlock
                String keyUUID = getKeyUUID(heldItem);
                if (keyUUID != null && keyUUID.equals(data.getKey())) {
                    doorDataManager.deleteDoorData(canonical);
                    player.sendTitle(ChatColor.GREEN + "Door unlocked.", "", 5, 30, 10);
                } else {
                    player.sendTitle(ChatColor.RED + "This key does not fit this lock.", "", 5, 30, 10);
                }
            }
            return;
        }

        // Lockpick handling — must come before the generic locked-door block
        if (isLockpickItem(heldItem) && data != null) {
            event.setCancelled(true);
            UUID uuid = player.getUniqueId();

            // Cannot lockpick a door that is already open
            if (isDoorOpen(block)) {
                return;
            }

            if (lockPickManager.isInSession(uuid)) {
                if (isSameDoor(lockPickManager.getSessionDoor(uuid), canonical)) {
                    handleSelectResult(player, lockPickManager.handleSelect(player), canonical);
                } else {
                    // Different door — cancel old session and start fresh
                    lockPickManager.cancelSession(uuid);
                    startLockpicking(player, canonical, data);
                }
            } else {
                startLockpicking(player, canonical, data);
            }
            return;
        }

        if (data != null) {
            // Always allow closing an open door, even without the key
            if (isDoorOpen(block)) return;

            String keyUUID = getKeyUUID(heldItem);
            if (keyUUID == null || !keyUUID.equals(data.getKey())) {
                event.setCancelled(true);
                player.sendTitle(ChatColor.RED + "This door is locked.", "", 5, 30, 10);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Resolve which locked door (if any) this break would affect
        Location lockedDoor = getLockedDoorForBreak(block);
        if (lockedDoor == null) return;

        DoorData data = doorDataManager.loadDoorData(lockedDoor);
        if (data == null) {
            // Not locked — only clean up if the block itself is the door
            if (isDoor(block)) doorDataManager.deleteDoorData(lockedDoor);
            return;
        }

        // Check key
        String keyUUID = getKeyUUID(player.getInventory().getItemInMainHand());
        if (keyUUID != null && keyUUID.equals(data.getKey())) {
            // Correct key held — allow break and always clean up door data
            doorDataManager.deleteDoorData(lockedDoor);
        } else {
            event.setCancelled(true);
            player.sendTitle(ChatColor.RED + "This door is locked.", "", 5, 30, 10);
        }
    }

    /**
     * Returns the canonical (bottom-half) location of a locked door that would
     * be affected by breaking the given block, or null if no locked door is involved.
     * Covers: door block (top or bottom half), and the support block beneath the door.
     */
    private Location getLockedDoorForBreak(Block block) {
        // Case 1: the block itself is a door piece
        if (isDoor(block)) {
            return getCanonicalLocation(block);
        }

        // Case 2: the block above this one is the bottom half of a door (support block)
        Block above = block.getRelative(BlockFace.UP);
        if (Tag.DOORS.isTagged(above.getType()) && above.getType() != Material.IRON_DOOR) {
            Bisected bisected = (Bisected) above.getBlockData();
            if (bisected.getHalf() == Bisected.Half.BOTTOM) {
                return above.getLocation();
            }
        }

        return null;
    }

    // --- Helpers ---

    private boolean isDoorOpen(Block block) {
        if (block.getBlockData() instanceof Openable openable) {
            return openable.isOpen();
        }
        return false;
    }

    private boolean isDoor(Block block) {
        Material type = block.getType();
        if (type == Material.IRON_DOOR || type == Material.IRON_TRAPDOOR) return false;
        return Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type) || Tag.FENCE_GATES.isTagged(type);
    }

    private Location getCanonicalLocation(Block block) {
        if (Tag.DOORS.isTagged(block.getType())) {
            Bisected bisected = (Bisected) block.getBlockData();
            if (bisected.getHalf() == Bisected.Half.TOP) {
                return block.getRelative(BlockFace.DOWN).getLocation();
            }
        }
        return block.getLocation();
    }

    private boolean isKeyItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        for (String path : Cache.keyItems) {
            if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, path)) return true;
        }
        return false;
    }

    private String getKeyUUID(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(Keys.keyUUIDKey, PersistentDataType.STRING)) return null;
        return meta.getPersistentDataContainer().get(Keys.keyUUIDKey, PersistentDataType.STRING);
    }

    private String getOrCreateKeyUUID(ItemStack item) {
        String existing = getKeyUUID(item);
        if (existing != null) return existing;
        String newUUID = UUID.randomUUID().toString();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.keyUUIDKey, PersistentDataType.STRING, newUUID);
        item.setItemMeta(meta);
        return newUUID;
    }

    private double getOrCreateKeyStrength(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        double strength;
        if (meta.getPersistentDataContainer().has(Keys.keyStrength, PersistentDataType.DOUBLE)) {
            strength = meta.getPersistentDataContainer().get(Keys.keyStrength, PersistentDataType.DOUBLE);
        } else {
            strength = Parameters.defaultKeyStrength;
            meta.getPersistentDataContainer().set(Keys.keyStrength, PersistentDataType.DOUBLE, strength);
        }
        updateStrengthLore(meta, strength);
        item.setItemMeta(meta);
        return strength;
    }

    private void updateStrengthLore(ItemMeta meta, double strength) {
        int filled = (int) Math.round(strength * 5);
        filled = Math.max(0, Math.min(5, filled));
        StringBuilder stars = new StringBuilder(ChatColor.GOLD.toString());
        for (int i = 0; i < filled; i++) stars.append('★');
        stars.append(ChatColor.DARK_GRAY);
        for (int i = filled; i < 5; i++) stars.append('☆');

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        while (lore.size() < 3) lore.add("");
        lore.set(2, stars.toString());
        meta.setLore(lore);
    }

    // --- Lockpick helpers ---

    private void startLockpicking(Player player, Location canonical, DoorData data) {
        GuildChecker.LockpickAccessResult access = GuildChecker.checkLockpickAccess(data.getOwnerUUID());
        if (access.type == GuildChecker.LockpickAccessResult.Type.DENY) {
            player.sendMessage(ChatColor.RED + access.message);
            return;
        }
        if (access.type == GuildChecker.LockpickAccessResult.Type.WARN) {
            player.sendMessage(ChatColor.YELLOW + access.message);
        }
        double debuffFactor = lockPickManager.getDebuffFactor(player.getUniqueId());
        if (debuffFactor > 0) {
            int penalty = (int) Math.round(debuffFactor * 100);
            long seconds = lockPickManager.getCooldownRemainingSeconds(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Lockpicking with " + penalty + "% penalty (" + seconds + "s)");
        }
        double lockpickStrength = getLockpickStrength(player.getInventory().getItemInMainHand());
        double effectiveStrength = data.getStrength() * (1.0 - Math.min(1.0, lockpickStrength) * Parameters.lockpickMaxReduction);
        int dexterity = getDexterity(player);
        lockPickManager.startSession(player, canonical, effectiveStrength, dexterity);
    }

    private void handleSelectResult(Player player, LockPickManager.SelectResult result, Location canonical) {
        switch (result) {
            case SUCCESS -> {
                openDoor(canonical);
                player.sendTitle(ChatColor.GREEN + "Picked!", "", 5, 40, 10);
                // Quiet world sound — sneaky entry
                canonical.getWorld().playSound(canonical, Sound.BLOCK_WOODEN_DOOR_OPEN, 0.15f, 1.2f);
            }
            case FAIL -> {
                player.sendTitle(ChatColor.RED + "Failed!", "", 5, 40, 10);
                // Loud world sound — others nearby hear the failed pick
                canonical.getWorld().playSound(canonical, Sound.BLOCK_IRON_TRAPDOOR_OPEN, 4.5f, 0.8f);
            }
            case BREAK -> {
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getAmount() > 1) {
                    held.setAmount(held.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                player.sendTitle(ChatColor.RED + "Lockpick broke!", "", 5, 40, 10);
                // Loud world sounds — pick snapping is audible
                canonical.getWorld().playSound(canonical, Sound.BLOCK_IRON_TRAPDOOR_OPEN, 4.5f, 0.8f);
                canonical.getWorld().playSound(canonical, Sound.ENTITY_ITEM_BREAK, 4.5f, 1f);
            }
            default -> {}
        }
    }

    private void openDoor(Location canonical) {
        Block bottom = canonical.getBlock();
        if (bottom.getBlockData() instanceof Openable openable) {
            openable.setOpen(true);
            bottom.setBlockData(openable);
        }
        // Sync top half for full doors
        if (Tag.DOORS.isTagged(bottom.getType())) {
            Block top = bottom.getRelative(BlockFace.UP);
            if (top.getBlockData() instanceof Openable topOpenable) {
                topOpenable.setOpen(true);
                top.setBlockData(topOpenable);
            }
        }
    }

    private boolean isLockpickItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        for (String path : Cache.lockPickItems) {
            if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, path)) return true;
        }
        return false;
    }

    private double getLockpickStrength(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Parameters.defaultLockpickStrength;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(Keys.lockpickStrength, PersistentDataType.DOUBLE)) return Parameters.defaultLockpickStrength;
        return meta.getPersistentDataContainer().get(Keys.lockpickStrength, PersistentDataType.DOUBLE);
    }

    private int getDexterity(Player player) {
        try {
            return PlayerData.get(player.getUniqueId()).getAttributes().getInstance(Parameters.lockpickAttribute).getTotal();
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isSameDoor(Location a, Location b) {
        if (a == null || b == null) return false;
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}

