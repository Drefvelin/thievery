package net.tfminecraft.thievery.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
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

import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.cache.Parameters;
import net.tfminecraft.thievery.data.ChestLockpickSession;
import net.tfminecraft.thievery.data.ContainerData;
import net.tfminecraft.thievery.data.LockState;
import net.tfminecraft.thievery.data.LockpickDefinition;
import net.tfminecraft.thievery.data.RiskSource;
import net.tfminecraft.thievery.database.Database;
import net.tfminecraft.thievery.holder.StealGuiHolder;
import net.tfminecraft.thievery.util.BundleHandler;
import net.tfminecraft.thievery.util.CategoryResolver;
import net.tfminecraft.thievery.util.ClueChecker;
import net.tfminecraft.thievery.util.ClueDropper;
import net.tfminecraft.thievery.util.DexterityHelper;
import net.tfminecraft.thievery.util.GuildAccessCooldown;
import net.tfminecraft.thievery.util.RiskCalculator;
import net.tfminecraft.thievery.util.StealBudget;
import net.tfminecraft.thievery.util.StealGuiBuilder;
import net.tfminecraft.thievery.util.StealGuiPanes;
import net.tfminecraft.thievery.util.StealGuiRefresher;
import net.tfminecraft.thievery.util.StealGuiTitle;
import net.tfminecraft.thievery.util.StealItemDisplay;
import net.tfminecraft.thievery.util.StealTakePreview;
import net.tfminecraft.thievery.util.TakeCluePreviewDebug;
import net.tfminecraft.thievery.util.TargetKeyResolver;
import net.tfminecraft.thievery.util.ThieveryTexts;
import net.tfminecraft.thievery.util.ToolResolver;
import net.tfminecraft.util.GuildChecker;

public class ContainerManager implements Listener {

    private final ContainerDataManager containerDataManager = new ContainerDataManager();

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
        String baseMessage = ThieveryTexts.msg(ThieveryTexts.STAFF + "[Thievery] " + ThieveryTexts.ERROR + thief.getName()
                + " is taking from a chest they do not own at x"
                + loc.getBlockX() + " y" + loc.getBlockY() + " z" + loc.getBlockZ() + " ");

