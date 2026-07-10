package net.tfminecraft.thievery.manager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.data.ChestLockpickSession;
import net.tfminecraft.thievery.data.ContainerData;
import net.tfminecraft.thievery.data.LockState;
import net.tfminecraft.thievery.data.LockpickDefinition;
import net.tfminecraft.thievery.data.RiskSource;
import net.tfminecraft.thievery.database.Database;
import net.tfminecraft.thievery.util.BundleHandler;
import net.tfminecraft.thievery.util.CategoryResolver;
import net.tfminecraft.thievery.util.ClueChecker;
import net.tfminecraft.thievery.util.ClueDropper;
import net.tfminecraft.thievery.util.DexterityHelper;
import net.tfminecraft.thievery.util.RiskCalculator;
import net.tfminecraft.thievery.util.TargetKeyResolver;
import net.tfminecraft.thievery.util.ToolResolver;
import net.tfminecraft.util.GuildChecker;
import net.tfminecraft.util.Keys;

public class ContainerManager implements Listener {

    private final ContainerDataManager containerDataManager = new ContainerDataManager();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final Map<UUID, Boolean> feedbackMap = new HashMap<>();
    private final Map<UUID, Long> lastAlertTimestamps = new HashMap<>();

    private final Map<UUID, ChestLockpickSession> lockpickingSessions = new HashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("thievery.admin")) {
            enableFeedback(player);
        }
    }

    public void enableFeedback(Player admin) {
        feedbackMap.put(admin.getUniqueId(), true);
    }

    public boolean getFeedbackState(UUID uuid) {
        return feedbackMap.getOrDefault(uuid, false);
    }

    public void setFeedbackState(UUID uuid, boolean value) {
        feedbackMap.put(uuid, value);
    }

    private void alertAdmins(Player thief, Location loc) {
        String baseMessage = "§e[Thievery] §c" + thief.getName() + " is taking from a chest they do not own at x"
                + loc.getBlockX() + " y" + loc.getBlockY() + " z" + loc.getBlockZ() + " ";

        String tpCommand = "/tp " + thief.getName();

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.hasPermission("thievery.admin")) return;
            if (!feedbackMap.getOrDefault(p.getUniqueId(), false)) return;

            // Create the base message component
            net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(baseMessage);

            // Create the [TP] clickable component
            net.md_5.bungee.api.chat.TextComponent tpButton = new net.md_5.bungee.api.chat.TextComponent("§b[TP]");
            tpButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                    net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, tpCommand
            ));
            tpButton.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                    net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                    new net.md_5.bungee.api.chat.ComponentBuilder("Click to teleport to thief").create()
            ));

            // Combine them
            message.addExtra(tpButton);

            // Send as one message
            p.spigot().sendMessage(message);
        });
    }

    private void alertAdminsContainerBreak(Player breaker, Location loc) {
        String baseMessage = "§e[Thievery] §c" + breaker.getName() + " broke a container they do not own at x"
                + loc.getBlockX() + " y" + loc.getBlockY() + " z" + loc.getBlockZ() + " ";

        String tpCommand = "/tp " + breaker.getName();

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.hasPermission("thievery.admin")) return;
            if (!feedbackMap.getOrDefault(p.getUniqueId(), false)) return;

            net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(baseMessage);

            net.md_5.bungee.api.chat.TextComponent tpButton = new net.md_5.bungee.api.chat.TextComponent("§b[TP]");
            tpButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                    net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, tpCommand
            ));
            tpButton.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                    net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                    new net.md_5.bungee.api.chat.ComponentBuilder("Click to teleport to player").create()
            ));

            message.addExtra(tpButton);
            p.spigot().sendMessage(message);
        });
    }


    private net.md_5.bungee.api.chat.TextComponent clickableText(String label, String command) {
        net.md_5.bungee.api.chat.TextComponent tp = new net.md_5.bungee.api.chat.TextComponent("§b[" + label + "]");
        tp.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, command
        ));
        tp.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("Click to teleport to thief").create()
        ));
        return tp;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Only handle containers (chests, barrels, etc.)
        if (!(block.getState() instanceof Container)) return;

        Location location = block.getLocation();
        Player breaker = event.getPlayer();

        ContainerData data = containerDataManager.loadContainerData(location);

        if (block.getState() instanceof Chest chest) {
            Inventory inventory = chest.getInventory();
            if (inventory instanceof DoubleChestInventory doubleChestInventory) {
                DoubleChest doubleChest = (DoubleChest) doubleChestInventory.getHolder();
                if (doubleChest != null) {
                    if (!(doubleChest.getLeftSide() instanceof Chest leftChest)) return;
                    if (!(doubleChest.getRightSide() instanceof Chest rightChest)) return;

                    ContainerData leftData = containerDataManager.loadContainerData(leftChest.getLocation());
                    ContainerData rightData = containerDataManager.loadContainerData(rightChest.getLocation());
                    boolean hasOwner = leftData.getOwner() != null || rightData.getOwner() != null;

                    if (hasOwner && (!leftData.canAccess(breaker) || !rightData.canAccess(breaker))) {
                        event.setCancelled(true);
                        breaker.sendMessage(ChatColor.RED + "You do not have access to break this container.");
                        alertAdminsContainerBreak(breaker, location);
                        return;
                    }
                }
            }
        }

        // Prevent break if player cannot access this container
        if (!data.canAccess(breaker)) {
            event.setCancelled(true);
            breaker.sendMessage(ChatColor.RED + "You do not have access to break this container.");
            alertAdminsContainerBreak(breaker, location);
            return;
        }

        // Delete container data file
        containerDataManager.deleteContainerData(location);
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        InventoryHolder sourceHolder = source.getHolder();

        if (sourceHolder instanceof DoubleChest doubleChest) {
            if (!(doubleChest.getLeftSide() instanceof Chest leftChest)) return;
            if (!(doubleChest.getRightSide() instanceof Chest rightChest)) return;

            ContainerData leftData = containerDataManager.loadContainerData(leftChest.getLocation());
            ContainerData rightData = containerDataManager.loadContainerData(rightChest.getLocation());

            boolean leftLocked = leftData.getOwner() != null && leftData.getLockState() != LockState.PUBLIC;
            boolean rightLocked = rightData.getOwner() != null && rightData.getLockState() != LockState.PUBLIC;
            if (leftLocked || rightLocked) {
                event.setCancelled(true);
            }
            return;
        }

        if (sourceHolder instanceof Container container) {
            ContainerData data = containerDataManager.loadContainerData(container.getBlock().getLocation());
            boolean locked = data.getOwner() != null && data.getLockState() != LockState.PUBLIC;
            if (locked) {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();

        Location location;

        if (holder instanceof DoubleChest doubleChest) {
            // Always use the left side as the primary location
            location = ((Chest) doubleChest.getLeftSide()).getBlock().getLocation();
        } else if (holder instanceof Container container) {
            location = container.getBlock().getLocation();
        } else {
            return;
        }

        // Only care if they're taking items out
        if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

            ContainerData data = containerDataManager.loadContainerData(location);

            if (data.getOwner() == null) return;

            if (!data.canAccess(player)) {
                long now = System.currentTimeMillis();
                long last = lastAlertTimestamps.getOrDefault(player.getUniqueId(), 0L);

                if (now - last >= 30_000) { // 30 seconds
                    alertAdmins(player, location);
                    lastAlertTimestamps.put(player.getUniqueId(), now);
                }
            }
        }
    }


    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof DoubleChest doubleChest) {
            Container left = (Container) doubleChest.getLeftSide();
            Container right = (Container) doubleChest.getRightSide();

            Location mainLoc = left.getBlock().getLocation();
            ContainerData mainData = containerDataManager.loadContainerData(mainLoc);
            Location rightLoc = right.getBlock().getLocation();
            ContainerData rightData = containerDataManager.loadContainerData(rightLoc);

            boolean hasOwner = mainData.getOwner() != null || rightData.getOwner() != null;
            if (hasOwner && (!mainData.canAccess(player) || !rightData.canAccess(player))) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You do not have access to this container.");
                return;
            }

            if (mainData.getOwner() == null) {
                if (rightData.getOwner() != null) {
                    mainData.setOwner(rightData.getOwner());
                    mainData.setLockState(rightData.getLockState());
                } else {
                    mainData.setOwner(player.getUniqueId());
                }
            }

            containerDataManager.saveContainerData(mainData);

            rightData.setOwner(mainData.getOwner());
            rightData.setLockState(mainData.getLockState());
            containerDataManager.saveContainerData(rightData);

        } else if (holder instanceof Container container) {
            Location location = container.getBlock().getLocation();
            ContainerData data = containerDataManager.loadContainerData(location);
            if (data.getOwner() != null && !data.canAccess(player)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You do not have access to this container.");
                return;
            }
            handleContainerAccess(location, player);
        }
    }

    private void handleContainerAccess(Location location, Player player) {
        ContainerData data = containerDataManager.loadContainerData(location);

        // Assign owner if not set
        if (data.getOwner() == null) {
            data.setOwner(player.getUniqueId());
            containerDataManager.saveContainerData(data);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        if (!(block.getState() instanceof Container)) {
            return;
        }

        Location location = block.getLocation();
        ContainerData data = new ContainerData(location, event.getPlayer().getUniqueId());
        containerDataManager.saveContainerData(data);
        String displayState = formatLockState(data.getLockState());
        event.getPlayer().sendTitle(ChatColor.GOLD + "Lock State", ChatColor.YELLOW + displayState, 5, 30, 10);
    }

    @EventHandler
    public void onShiftLeftClickContainer(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Container container)) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        Inventory inventory = container.getInventory();
        UUID playerId = player.getUniqueId();

        if (inventory instanceof DoubleChestInventory doubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest) doubleChestInventory.getHolder();
            if (doubleChest == null) return;

            Location leftLoc = ((Chest) doubleChest.getLeftSide()).getLocation();
            Location rightLoc = ((Chest) doubleChest.getRightSide()).getLocation();

            ContainerData leftData = containerDataManager.loadContainerData(leftLoc);
            ContainerData rightData = containerDataManager.loadContainerData(rightLoc);

            UUID leftOwner = leftData.getOwner();
            UUID rightOwner = rightData.getOwner();
            boolean ownsDoubleChest = playerId.equals(leftOwner) || playerId.equals(rightOwner);
            if (!ownsDoubleChest) {
                player.sendMessage(ChatColor.RED + "You can only change the lock state on containers you own.");
                return;
            }

            LockState nextState = leftData.rotateLockState();
            rightData.setLockState(nextState);

            containerDataManager.saveContainerData(leftData);
            containerDataManager.saveContainerData(rightData);

            notifyLockStateChange(player, nextState);
            return;
        }

        Location location = container.getBlock().getLocation();
        ContainerData data = containerDataManager.loadContainerData(location);

        if (!playerId.equals(data.getOwner())) {
            player.sendMessage(ChatColor.RED + "You can only change the lock state on containers you own.");
            return;
        }

        LockState nextState = data.rotateLockState();
        containerDataManager.saveContainerData(data);
        notifyLockStateChange(player, nextState);
    }

    @EventHandler
    public void onContainerRightClickAccessCheck(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Container container)) return;

        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (ToolResolver.isLockpick(heldItem)) {
            return;
        }

        Inventory inventory = container.getInventory();
        if (inventory instanceof DoubleChestInventory doubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest) doubleChestInventory.getHolder();
            if (doubleChest == null) return;

            Location leftLoc = ((Chest) doubleChest.getLeftSide()).getLocation();
            Location rightLoc = ((Chest) doubleChest.getRightSide()).getLocation();
            ContainerData leftData = containerDataManager.loadContainerData(leftLoc);
            ContainerData rightData = containerDataManager.loadContainerData(rightLoc);

            boolean hasOwner = leftData.getOwner() != null || rightData.getOwner() != null;
            if (hasOwner && (!leftData.canAccess(player) || !rightData.canAccess(player))) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You do not have access to this container.");
            }
            return;
        }

        Location location = container.getBlock().getLocation();
        ContainerData data = containerDataManager.loadContainerData(location);
        if (data.getOwner() != null && !data.canAccess(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You do not have access to this container.");
        }
    }

    private void notifyLockStateChange(Player player, LockState lockState) {
        String displayState = formatLockState(lockState);
        player.sendTitle(ChatColor.GOLD + "Lock State", ChatColor.YELLOW + displayState, 5, 30, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1.0f, 1.0f);
    }

    private String formatLockState(LockState lockState) {
        String value = lockState.name().toLowerCase();
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private void showParticleOutline(Player p, Location loc, Particle particle) {
        World world = loc.getWorld();
        if (world == null) return;

        double spacing = 0.1;
        double min = 0.0;
        double max = 1.0;

        // Use red particle
        Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.0F);
        Location base = loc.clone();

        // Top and bottom outlines
        for (double yOffset : List.of(0.0, 1.0)) {
            for (double i = min; i <= max; i += spacing) {
                p.spawnParticle(Particle.DUST, base.clone().add(i, yOffset, min), 1, 0, 0, 0, 0, red);
                p.spawnParticle(Particle.DUST, base.clone().add(i, yOffset, max), 1, 0, 0, 0, 0, red);
                p.spawnParticle(Particle.DUST, base.clone().add(min, yOffset, i), 1, 0, 0, 0, 0, red);
                p.spawnParticle(Particle.DUST, base.clone().add(max, yOffset, i), 1, 0, 0, 0, 0, red);
            }
        }

        // Vertical edges
        for (double y = min; y <= max; y += spacing) {
            p.spawnParticle(Particle.DUST, base.clone().add(min, y, min), 1, 0, 0, 0, 0, red);
            p.spawnParticle(Particle.DUST, base.clone().add(min, y, max), 1, 0, 0, 0, 0, red);
            p.spawnParticle(Particle.DUST, base.clone().add(max, y, min), 1, 0, 0, 0, 0, red);
            p.spawnParticle(Particle.DUST, base.clone().add(max, y, max), 1, 0, 0, 0, 0, red);
        }
    }

    private void pingNearbyContainers(Player player, Location origin, String date) {
        int radius = Cache.radius;
        if (radius < 0) {
            return;
        }
        UUID uuid = player.getUniqueId();

        Set<Location> alreadyPinged = new HashSet<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Location checkLoc = origin.clone().add(dx, dy, dz);

                    // Avoid pinging the same container twice
                    if (alreadyPinged.contains(checkLoc)) continue;

                    Block block = checkLoc.getBlock();
                    BlockState state = block.getState();

                    if (!(state instanceof Container container)) continue;

                    Inventory inv = container.getInventory();

                    // Handle double chest
                    if (inv instanceof DoubleChestInventory doubleInv) {
                        DoubleChest doubleChest = (DoubleChest) doubleInv.getHolder();

                        Location leftLoc = ((Chest) doubleChest.getLeftSide()).getLocation();
                        Location rightLoc = ((Chest) doubleChest.getRightSide()).getLocation();

                        // Mark both locations as pinged
                        alreadyPinged.add(leftLoc);
                        alreadyPinged.add(rightLoc);

                        ContainerData leftData = containerDataManager.loadContainerData(leftLoc);
                        ContainerData rightData = containerDataManager.loadContainerData(rightLoc);

                        leftData.updateAccess(uuid, date);
                        rightData.updateAccess(uuid, date);

                        containerDataManager.saveContainerData(leftData);
                        containerDataManager.saveContainerData(rightData);
                        showParticleOutline(player, leftLoc, Particle.DUST);
                        showParticleOutline(player, rightLoc, Particle.DUST);
                    } else {
                        // Single container
                        alreadyPinged.add(checkLoc);

                        ContainerData data = containerDataManager.loadContainerData(checkLoc);
                        data.updateAccess(uuid, date);
                        containerDataManager.saveContainerData(data);
                        showParticleOutline(player, checkLoc, Particle.DUST);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onRightClickChest(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Container)) return;

        Player player = event.getPlayer();
        // Only allow if player is holding a lockpick item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!ToolResolver.isLockpick(heldItem)) return;

        // Prevent opening chest normally
        event.setCancelled(true);

        if(Cache.traits.size() > 0) {
            net.tfminecraft.RPCharacters.Objects.PlayerData pd = PlayerManager.get(player);
            if(!pd.hasActiveCharacter()) return;
            RPCharacter character = pd.getActiveCharacter();

            boolean hasTrait = false;
            for(Trait trait : character.getTraits()) {
                if(Cache.traits.contains(trait.getId())) hasTrait = true;
            }
            if(!hasTrait) {
                player.sendMessage(ChatColor.RED + "You lack the needed character trait(s) to lockpick!");
                return;
            }
        }

        // Only allow lockpicking containers the player cannot already access
        Block clickedBlock = event.getClickedBlock();
        Inventory blockInv = ((Container) clickedBlock.getState()).getInventory();
        if (blockInv instanceof DoubleChestInventory doubleInv) {
            DoubleChest doubleChest = (DoubleChest) doubleInv.getHolder();
            if (doubleChest != null) {
                Location leftLoc = ((Chest) doubleChest.getLeftSide()).getLocation();
                Location rightLoc = ((Chest) doubleChest.getRightSide()).getLocation();
                ContainerData leftData = containerDataManager.loadContainerData(leftLoc);
                ContainerData rightData = containerDataManager.loadContainerData(rightLoc);
                if (leftData.canAccess(player) && rightData.canAccess(player)) {
                    player.sendMessage(ChatColor.RED + "You already have access to this container.");
                    return;
                }
            }
        } else {
            ContainerData data = containerDataManager.loadContainerData(clickedBlock.getLocation());
            if (data.canAccess(player)) {
                player.sendMessage(ChatColor.RED + "You already have access to this container.");
                return;
            }
        }

        var ownerUUID = getContainerOwnerUUID(clickedBlock);
        GuildChecker.LockpickAccessResult access = GuildChecker.checkLockpickAccess(ownerUUID);
        if (access.type == GuildChecker.LockpickAccessResult.Type.DENY) {
            player.sendMessage(ChatColor.RED + access.message);
            return;
        }
        if (access.type == GuildChecker.LockpickAccessResult.Type.WARN) {
            player.sendMessage(ChatColor.YELLOW + access.message);
        }

        if (!ClueChecker.hasEnoughClues(player)) {
            ClueChecker.sendInsufficientCluesMessage(player);
            return;
        }

        lockpickChest(event); // proceed to start the system
    }

    private UUID getContainerOwnerUUID(Block block) {
        Inventory inv = ((Container) block.getState()).getInventory();
        if (inv instanceof DoubleChestInventory doubleInv) {
            DoubleChest dc = (DoubleChest) doubleInv.getHolder();
            if (dc != null) {
                ContainerData left = containerDataManager.loadContainerData(((Chest) dc.getLeftSide()).getLocation());
                if (left.getOwner() != null) return left.getOwner();
                ContainerData right = containerDataManager.loadContainerData(((Chest) dc.getRightSide()).getLocation());
                return right.getOwner();
            }
        }
        return containerDataManager.loadContainerData(block.getLocation()).getOwner();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        lockpickingSessions.remove(event.getPlayer().getUniqueId());
    }

    private void lockpickChest(PlayerInteractEvent e) {
        Block b = e.getClickedBlock();
        Player p = e.getPlayer();

        e.setCancelled(true);

        for (ChestLockpickSession active : lockpickingSessions.values()) {
            if (active.getChestBlock().equals(b)) {
                p.sendMessage(ChatColor.DARK_RED + "Someone is already lockpicking this chest!");
                return;
            }
        }

        ItemStack heldLockpick = p.getInventory().getItemInMainHand();
        LockpickDefinition lockpickDef = ToolResolver.resolveLockpick(heldLockpick);
        if (lockpickDef == null) return;

        Location chestLoc = b.getLocation();
        ContainerData data = containerDataManager.loadContainerData(chestLoc);
        UUID playerId = p.getUniqueId();

        String lastAccessDate = getMostRecentGuildAccess(data, p);
        if (lastAccessDate != null) {
            try {
                Date lastAccess = dateFormat.parse(lastAccessDate);
                Duration duration = Duration.between(
                    lastAccess.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(),
                    LocalDate.now().atStartOfDay(ZoneId.systemDefault())
                );

                long daysSince = duration.toDays();
                long daysRemaining = Cache.cooldown - daysSince;

                if (daysRemaining > 0) {
                    long millisElapsed = System.currentTimeMillis() - lastAccess.getTime();
                    long totalCooldownMillis = Cache.cooldown * 24L * 60L * 60L * 1000L;
                    long millisRemaining = totalCooldownMillis - millisElapsed;

                    long hours = (millisRemaining / (1000 * 60 * 60)) % 24;
                    long minutes = (millisRemaining / (1000 * 60)) % 60;

                    p.sendMessage(ChatColor.RED + "You must wait " +
                            daysRemaining + " day(s), " +
                            hours + " hour(s), and " +
                            minutes + " minute(s) before attempting to lockpick this chest again.");
                    return;
                }
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }

        BlockState state = b.getState();
        if (!(state instanceof Container container)) return;

        Inventory chestInv = container.getInventory();
        int chestSize = chestInv.getSize();

        int dexterity = DexterityHelper.getDexterity(p);
        double successChance = ChestLockpickSession.computeSuccessChance(dexterity, lockpickDef.getStrength());

        String targetKey = TargetKeyResolver.resolve(getContainerOwnerUUID(b));
        ChestLockpickSession session = new ChestLockpickSession(b, lockpickDef, successChance, chestInv, targetKey);
        int guiSize = chestSize + 1;
        Inventory lockpickInv = Bukkit.createInventory(null, guiSize, ChatColor.DARK_RED + "Lockpicking...");

        NamespacedKey dummyKey = new NamespacedKey(Thievery.getInstance(), "dummy");
        ItemStack unkPane = createUnknownPane(dummyKey);

        for (int chestSlot = 0; chestSlot < chestSize; chestSlot++) {
            if (chestSlot == ChestLockpickSession.MASK_CHEST_SLOT) continue;
            lockpickInv.setItem(ChestLockpickSession.chestSlotToGui(chestSlot), unkPane);
        }
        net.tfminecraft.thievery.data.PlayerData thiefData = Thievery.getPlayerManager().get(p.getUniqueId());
        lockpickInv.setItem(ChestLockpickSession.SEARCH_GUI_SLOT,
                createSearchButton(thiefData, dexterity, lockpickDef.getStrength()));

        p.openInventory(lockpickInv);
        p.sendMessage(ChatColor.ITALIC + "" + ChatColor.DARK_RED + "Click Search to probe the chest.");

        String today = dateFormat.format(new Date());
        pingNearbyContainers(p, b.getLocation(), today);
        recordContainerAccess(p, b, data, playerId, today);

        lockpickingSessions.put(p.getUniqueId(), session);
    }

    private void recordContainerAccess(Player player, Block b, ContainerData data, UUID playerId, String today) {
        BlockState chestState = b.getState();
        if (!(chestState instanceof Container chest)) return;

        Inventory inventory = chest.getInventory();
        if (inventory instanceof DoubleChestInventory doubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest) doubleChestInventory.getHolder();
            if (doubleChest != null) {
                Chest leftChest = (Chest) doubleChest.getLeftSide();
                Chest rightChest = (Chest) doubleChest.getRightSide();

                ContainerData leftData = containerDataManager.loadContainerData(leftChest.getLocation());
                ContainerData rightData = containerDataManager.loadContainerData(rightChest.getLocation());

                leftData.updateAccess(playerId, today);
                rightData.updateAccess(playerId, today);

                containerDataManager.saveContainerData(leftData);
                containerDataManager.saveContainerData(rightData);
            }
        } else {
            data.updateAccess(playerId, today);
            containerDataManager.saveContainerData(data);
        }
    }

    private void performSearch(Player player, ChestLockpickSession session, Inventory guiInv) {
        if (!session.hasMoreSearches()) {
            player.sendMessage(ChatColor.GRAY + "There is nothing left to search.");
            return;
        }

        Integer chestSlot = session.pollNextChestSlot();
        if (chestSlot == null) return;

        int dexterity = DexterityHelper.getDexterity(player);
        double lockpickStrength = session.getLockpickDef().getStrength();
        net.tfminecraft.thievery.data.PlayerData thiefData = Thievery.getPlayerManager().get(player.getUniqueId());
        thiefData.addRiskGain(dexterity, lockpickStrength, RiskSource.CHEST);
        Database.savePlayerData(thiefData);
        refreshSearchButton(player, session, guiInv);

        NamespacedKey dummyKey = new NamespacedKey(Thievery.getInstance(), "dummy");

        if (Math.random() >= session.getSuccessChance()) {
            breakLockpick(player);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            player.sendMessage(ChatColor.RED + "Your lockpick broke!");
            lockpickingSessions.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.4f, 0.8f);
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.3f, 1.2f);

        Block chestBlock = session.getChestBlock();
        if (!(chestBlock.getState() instanceof Container container)) {
            lockpickingSessions.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }

        Inventory chestInv = container.getInventory();
        ItemStack realItem = chestInv.getItem(chestSlot);
        int guiSlot = session.getRevealGuiSlot(chestSlot);

        if (realItem == null || realItem.getType().isAir()) {
            guiInv.setItem(guiSlot, createNothingPane(dummyKey));
            session.markRevealed(chestSlot, guiSlot);
            return;
        }

        int displayAmount = hasTakeableAmount(realItem, session.getCapacityRemaining(), thiefData) ? 1 : 0;
        ItemStack display = buildRepresentation(realItem, displayAmount, thiefData, dummyKey);
        guiInv.setItem(guiSlot, display);
        session.markRevealed(chestSlot, guiSlot);
    }

    private boolean hasTakeableAmount(ItemStack item, double capacityRemaining,
            net.tfminecraft.thievery.data.PlayerData thiefData) {
        if (BundleHandler.isBundle(item)) {
            return BundleHandler.canStealAnything(thiefData, item, capacityRemaining);
        }
        return ChestLockpickSession.computeTakeableAmount(item, capacityRemaining) > 0;
    }

    private void refreshRevealedSlots(Player player, ChestLockpickSession session, Inventory guiInv,
            Inventory chestInv) {
        net.tfminecraft.thievery.data.PlayerData thiefData = Thievery.getPlayerManager().get(player.getUniqueId());
        NamespacedKey dummyKey = new NamespacedKey(Thievery.getInstance(), "dummy");

        for (Map.Entry<Integer, Integer> entry : new HashMap<>(session.getGuiSlotMappings()).entrySet()) {
            int guiSlot = entry.getKey();
            int chestSlot = entry.getValue();
            ItemStack realItem = chestInv.getItem(chestSlot);

            if (realItem == null || realItem.getType().isAir()) {
                guiInv.setItem(guiSlot, null);
                continue;
            }

            if (!CategoryResolver.canRevealItem(thiefData, realItem)) {
                guiInv.setItem(guiSlot, createHiddenPane(dummyKey));
                continue;
            }

            int displayAmount = hasTakeableAmount(realItem, session.getCapacityRemaining(), thiefData) ? 1 : 0;
            ItemStack display = buildRepresentation(realItem, displayAmount, thiefData, dummyKey);
            guiInv.setItem(guiSlot, display);
        }
    }

    private ItemStack buildRepresentation(ItemStack realItem, int displayAmount,
            net.tfminecraft.thievery.data.PlayerData thiefData, NamespacedKey dummyKey) {
        if (!CategoryResolver.canRevealItem(thiefData, realItem)) {
            return createHiddenPane(dummyKey);
        }
        if (displayAmount <= 0) {
            return null;
        }

        ItemStack display = realItem.clone();
        if (!BundleHandler.isBundle(realItem)) {
            display.setAmount(displayAmount);
        }

        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        if (BundleHandler.isBundle(realItem)) {
            lore.add("§eContents Value: §a"
                    + formatValue(BundleHandler.getRevealableContentsValue(thiefData, realItem)));
            lore.add("§eClick §7to take what you can");
        } else {
            lore.add("§eTotal Value: §a" + formatValue(CategoryResolver.getTotalValue(display)));
            lore.add("§eClick §7to take §bOne");
            if (displayAmount > 1) {
                lore.add("§eShift-Click §7to take §ball");
            }
        }
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private String formatValue(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private void performTake(Player player, ChestLockpickSession session, int guiSlot, Inventory guiInv,
            ClickType clickType, ItemStack clickedItem) {
        Integer chestSlot = session.getChestSlotForGui(guiSlot);
        if (chestSlot == null) return;

        Block chestBlock = session.getChestBlock();
        if (!(chestBlock.getState() instanceof Container container)) return;

        Inventory realInv = container.getInventory();
        ItemStack realItem = realInv.getItem(chestSlot);
        if (realItem == null || realItem.getType().isAir()) {
            guiInv.setItem(guiSlot, null);
            player.sendMessage(ChatColor.RED + "The item is no longer there.");
            return;
        }
        if (ClueChecker.isClueItem(realItem)) {
            return;
        }

        net.tfminecraft.thievery.data.PlayerData thiefData = Thievery.getPlayerManager().get(player.getUniqueId());

        if (BundleHandler.isBundle(realItem)) {
            performBundleTake(player, session, guiInv, realInv, chestSlot, chestBlock, realItem, thiefData);
            return;
        }

        int maxByBudget = ChestLockpickSession.computeTakeableAmount(realItem, session.getCapacityRemaining());
        if (maxByBudget <= 0) {
            guiInv.setItem(guiSlot, null);
            refreshRevealedSlots(player, session, guiInv, realInv);
            return;
        }

        ClickType effectiveClick = clickType;
        if (clickType == ClickType.SHIFT_LEFT
                && (clickedItem == null || clickedItem.getAmount() <= 1)) {
            effectiveClick = ClickType.LEFT;
        }

        int takeAmount;
        if (effectiveClick == ClickType.SHIFT_LEFT) {
            int maxFit = maxFitInPlayerInventory(player, realItem, maxByBudget);
            if (maxFit <= 0) {
                player.sendMessage(ChatColor.RED + "You don't have enough inventory space!");
                return;
            }
            takeAmount = Math.min(realItem.getAmount(), Math.min(maxByBudget, maxFit));
        } else {
            takeAmount = Math.min(1, Math.min(realItem.getAmount(), maxByBudget));
            if (maxFitInPlayerInventory(player, realItem, 1) < 1) {
                player.sendMessage(ChatColor.RED + "You don't have enough inventory space!");
                return;
            }
        }

        if (takeAmount <= 0) return;

        ItemStack toGive = realItem.clone();
        toGive.setAmount(takeAmount);

        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(toGive);
        if (!leftovers.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You don't have enough inventory space!");
            return;
        }

        if (realItem.getAmount() <= takeAmount) {
            realInv.setItem(chestSlot, null);
        } else {
            realItem.setAmount(realItem.getAmount() - takeAmount);
        }

        session.addCapacityUsed(CategoryResolver.getTotalValue(toGive));
        if (Cache.coreProtect) {
            Thievery.getCoreProtect().logContainerTransaction(player.getName() + "_lockpick", chestBlock.getLocation());
        }

        ClueDropper.tryDropChestClue(player, session, chestBlock, realInv, chestSlot,
                DexterityHelper.getDexterity(player), session.getLockpickDef().getStrength());
        refreshRevealedSlots(player, session, guiInv, realInv);
    }

    private void performBundleTake(Player player, ChestLockpickSession session, Inventory guiInv,
            Inventory realInv, int chestSlot, Block chestBlock, ItemStack realItem,
            net.tfminecraft.thievery.data.PlayerData thiefData) {
        if (!BundleHandler.canStealAnything(thiefData, realItem, session.getCapacityRemaining())) {
            guiInv.setItem(session.getRevealGuiSlot(chestSlot), null);
            refreshRevealedSlots(player, session, guiInv, realInv);
            return;
        }

        BundleHandler.BundleTakeResult result = BundleHandler.takeFromBundle(
                realItem, player, thiefData, session.getCapacityRemaining());
        if (!result.isAnyTaken()) {
            player.sendMessage(ChatColor.RED + "You don't have enough inventory space!");
            return;
        }

        realInv.setItem(chestSlot, result.getUpdatedBundle());
        session.addCapacityUsed(result.getValueTaken());
        if (Cache.coreProtect) {
            Thievery.getCoreProtect().logContainerTransaction(player.getName() + "_lockpick", chestBlock.getLocation());
        }

        ClueDropper.tryDropChestClue(player, session, chestBlock, realInv, chestSlot,
                DexterityHelper.getDexterity(player), session.getLockpickDef().getStrength(), true);
        refreshRevealedSlots(player, session, guiInv, realInv);
    }

    private int maxFitInPlayerInventory(Player player, ItemStack prototype, int maxAttempt) {
        if (maxAttempt <= 0) return 0;
        int maxStack = prototype.getMaxStackSize();
        int fit = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                fit += maxStack;
            } else if (slot.isSimilar(prototype) && slot.getAmount() < maxStack) {
                fit += maxStack - slot.getAmount();
            }
            if (fit >= maxAttempt) return maxAttempt;
        }
        return Math.min(maxAttempt, fit);
    }

    private void breakLockpick(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) return;
        if (held.getAmount() > 1) {
            held.setAmount(held.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    @EventHandler
    public void takeLoot(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        Inventory clickedInv = e.getClickedInventory();
        if (clickedInv == null || !e.getView().getTitle().equals(ChatColor.DARK_RED + "Lockpicking...")) return;

        e.setCancelled(true);

        ChestLockpickSession session = lockpickingSessions.get(uuid);
        if (session == null) return;

        ItemStack clickedItem = e.getCurrentItem();
        int slot = e.getSlot();

        if (clickedInv != e.getView().getTopInventory()) return;

        if (slot == ChestLockpickSession.SEARCH_GUI_SLOT && isSearchButton(clickedItem)) {
            performSearch(player, session, e.getView().getTopInventory());
            return;
        }

        if (clickedItem == null || isDummyPane(clickedItem) || isSearchButton(clickedItem)) return;

        ClickType click = e.getClick();
        if (click != ClickType.LEFT && click != ClickType.SHIFT_LEFT) return;

        Integer chestSlot = session.getChestSlotForGui(slot);
        if (chestSlot == null) return;

        performTake(player, session, slot, e.getView().getTopInventory(), click, clickedItem);
    }

    private ItemStack createUnknownPane(NamespacedKey dummyKey) {
        ItemStack unkPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta unkMeta = unkPane.getItemMeta();
        unkMeta.setDisplayName("???");
        unkMeta.getPersistentDataContainer().set(dummyKey, PersistentDataType.BYTE, (byte) 1);
        unkPane.setItemMeta(unkMeta);
        return unkPane;
    }

    private ItemStack createNothingPane(NamespacedKey dummyKey) {
        ItemStack failPane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta failMeta = failPane.getItemMeta();
        failMeta.setDisplayName(ChatColor.RED + "Nothing found.");
        failMeta.getPersistentDataContainer().set(dummyKey, PersistentDataType.BYTE, (byte) 1);
        failPane.setItemMeta(failMeta);
        return failPane;
    }

    private ItemStack createSearchButton(net.tfminecraft.thievery.data.PlayerData thiefData,
            int dexterity, double lockpickStrength) {
        ItemStack search = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = search.getItemMeta();
        meta.setDisplayName("§aSearch");
        double risk = thiefData.getRisk();
        double critical = thiefData.getCriticalChance(dexterity, lockpickStrength);
        List<String> lore = new ArrayList<>(RiskCalculator.formatRiskLore(risk, critical));
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        meta.getPersistentDataContainer().set(Keys.searchButton, PersistentDataType.BYTE, (byte) 1);
        search.setItemMeta(meta);
        return search;
    }

    private void refreshSearchButton(Player player, ChestLockpickSession session, Inventory guiInv) {
        net.tfminecraft.thievery.data.PlayerData thiefData = Thievery.getPlayerManager().get(player.getUniqueId());
        int dexterity = DexterityHelper.getDexterity(player);
        double lockpickStrength = session.getLockpickDef().getStrength();
        guiInv.setItem(ChestLockpickSession.SEARCH_GUI_SLOT,
                createSearchButton(thiefData, dexterity, lockpickStrength));
    }

    private boolean isSearchButton(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(Keys.searchButton, PersistentDataType.BYTE);
    }

    private String getMostRecentGuildAccess(ContainerData data, Player player) {
        String best = data.getLastAccess(player.getUniqueId());
        Guild guild = FactionManager.getGuildByMember(player.getName());
        if (guild == null) return best;
        Date bestDate = null;
        try { if (best != null) bestDate = dateFormat.parse(best); } catch (ParseException ignored) {}
        for (String memberName : guild.getMembers()) {
            if (!memberName.equalsIgnoreCase(player.getName())) {
            } else {
                continue;
            }
            var member = Bukkit.getOfflinePlayer(memberName);
            String access = data.getLastAccess(member.getUniqueId());
            if (access == null) continue;
            try {
                Date d = dateFormat.parse(access);
                if (bestDate == null || d.after(bestDate)) {
                    bestDate = d;
                    best = access;
                }
            } catch (ParseException ignored) {}
        }
        return best;
    }

    private boolean isDummyPane(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(Thievery.getInstance(), "dummy");

        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private ItemStack createHiddenPane(NamespacedKey dummyKey) {
        ItemStack hidden = new ItemStack(Material.BARRIER);
        ItemMeta meta = hidden.getItemMeta();
        meta.setDisplayName("§7HIDDEN!");
        meta.getPersistentDataContainer().set(dummyKey, PersistentDataType.BYTE, (byte) 1);
        hidden.setItemMeta(meta);
        return hidden;
    }
}