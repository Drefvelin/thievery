package net.tfminecraft.thievery.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.data.PickpocketSession;
import net.tfminecraft.thievery.data.PlayerData;
import net.tfminecraft.thievery.data.PlayerTargetData;
import net.tfminecraft.thievery.data.RiskSource;
import net.tfminecraft.thievery.database.Database;
import net.tfminecraft.thievery.holder.StealGuiHolder;
import net.tfminecraft.thievery.loader.PickpocketLoader;
import net.tfminecraft.thievery.util.DexterityHelper;
import net.tfminecraft.thievery.util.GuildAccessCooldown;
import net.tfminecraft.thievery.util.PickpocketVictimAlerter;
import net.tfminecraft.thievery.util.PlayerInteractCooldown;
import net.tfminecraft.thievery.util.PlayerSlotMap;
import net.tfminecraft.thievery.util.PlayerStealSource;
import net.tfminecraft.thievery.util.RobberyUtil;
import net.tfminecraft.thievery.util.StealBudget;
import net.tfminecraft.thievery.util.StealGuiBuilder;
import net.tfminecraft.thievery.util.StealGuiLayout;
import net.tfminecraft.thievery.util.StealGuiPanes;
import net.tfminecraft.thievery.util.StealGuiRefresher;
import net.tfminecraft.thievery.util.StealGuiTitle;
import net.tfminecraft.thievery.util.StealItemDisplay;
import net.tfminecraft.thievery.util.StealTakeHandler;
import net.tfminecraft.thievery.util.ThieveryTexts;

public class PickpocketManager implements Listener {

    private final PlayerTargetDataManager targetDataManager = new PlayerTargetDataManager();
    private final Set<UUID> awaitingTarget = new java.util.HashSet<>();
    private final Map<UUID, PickpocketSession> sessionsByPickpocket = new HashMap<>();
    private final Map<UUID, BukkitTask> distanceWatchTasks = new HashMap<>();

    public void startAwaitingTarget(Player pickpocket) {
        endSession(pickpocket.getUniqueId(), false);
        awaitingTarget.add(pickpocket.getUniqueId());
        pickpocket.sendMessage(ThieveryTexts.msg(ThieveryTexts.WARN + "Right-click a player within "
                + PickpocketLoader.getMaxDistance() + " blocks to pickpocket them."));
    }