        String tpCommand = "/tp " + thief.getName();

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.hasPermission("thievery.admin")) return;
            if (!feedbackMap.getOrDefault(p.getUniqueId(), false)) return;

            // Create the base message component
            net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(baseMessage);

            // Create the [TP] clickable component
            net.md_5.bungee.api.chat.TextComponent tpButton = new net.md_5.bungee.api.chat.TextComponent(
                    ThieveryTexts.msg(ThieveryTexts.INFO + "[TP]"));
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
        String baseMessage = ThieveryTexts.msg(ThieveryTexts.STAFF + "[Thievery] " + ThieveryTexts.ERROR + breaker.getName()
                + " broke a container they do not own at x"
                + loc.getBlockX() + " y" + loc.getBlockY() + " z" + loc.getBlockZ() + " ");

        String tpCommand = "/tp " + breaker.getName();

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.hasPermission("thievery.admin")) return;
            if (!feedbackMap.getOrDefault(p.getUniqueId(), false)) return;

            net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(baseMessage);

            net.md_5.bungee.api.chat.TextComponent tpButton = new net.md_5.bungee.api.chat.TextComponent(
                    ThieveryTexts.msg(ThieveryTexts.INFO + "[TP]"));
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
        net.md_5.bungee.api.chat.TextComponent tp = new net.md_5.bungee.api.chat.TextComponent(
                ThieveryTexts.msg(ThieveryTexts.INFO + "[" + label + "]"));
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
                        breaker.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You do not have access to break this container."));
                        alertAdminsContainerBreak(breaker, location);
                        return;
                    }
                }
            }
        }

        // Prevent break if player cannot access this container
        if (!data.canAccess(breaker)) {
            event.setCancelled(true);
            breaker.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You do not have access to break this container."));
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
                player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You do not have access to this container."));
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
                player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You do not have access to this container."));
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
        event.getPlayer().sendTitle(
                ThieveryTexts.msg(ThieveryTexts.ACCENT + "Lock State"),
                ThieveryTexts.msg(ThieveryTexts.WARN + displayState), 5, 30, 10);
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
                player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You can only change the lock state on containers you own."));
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
            player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You can only change the lock state on containers you own."));
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
                player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You do not have access to this container."));
            }
            return;
        }

        Location location = container.getBlock().getLocation();
        ContainerData data = containerDataManager.loadContainerData(location);
        if (data.getOwner() != null && !data.canAccess(player)) {
            event.setCancelled(true);
            player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You do not have access to this container."));
        }
    }

    private void notifyLockStateChange(Player player, LockState lockState) {
        String displayState = formatLockState(lockState);
        player.sendTitle(
                ThieveryTexts.msg(ThieveryTexts.ACCENT + "Lock State"),
                ThieveryTexts.msg(ThieveryTexts.WARN + displayState), 5, 30, 10);
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
        if (Parameters.excludedContainerMaterials.contains(event.getClickedBlock().getType())) return;

        // Prevent opening container normally
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
                player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You lack the needed character trait(s) to lockpick!"));
                return;
            }
        }

        // Only allow lockpicking containers the player cannot already access
        Block clickedBlock = event.getClickedBlock();
        Inventory blockInv = ((Container) clickedBlock.getState()).getInventory();
        if (!Cache.debugAllowOwnChest) {
            if (blockInv instanceof DoubleChestInventory doubleInv) {
                DoubleChest doubleChest = (DoubleChest) doubleInv.getHolder();
                if (doubleChest != null) {
                    Location leftLoc = ((Chest) doubleChest.getLeftSide()).getLocation();
                    Location rightLoc = ((Chest) doubleChest.getRightSide()).getLocation();
                    ContainerData leftData = containerDataManager.loadContainerData(leftLoc);
                    ContainerData rightData = containerDataManager.loadContainerData(rightLoc);
                    if (leftData.canAccess(player) && rightData.canAccess(player)) {
                        player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You already have access to this container."));
                        return;
                    }
                }
            } else {
                ContainerData data = containerDataManager.loadContainerData(clickedBlock.getLocation());
                if (data.canAccess(player)) {
                    player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You already have access to this container."));
                    return;
                }
            }
        }

        var ownerUUID = getContainerOwnerUUID(clickedBlock);
        GuildChecker.LockpickAccessResult access = GuildChecker.checkLockpickAccess(ownerUUID);
        if (access.type == GuildChecker.LockpickAccessResult.Type.DENY) {
            player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + access.message));
            return;
        }
        if (access.type == GuildChecker.LockpickAccessResult.Type.WARN) {
            player.sendMessage(ThieveryTexts.msg(ThieveryTexts.WARN + access.message));
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
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        StealGuiHolder holder = getStealGuiHolder(event.getView().getTopInventory());
        if (holder == null || holder.getKind() != StealGuiHolder.Kind.CHEST) {
            return;
        }
        lockpickingSessions.remove(player.getUniqueId());
    }

    private void lockpickChest(PlayerInteractEvent e) {
        Block b = e.getClickedBlock();
        Player p = e.getPlayer();

        e.setCancelled(true);

        for (ChestLockpickSession active : lockpickingSessions.values()) {
            if (active.getChestBlock().equals(b)) {
                p.sendMessage(ThieveryTexts.msg(ThieveryTexts.CRITICAL + "Someone is already lockpicking this container!"));
                return;
            }
        }

        ItemStack heldLockpick = p.getInventory().getItemInMainHand();
        LockpickDefinition lockpickDef = ToolResolver.resolveLockpick(heldLockpick);
        if (lockpickDef == null) return;

        Location chestLoc = b.getLocation();
        ContainerData data = containerDataManager.loadContainerData(chestLoc);
        UUID playerId = p.getUniqueId();

        if (GuildAccessCooldown.isOnCooldown(data.getAccessMap(), p, Cache.cooldown)) {
            long millisRemaining = GuildAccessCooldown.getMillisRemaining(data.getAccessMap(), p, Cache.cooldown);
            p.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You must wait "
                    + GuildAccessCooldown.formatRemaining(millisRemaining)
                    + " before attempting to lockpick this container again."));
            return;
        }

        BlockState state = b.getState();
        if (!(state instanceof Container container)) return;

        Inventory chestInv = container.getInventory();

        int dexterity = DexterityHelper.getDexterity(p);
        double successChance = ChestLockpickSession.computeSuccessChance(dexterity, lockpickDef.getStrength());

        String targetKey = TargetKeyResolver.resolve(getContainerOwnerUUID(b));
        ChestLockpickSession session = new ChestLockpickSession(b, lockpickDef, successChance, chestInv, targetKey);

        StealGuiHolder holder = new StealGuiHolder(p.getUniqueId(), StealGuiHolder.Kind.CHEST);
        net.tfminecraft.thievery.data.PlayerData thiefData = Thievery.getPlayerManager().get(p.getUniqueId());
        String title = StealGuiTitle.forChest(thiefData, dexterity, lockpickDef.getStrength(), session.getBudget(),
                session.getNextRevealSuccessChance(), session.isLockpickBroken());
        Inventory lockpickInv = StealGuiBuilder.buildHiddenGui(holder, session.getLayout(), title);

        p.openInventory(lockpickInv);
        p.sendMessage(ThieveryTexts.msg("§o" + ThieveryTexts.CRITICAL + "Click a slot to probe the container."));

        String today = GuildAccessCooldown.today();
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

    public void tickOpenChestGuis() {
        for (ChestLockpickSession session : new ArrayList<>(lockpickingSessions.values())) {
            tickOpenChestGui(session);
        }
    }

    public void tickOpenChestGui(ChestLockpickSession session) {
        UUID playerId = null;
        for (Map.Entry<UUID, ChestLockpickSession> entry : lockpickingSessions.entrySet()) {
            if (entry.getValue() == session) {
                playerId = entry.getKey();
                break;
            }
        }
        if (playerId == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        StealGuiHolder holder = getStealGuiHolder(player.getOpenInventory().getTopInventory());
        if (holder == null || holder.getKind() != StealGuiHolder.Kind.CHEST) {
            return;
        }
        int dexterity = DexterityHelper.getDexterity(player);
        net.tfminecraft.thievery.data.PlayerData thiefData = Thievery.getPlayerManager().get(player.getUniqueId());
        double lockpickStrength = session.getLockpickDef().getStrength();
        String title = StealGuiTitle.forChest(thiefData, dexterity, lockpickStrength, session.getBudget(),
                session.getNextRevealSuccessChance(), session.isLockpickBroken());
        StealGuiRefresher.updateTitle(player, holder, title);
    }

    private void revealChestSlot(Player player, ChestLockpickSession session, Inventory guiInv, int guiSlot) {
        if (session.isLockpickBroken()) {
            player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Your lockpick is broken. Take what you revealed or close the chest."));
            return;
        }
        if (session.isRevealed(guiSlot)) {
            return;
        }
        Integer chestSlot = session.getLayout().getLogicalForGui(guiSlot);
        if (chestSlot == null) {
            return;
        }

        int dexterity = DexterityHelper.getDexterity(player);
        double lockpickStrength = session.getLockpickDef().getStrength();
        net.tfminecraft.thievery.data.PlayerData thiefData = Thievery.getPlayerManager().get(player.getUniqueId());
        thiefData.addRiskGain(dexterity, lockpickStrength, RiskSource.CHEST);
        Database.savePlayerData(thiefData);

        if (Math.random() >= session.getNextRevealSuccessChance()) {
            breakLockpick(player);
            session.markLockpickBroken();
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Your lockpick broke!"));
            StealGuiHolder holder = getStealGuiHolder(guiInv);
            if (holder != null) {
                String title = StealGuiTitle.forChest(thiefData, dexterity, lockpickStrength, session.getBudget(),
                session.getNextRevealSuccessChance(), session.isLockpickBroken());
                StealGuiRefresher.updateTitle(player, holder, title);
            }
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
        session.markRevealed(guiSlot);
        refreshRevealedSlots(player, session, guiInv, chestInv, getStealGuiHolder(guiInv), "reveal");
    }

    private void refreshRevealedSlots(Player player, ChestLockpickSession session, Inventory guiInv,
            Inventory chestInv, StealGuiHolder holder, String debugReason) {
        net.tfminecraft.thievery.data.PlayerData thiefData = Thievery.getPlayerManager().get(player.getUniqueId());
        int dexterity = DexterityHelper.getDexterity(player);
        double lockpickStrength = session.getLockpickDef().getStrength();
        StealItemDisplay.ChestCluePreviewContext cluePreview = buildCluePreviewContext(
                player, session, thiefData, dexterity, lockpickStrength);
        double budgetRemaining = session.getBudget().getRemaining();
        boolean guaranteed = session.getSuccessfulClueDrops() < Cache.minCluesContainer;

        for (int revealedGuiSlot : session.getRevealedGuiSlots()) {
            Integer chestSlot = session.getLayout().getLogicalForGui(revealedGuiSlot);
            if (chestSlot == null) {
                continue;
            }
            ItemStack realItem = chestInv.getItem(chestSlot);
            StealTakePreview.TakeValues values = StealTakePreview.estimate(
                    player, thiefData, realItem, budgetRemaining);
            RiskCalculator.TakeCluePreview one = RiskCalculator.computeTakeCluePreview(
                    cluePreview.sessionRisk(), values.valueOne(), dexterity, lockpickStrength, guaranteed);
            RiskCalculator.TakeCluePreview all = RiskCalculator.computeTakeCluePreview(
                    cluePreview.sessionRisk(), values.valueAll(), dexterity, lockpickStrength, guaranteed);
            TakeCluePreviewDebug.log(debugReason, player, session, revealedGuiSlot, realItem,
                    values, cluePreview, one, all, guaranteed);
            StealGuiBuilder.placeRevealedSlot(guiInv, revealedGuiSlot, realItem, session.getBudget(),
                    thiefData, cluePreview);
        }

        if (holder != null) {
            String title = StealGuiTitle.forChest(thiefData, dexterity,
                    lockpickStrength, session.getBudget(),
                    session.getNextRevealSuccessChance(), session.isLockpickBroken());
            StealGuiRefresher.updateTitle(player, holder, title);
        }
    }

    private void refreshRevealedSlots(Player player, ChestLockpickSession session, Inventory guiInv,
            Inventory chestInv, StealGuiHolder holder) {
        refreshRevealedSlots(player, session, guiInv, chestInv, holder, "refresh");
    }

    private static StealItemDisplay.ChestCluePreviewContext buildCluePreviewContext(Player player,
            ChestLockpickSession session, net.tfminecraft.thievery.data.PlayerData thiefData, int dexterity,
            double lockpickStrength) {
        thiefData.applyRiskDecay(dexterity);
        return new StealItemDisplay.ChestCluePreviewContext(
                player,
                dexterity,
                lockpickStrength,
                thiefData.getRisk(),
                session.getSuccessfulClueDrops());
    }

    private void refreshRevealedSlots(Player player, ChestLockpickSession session, Inventory guiInv,
            Inventory chestInv) {
        refreshRevealedSlots(player, session, guiInv, chestInv, getStealGuiHolder(guiInv), "take");
    }

    private void performTake(Player player, ChestLockpickSession session, int guiSlot, Inventory guiInv,
            ClickType clickType, ItemStack clickedItem) {
        if (!session.isRevealed(guiSlot)) {
            return;
        }
        Integer chestSlot = session.getLayout().getLogicalForGui(guiSlot);
        if (chestSlot == null) return;

        Block chestBlock = session.getChestBlock();
        if (!(chestBlock.getState() instanceof Container container)) return;

        Inventory realInv = container.getInventory();
        ItemStack realItem = realInv.getItem(chestSlot);
        if (realItem == null || realItem.getType().isAir()) {
            guiInv.setItem(guiSlot, null);
            player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "The item is no longer there."));
            return;
        }
        if (ClueChecker.isClueItem(realItem)) {
            return;
        }

        net.tfminecraft.thievery.data.PlayerData thiefData = Thievery.getPlayerManager().get(player.getUniqueId());

        if (BundleHandler.isBundle(realItem)
                && BundleHandler.hasStealableContents(thiefData, realItem, session.getBudget().getRemaining())) {
            performBundleTake(player, session, guiInv, realInv, chestSlot, guiSlot, chestBlock, realItem, thiefData,
                    clickType);
            return;
        }

        int maxByBudget = StealBudget.computeTakeableAmount(realItem, session.getBudget().getRemaining());
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
                player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You don't have enough inventory space!"));
                return;
            }
            takeAmount = Math.min(realItem.getAmount(), Math.min(maxByBudget, maxFit));
        } else {
            takeAmount = Math.min(1, Math.min(realItem.getAmount(), maxByBudget));
            if (maxFitInPlayerInventory(player, realItem, 1) < 1) {
                player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You don't have enough inventory space!"));
                return;
            }
        }

        if (takeAmount <= 0) return;

        ItemStack toGive = realItem.clone();
        toGive.setAmount(takeAmount);

        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(toGive);
        if (!leftovers.isEmpty()) {
            player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You don't have enough inventory space!"));
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
                DexterityHelper.getDexterity(player), session.getLockpickDef().getStrength(),
                CategoryResolver.getTotalValue(toGive));
        refreshRevealedSlots(player, session, guiInv, realInv);
    }

    private void performBundleTake(Player player, ChestLockpickSession session, Inventory guiInv,
            Inventory realInv, int chestSlot, int guiSlot, Block chestBlock, ItemStack realItem,
            net.tfminecraft.thievery.data.PlayerData thiefData, ClickType clickType) {
        BundleHandler.BundleTakeMode mode = clickType == ClickType.SHIFT_LEFT
                ? BundleHandler.BundleTakeMode.GREEDY
                : BundleHandler.BundleTakeMode.ONE;

        BundleHandler.BundleTakeResult result = BundleHandler.takeFromBundle(
                realItem, player, thiefData, session.getBudget().getRemaining(), mode);
        if (!result.isAnyTaken()) {
            player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You don't have enough inventory space!"));
            return;
        }

        if (result.isRemovedFromSource()) {
            realInv.setItem(chestSlot, null);
        } else {
            realInv.setItem(chestSlot, result.getUpdatedBundle());
        }
        session.addCapacityUsed(result.getValueTaken());
        if (Cache.coreProtect) {
            Thievery.getCoreProtect().logContainerTransaction(player.getName() + "_lockpick", chestBlock.getLocation());
        }

        ClueDropper.tryDropChestClue(player, session, chestBlock, realInv, chestSlot,
                DexterityHelper.getDexterity(player), session.getLockpickDef().getStrength(),
                result.getValueTaken(), true);
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
        StealGuiHolder holder = getStealGuiHolder(e.getView().getTopInventory());
        if (clickedInv == null || holder == null || holder.getKind() != StealGuiHolder.Kind.CHEST) return;

        e.setCancelled(true);

        ChestLockpickSession session = lockpickingSessions.get(uuid);
        if (session == null) return;

        ItemStack clickedItem = e.getCurrentItem();
        int slot = e.getSlot();

        if (clickedInv != e.getView().getTopInventory()) return;

        if (StealGuiPanes.isUnknownPane(clickedItem) && !session.isRevealed(slot)) {
            revealChestSlot(player, session, e.getView().getTopInventory(), slot);
            return;
        }

        if (clickedItem == null || StealGuiPanes.isNonInteractivePane(clickedItem)
                || StealItemDisplay.isStealPane(clickedItem) && !session.isRevealed(slot)) return;

        ClickType click = e.getClick();
        if (click != ClickType.LEFT && click != ClickType.SHIFT_LEFT) return;

        if (!session.isRevealed(slot)) return;

        performTake(player, session, slot, e.getView().getTopInventory(), click, clickedItem);
    }

    private static StealGuiHolder getStealGuiHolder(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryHolder inventoryHolder = inventory.getHolder();
        return inventoryHolder instanceof StealGuiHolder stealGuiHolder ? stealGuiHolder : null;
    }
}