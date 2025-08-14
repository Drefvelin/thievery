package net.tfminecraft.thievery.manager;

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
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.TLibs.TLibs;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.attribute.PlayerAttributes.AttributeInstance;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.data.ContainerData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ContainerManager implements Listener {

    private final ContainerDataManager containerDataManager = new ContainerDataManager();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final Map<UUID, Boolean> feedbackMap = new HashMap<>();
    private final Map<UUID, Long> lastAlertTimestamps = new HashMap<>();

    private final Map<UUID, Block> lockpickingSessions = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeLockpickingTasks = new HashMap<>();
    private final Map<UUID, List<Integer>> lockpickingSlotOrders = new HashMap<>();

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

        // Alert if broken by someone who doesn't own it
        if (!data.owns(breaker)) {
            alertAdminsContainerBreak(breaker, location);
        }

        // Delete container data file
        boolean deleted = containerDataManager.deleteContainerData(location);
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

            if (!data.owns(player)) {
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
        String today = dateFormat.format(new Date());

        if (holder instanceof DoubleChest doubleChest) {
            Container left = (Container) doubleChest.getLeftSide();
            Container right = (Container) doubleChest.getRightSide();

            Location mainLoc = left.getBlock().getLocation();
            ContainerData mainData = containerDataManager.loadContainerData(mainLoc);

            if (mainData.getOwner() == null) {
                mainData.setOwner(player.getUniqueId());
            }

            mainData.updateAccess(player.getUniqueId(), today);
            containerDataManager.saveContainerData(mainData);

            Location rightLoc = right.getBlock().getLocation();
            ContainerData rightData = containerDataManager.loadContainerData(rightLoc);
            rightData.setOwner(mainData.getOwner());
            rightData.updateAccess(player.getUniqueId(), today);
            containerDataManager.saveContainerData(rightData);

        } else if (holder instanceof Container container) {
            Location location = container.getBlock().getLocation();
            handleContainerAccess(location, player, today); // only sets date
        }
    }

    private void handleContainerAccess(Location location, Player player, String today) {
        ContainerData data = containerDataManager.loadContainerData(location);

        // Assign owner if not set
        if (data.getOwner() == null) {
            data.setOwner(player.getUniqueId());
        }

        // Track access
        data.updateAccess(player.getUniqueId(), today);
        containerDataManager.saveContainerData(data);
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
                p.spawnParticle(Particle.REDSTONE, base.clone().add(i, yOffset, min), 1, 0, 0, 0, 0, red);
                p.spawnParticle(Particle.REDSTONE, base.clone().add(i, yOffset, max), 1, 0, 0, 0, 0, red);
                p.spawnParticle(Particle.REDSTONE, base.clone().add(min, yOffset, i), 1, 0, 0, 0, 0, red);
                p.spawnParticle(Particle.REDSTONE, base.clone().add(max, yOffset, i), 1, 0, 0, 0, 0, red);
            }
        }

        // Vertical edges
        for (double y = min; y <= max; y += spacing) {
            p.spawnParticle(Particle.REDSTONE, base.clone().add(min, y, min), 1, 0, 0, 0, 0, red);
            p.spawnParticle(Particle.REDSTONE, base.clone().add(min, y, max), 1, 0, 0, 0, 0, red);
            p.spawnParticle(Particle.REDSTONE, base.clone().add(max, y, min), 1, 0, 0, 0, 0, red);
            p.spawnParticle(Particle.REDSTONE, base.clone().add(max, y, max), 1, 0, 0, 0, 0, red);
        }
    }

    private void pingNearbyContainers(Player player, Location origin, String date) {
        int radius = Cache.radius;
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
                        showParticleOutline(player, leftLoc, Particle.REDSTONE);
                        showParticleOutline(player, rightLoc, Particle.REDSTONE);
                    } else {
                        // Single container
                        alreadyPinged.add(checkLoc);

                        ContainerData data = containerDataManager.loadContainerData(checkLoc);
                        data.updateAccess(uuid, date);
                        containerDataManager.saveContainerData(data);
                        showParticleOutline(player, checkLoc, Particle.REDSTONE);
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
        if(!TLibs.getItemAPI().getChecker().checkItemWithPath(heldItem, Cache.lockpick)) return;

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

        lockpickChest(event); // proceed to start the system
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Only cancel if it's an active lockpicking session
        if (!lockpickingSessions.containsKey(playerId)) return;

        // Cancel the task if it's running
        BukkitRunnable task = activeLockpickingTasks.remove(playerId);
        if (task != null) {
            player.sendMessage(ChatColor.GRAY + "You stopped lockpicking.");
            task.cancel();
        }

        // Remove session data
        lockpickingSessions.remove(playerId);
        lockpickingSlotOrders.remove(playerId);
    }

    private void lockpickChest(PlayerInteractEvent e) {
        Block b = e.getClickedBlock();
        Player p = e.getPlayer();

        e.setCancelled(true);

        if (lockpickingSessions.containsValue(b)) {
            p.sendMessage(ChatColor.DARK_RED + "Someone is already lockpicking this chest!");
            return;
        }
        Location chestLoc = b.getLocation();
        ContainerData data = containerDataManager.loadContainerData(chestLoc);
        UUID playerId = p.getUniqueId();

        String lastAccessDate = data.getLastAccess(playerId);
        if (lastAccessDate != null) {
            try {
                Date lastAccess = dateFormat.parse(lastAccessDate); // same format as used earlier
                Duration duration = Duration.between(
                    lastAccess.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(),
                    LocalDate.now().atStartOfDay(ZoneId.systemDefault())
                );

                long daysSince = duration.toDays();
                long daysRemaining = Cache.cooldown - daysSince;

                if (daysRemaining > 0) {
                    // Reconstruct full duration for exact time left
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
                // allow lockpick if there's a parsing error (or optionally deny)
            }
        }                         

        p.sendMessage(ChatColor.ITALIC + "" + ChatColor.DARK_RED + "Lockpicking in progress...");

        BlockState state = b.getState();
        if (!(state instanceof Container container)) return;

        Inventory chestInv = container.getInventory();
        int invSize = chestInv.getSize();
        Inventory lockpickInv = Bukkit.createInventory(null, invSize, ChatColor.DARK_RED + "Lockpicking...");


        NamespacedKey dummyKey = new NamespacedKey(Thievery.getInstance(), "dummy");

        ItemStack unkPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta unkMeta = unkPane.getItemMeta();
        unkMeta.setDisplayName("???");
        unkMeta.getPersistentDataContainer().set(dummyKey, PersistentDataType.BYTE, (byte) 1);
        unkPane.setItemMeta(unkMeta);

        ItemStack curPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta curMeta = curPane.getItemMeta();
        curMeta.setDisplayName("Searching");
        curMeta.getPersistentDataContainer().set(dummyKey, PersistentDataType.BYTE, (byte) 1);
        curPane.setItemMeta(curMeta);

        List<Integer> randomizedSlots = IntStream.range(0, invSize).boxed().collect(Collectors.toList());
        Collections.shuffle(randomizedSlots);
        lockpickingSlotOrders.put(p.getUniqueId(), randomizedSlots);

        boolean start = true;
        for (Integer slot : randomizedSlots) {
            lockpickInv.setItem(slot, start ? curPane : unkPane);
            start = false;
        }

        AttributeInstance dexterityAttr = PlayerData.get(p.getUniqueId()).getAttributes().getInstance("dexterity");
        int dexterity = dexterityAttr.getTotal(); // dexterity ranges from 0 to 40

        // linear interp for each one
        double minSuccess = Cache.minSuccess;
        double maxSuccess = Cache.maxSuccess;
        double lockpickSuccessRate = minSuccess + ((maxSuccess - minSuccess) * dexterity / 40.0);

        double minBreak = Cache.minBreak;
        double maxBreak = Cache.maxBreak;
        double lockpickBreakChance = maxBreak - ((maxBreak - minBreak) * dexterity / 40.0);

        long minDelay = 5L;
        long maxDelay = 20L;
        long lockpickDelay = maxDelay - (long)((maxDelay - minDelay) * dexterity / 40.0);


        p.openInventory(lockpickInv);
        String today = dateFormat.format(new Date());
        pingNearbyContainers(p, b.getLocation(), today);

        // Write to both sides if it's a double chest
        BlockState chestState = b.getState();
        if (chestState instanceof Container chest) {
            Inventory inventory = chest.getInventory();
            if (inventory instanceof DoubleChestInventory doubleChestInventory) {
                DoubleChest doubleChest = (DoubleChest) doubleChestInventory.getHolder();
                if (doubleChest != null) {
                    Chest leftChest = (Chest) doubleChest.getLeftSide();
                    Chest rightChest = (Chest) doubleChest.getRightSide();

                    Location leftLoc = leftChest.getLocation();
                    Location rightLoc = rightChest.getLocation();

                    ContainerData leftData = containerDataManager.loadContainerData(leftLoc);
                    ContainerData rightData = containerDataManager.loadContainerData(rightLoc);

                    leftData.updateAccess(playerId, today);
                    rightData.updateAccess(playerId, today);

                    containerDataManager.saveContainerData(leftData);
                    containerDataManager.saveContainerData(rightData);
                }
            } else {
                // Single chest
                data.updateAccess(playerId, today);
                containerDataManager.saveContainerData(data);
            }
        }
        lockpickingSessions.put(p.getUniqueId(), b);

        BukkitRunnable task = new BukkitRunnable() {
            int slot = 0;
            @Override
            public void run() {
                if (!(b.getState() instanceof Container)) {
                    cleanup();
                    return;
                }

                if (slot >= invSize) {
                    cleanup();
                    return;
                }

                if (Math.random() < lockpickBreakChance) {
                    p.getInventory().setItemInMainHand(null);
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                    p.sendMessage(ChatColor.RED + "Your lockpick broke!");
                    cleanup();
                    return;
                }

                p.playSound(p.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.4f, 0.8f);
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.3f, 1.2f);

                int chestSlot = randomizedSlots.get(slot);
                ItemStack realItem = chestInv.getItem(chestSlot);

                if (realItem != null && Math.random() < lockpickSuccessRate) {
                    lockpickInv.setItem(chestSlot, realItem.clone());
                } else {
                    ItemStack failPane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    ItemMeta failMeta = failPane.getItemMeta();
                    failMeta.setDisplayName(ChatColor.RED + "Nothing found.");
                    failMeta.getPersistentDataContainer().set(dummyKey, PersistentDataType.BYTE, (byte) 1);
                    failPane.setItemMeta(failMeta);
                    lockpickInv.setItem(chestSlot, failPane);
                }

                if (slot < invSize - 1) {
                    int nextSlot = randomizedSlots.get(slot + 1);
                    lockpickInv.setItem(nextSlot, curPane);
                }

                slot++;
            }

            void cleanup() {
                this.cancel();
                activeLockpickingTasks.remove(p.getUniqueId());
            }
        };

        activeLockpickingTasks.put(p.getUniqueId(), task);
        task.runTaskTimer(Thievery.getInstance(), lockpickDelay / 2, lockpickDelay);
    }

    @EventHandler
    public void takeLoot(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        UUID uuid = player.getUniqueId();

        Inventory clickedInv = e.getClickedInventory();
        if (clickedInv == null || !e.getView().getTitle().equals(ChatColor.DARK_RED + "Lockpicking...")) return;

        ItemStack clickedItem = e.getCurrentItem();
        e.setCancelled(true);
        if (clickedItem == null || isDummyPane(clickedItem)) {
            return;
        }
        if(!lockpickingSessions.containsKey(uuid)) return;

        // Only allow left click in the top inventory
        if (e.getClickedInventory() == e.getView().getTopInventory()) {
            ClickType click = e.getClick();

            if (click == ClickType.LEFT) {
                // Give the item to the player
                HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(clickedItem.clone());

                // If it didn't fully fit, don't take it out
                if (!leftovers.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You don't have enough inventory space!");
                    e.setCancelled(true);
                    return;
                }

                // Remove from both GUI and real chest
                int slot = e.getSlot();
                clickedInv.setItem(slot, null);

                Block chestBlock = lockpickingSessions.get(uuid);
                if (chestBlock.getState() instanceof Container container) {
                    Inventory realInv = container.getInventory();
                    if(Cache.coreProtect) {
                        Thievery.getCoreProtect().logContainerTransaction(player.getName(), chestBlock.getLocation());
                    }
                    realInv.setItem(slot, null);
                }
            } else {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Only left-clicking is allowed!");
            }
        } else {
            // Prevent putting stuff in from player inventory
            e.setCancelled(true);
        }
    }

    private boolean isDummyPane(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(Thievery.getInstance(), "dummy");

        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}