    public boolean isAwaitingTarget(UUID pickpocketId) {
        return awaitingTarget.contains(pickpocketId);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player pickpocket = event.getPlayer();
        UUID pickpocketId = pickpocket.getUniqueId();

        if (!awaitingTarget.contains(pickpocketId)) {
            return;
        }
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Player victim)) {
            return;
        }
        if (!PlayerInteractCooldown.tryAcquire(pickpocketId)) {
            return;
        }
        if (victim.getUniqueId().equals(pickpocketId)) {
            pickpocket.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You cannot pickpocket yourself."));
            return;
        }
        if (!RobberyUtil.isWithinRange(pickpocket, victim, PickpocketLoader.getMaxDistance())) {
            pickpocket.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "That player is too far away."));
            return;
        }

        PlayerTargetData targetData = targetDataManager.load(victim.getUniqueId());
        if (GuildAccessCooldown.isOnCooldownMillis(targetData.getPickpocketAccessMap(), pickpocket,
                PickpocketLoader.getCooldownMillis())) {
            long remaining = GuildAccessCooldown.getMillisRemainingMillis(targetData.getPickpocketAccessMap(),
                    pickpocket, PickpocketLoader.getCooldownMillis());
            pickpocket.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Your guild must wait "
                    + GuildAccessCooldown.formatRemaining(remaining)
                    + " before targeting this player again."));
            return;
        }

        awaitingTarget.remove(pickpocketId);
        endSession(pickpocketId, false);

        long now = System.currentTimeMillis();
        GuildAccessCooldown.recordAccessMillis(targetData.getPickpocketAccessMap(), pickpocketId, now);
        targetDataManager.save(targetData);

        StealGuiLayout layout = StealGuiLayout.create(PlayerSlotMap.MAIN_INV_SLOT_COUNT);
        StealBudget budget = new StealBudget(PickpocketLoader.getBudget());
        PickpocketSession session = new PickpocketSession(pickpocketId, victim.getUniqueId(), budget, layout);
        sessionsByPickpocket.put(pickpocketId, session);

        openPickpocketGui(pickpocket, victim, session);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        StealGuiHolder holder = getStealGuiHolder(event.getView().getTopInventory());
        if (holder == null || holder.getKind() != StealGuiHolder.Kind.PICKPOCKET) {
            return;
        }
        event.setCancelled(true);

        PickpocketSession session = sessionsByPickpocket.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        int guiSlot = event.getSlot();
        Inventory guiInv = event.getView().getTopInventory();

        if (StealGuiPanes.isUnknownPane(clickedItem) && !session.isRevealed(guiSlot)) {
            revealPickpocketSlot(player, session, guiInv, guiSlot);
            return;
        }

        if (clickedItem == null || StealGuiPanes.isNonInteractivePane(clickedItem)
                || StealItemDisplay.isStealPane(clickedItem) && !session.isRevealed(guiSlot)) {
            return;
        }

        ClickType click = event.getClick();
        if (click != ClickType.LEFT && click != ClickType.SHIFT_LEFT) {
            return;
        }

        if (!session.isRevealed(guiSlot)) {
            return;
        }

        Integer logicalSlot = session.getLayout().getLogicalForGui(guiSlot);
        if (logicalSlot == null) {
            return;
        }
        int playerSlot = PlayerSlotMap.toPlayerSlot(logicalSlot);
        if (playerSlot < 0) {
            return;
        }

        Player victim = Bukkit.getPlayer(session.getVictimId());
        if (victim == null || !victim.isOnline()) {
            player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Your target is no longer available."));
            endSession(player.getUniqueId(), true);
            return;
        }

        PlayerStealSource source = new PlayerStealSource(victim);
        StealTakeHandler.performTake(player, source, session.getBudget(), playerSlot, guiInv, guiSlot, click,
                clickedItem, () -> refreshRevealedSlots(player, victim, session, guiInv, holder));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        StealGuiHolder holder = getStealGuiHolder(event.getView().getTopInventory());
        if (holder == null || holder.getKind() != StealGuiHolder.Kind.PICKPOCKET) {
            return;
        }
        if (sessionsByPickpocket.containsKey(player.getUniqueId())) {
            endSession(player.getUniqueId(), false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        awaitingTarget.remove(playerId);
        PlayerInteractCooldown.clear(playerId);
        endSession(playerId, false);
    }

    public void tickOpenGuis() {
        for (PickpocketSession session : new ArrayList<>(sessionsByPickpocket.values())) {
            tickOpenGui(session);
        }
    }

    public void tickOpenGui(PickpocketSession session) {
        Player pickpocket = Bukkit.getPlayer(session.getPickpocketId());
        if (pickpocket == null || !pickpocket.isOnline()) {
            return;
        }
        StealGuiHolder holder = getStealGuiHolder(pickpocket.getOpenInventory().getTopInventory());
        if (holder == null || holder.getKind() != StealGuiHolder.Kind.PICKPOCKET) {
            return;
        }

        int dexterity = DexterityHelper.getDexterity(pickpocket);
        PlayerData thiefData = Thievery.getPlayerManager().get(pickpocket.getUniqueId());
        String title = StealGuiTitle.forPickpocket(thiefData, dexterity, session.getBudget());
        StealGuiRefresher.updateTitle(pickpocket, holder, title);
    }

    private void openPickpocketGui(Player pickpocket, Player victim, PickpocketSession session) {
        StealGuiHolder holder = new StealGuiHolder(pickpocket.getUniqueId(), StealGuiHolder.Kind.PICKPOCKET);
        int dexterity = DexterityHelper.getDexterity(pickpocket);
        PlayerData thiefData = Thievery.getPlayerManager().get(pickpocket.getUniqueId());
        String title = StealGuiTitle.forPickpocket(thiefData, dexterity, session.getBudget());
        Inventory gui = StealGuiBuilder.buildHiddenGui(holder, session.getLayout(), title);
        pickpocket.openInventory(gui);
        pickpocket.sendMessage(ThieveryTexts.msg("§o" + ThieveryTexts.CRITICAL
                + "Click a slot to probe their pockets."));
        startDistanceWatch(pickpocket, victim, session);
    }

    private void revealPickpocketSlot(Player pickpocket, PickpocketSession session, Inventory guiInv, int guiSlot) {
        Integer logicalSlot = session.getLayout().getLogicalForGui(guiSlot);
        if (logicalSlot == null || session.isRevealed(guiSlot)) {
            return;
        }

        Player victim = Bukkit.getPlayer(session.getVictimId());
        if (victim == null || !victim.isOnline()) {
            pickpocket.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Your target is no longer available."));
            endSession(pickpocket.getUniqueId(), true);
            return;
        }

        int dexterity = DexterityHelper.getDexterity(pickpocket);
        PlayerData thiefData = Thievery.getPlayerManager().get(pickpocket.getUniqueId());
        thiefData.addRiskGain(dexterity, 0, RiskSource.PICKPOCKET);
        Database.savePlayerData(thiefData);

        PickpocketVictimAlerter.tryAlert(pickpocket, victim, thiefData, session.getTargetKey(), dexterity);

        ItemStack realItem = PlayerSlotMap.getPickpocketItem(victim, logicalSlot);
        StealGuiBuilder.placeRevealedSlot(guiInv, guiSlot, realItem, session.getBudget(), thiefData);
        session.markRevealed(guiSlot);

        pickpocket.playSound(pickpocket.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.4f, 0.8f);
        pickpocket.playSound(pickpocket.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.3f, 1.2f);

        StealGuiHolder holder = getStealGuiHolder(guiInv);
        if (holder != null) {
            String title = StealGuiTitle.forPickpocket(thiefData, dexterity, session.getBudget());
            StealGuiRefresher.updateTitle(pickpocket, holder, title);
        }
    }

    private void refreshRevealedSlots(Player pickpocket, Player victim, PickpocketSession session,
            Inventory guiInv, StealGuiHolder holder) {
        PlayerData thiefData = Thievery.getPlayerManager().get(pickpocket.getUniqueId());
        for (int guiSlot : session.getRevealedGuiSlots()) {
            Integer logicalSlot = session.getLayout().getLogicalForGui(guiSlot);
            if (logicalSlot == null) {
                continue;
            }
            ItemStack realItem = PlayerSlotMap.getPickpocketItem(victim, logicalSlot);
            StealGuiBuilder.placeRevealedSlot(guiInv, guiSlot, realItem, session.getBudget(), thiefData);
        }
        int dexterity = DexterityHelper.getDexterity(pickpocket);
        String title = StealGuiTitle.forPickpocket(thiefData, dexterity, session.getBudget());
        StealGuiRefresher.updateTitle(pickpocket, holder, title);
    }

    private void startDistanceWatch(Player pickpocket, Player victim, PickpocketSession session) {
        cancelDistanceWatch(pickpocket.getUniqueId());
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(Thievery.getInstance(), () -> {
            PickpocketSession current = sessionsByPickpocket.get(pickpocket.getUniqueId());
            if (current != session) {
                cancelDistanceWatch(pickpocket.getUniqueId());
                return;
            }
            if (!victim.isOnline() || !RobberyUtil.isWithinRange(pickpocket, victim, PickpocketLoader.getMaxDistance())) {
                if (pickpocket.isOnline()) {
                    pickpocket.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Your target moved too far away."));
                    pickpocket.closeInventory();
                }
                endSession(pickpocket.getUniqueId(), false);
            }
        }, 20L, 20L);
        distanceWatchTasks.put(pickpocket.getUniqueId(), task);
    }

    private void cancelDistanceWatch(UUID pickpocketId) {
        BukkitTask task = distanceWatchTasks.remove(pickpocketId);
        if (task != null) {
            task.cancel();
        }
    }

    private void endSession(UUID pickpocketId, boolean closeInventory) {
        cancelDistanceWatch(pickpocketId);
        PickpocketSession session = sessionsByPickpocket.remove(pickpocketId);
        if (session == null) {
            return;
        }
        if (closeInventory) {
            Player pickpocket = Bukkit.getPlayer(pickpocketId);
            if (pickpocket != null && pickpocket.isOnline()) {
                pickpocket.closeInventory();
            }
        }
    }

    private static StealGuiHolder getStealGuiHolder(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof StealGuiHolder stealGuiHolder ? stealGuiHolder : null;
    }
}
