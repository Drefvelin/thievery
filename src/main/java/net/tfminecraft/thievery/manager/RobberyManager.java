package net.tfminecraft.thievery.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.data.PlayerData;
import net.tfminecraft.thievery.data.PlayerTargetData;
import net.tfminecraft.thievery.data.RobberySession;
import net.tfminecraft.thievery.data.RobberySession.State;
import net.tfminecraft.thievery.holder.StealGuiHolder;
import net.tfminecraft.thievery.loader.RobberyLoader;
import net.tfminecraft.thievery.util.GuildAccessCooldown;
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
import net.tfminecraft.thievery.util.StealIgnoreRules;
import net.tfminecraft.thievery.util.StealItemDisplay;
import net.tfminecraft.thievery.util.StealTakeHandler;
import net.tfminecraft.thievery.util.ThieveryTexts;

public class RobberyManager implements Listener {

    private final PlayerTargetDataManager targetDataManager = new PlayerTargetDataManager();
    private final Set<UUID> awaitingTarget = new HashSet<>();
    private final Map<UUID, RobberySession> sessionsByRobber = new HashMap<>();
    private final Map<UUID, Location> victimRestraintLocations = new HashMap<>();

    public void startAwaitingTarget(Player robber) {
        endSessionForRobber(robber.getUniqueId(), false);
        awaitingTarget.add(robber.getUniqueId());
        robber.sendMessage(ThieveryTexts.msg(ThieveryTexts.WARN + "Right-click a player within "
                + RobberyLoader.getMaxDistance() + " blocks to begin a robbery."));
    }

    public boolean acceptRobbery(Player victim) {
        RobberySession session = findPendingSessionForVictim(victim.getUniqueId());
        if (session == null) {
            victim.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You have no pending robbery request."));
            return false;
        }
        if (System.currentTimeMillis() > session.getAcceptDeadlineMs()) {
            victim.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "That robbery request has expired."));
            endSession(session, false);
            return false;
        }

        Player robber = Bukkit.getPlayer(session.getRobberId());
        if (robber == null || !robber.isOnline()) {
            victim.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "The robber is no longer available."));
            endSession(session, false);
            return false;
        }
        if (!RobberyUtil.isWithinRange(robber, victim, RobberyLoader.getMaxDistance())) {
            victim.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You are too far from the robber to accept."));
            return false;
        }

        PlayerTargetData targetData = targetDataManager.load(victim.getUniqueId());
        GuildAccessCooldown.recordAccess(targetData.getRobberyAccessMap(), robber.getUniqueId(),
                GuildAccessCooldown.today());
        targetDataManager.save(targetData);

        openActiveRobbery(robber, victim, session);
        return true;
    }

    public boolean isAwaitingTarget(UUID robberId) {
        return awaitingTarget.contains(robberId);
    }

    public boolean isVictimRestrained(UUID victimId) {
        return victimRestraintLocations.containsKey(victimId);
    }

    public boolean isRobberInActiveSession(UUID robberId) {
        RobberySession session = sessionsByRobber.get(robberId);
        return session != null && session.getState() == State.ACTIVE;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player robber = event.getPlayer();
        if (!awaitingTarget.contains(robber.getUniqueId())) {
            return;
        }
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Player victim)) {
            return;
        }
        if (!PlayerInteractCooldown.tryAcquire(robber.getUniqueId())) {
            return;
        }
        if (victim.getUniqueId().equals(robber.getUniqueId())) {
            robber.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You cannot rob yourself."));
            return;
        }
        if (!RobberyUtil.isWithinRange(robber, victim, RobberyLoader.getMaxDistance())) {
            robber.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "That player is too far away."));
            return;
        }

        PlayerTargetData targetData = targetDataManager.load(victim.getUniqueId());
        if (GuildAccessCooldown.isOnCooldown(targetData.getRobberyAccessMap(), robber,
                RobberyLoader.getCooldownDays())) {
            long remaining = GuildAccessCooldown.getMillisRemaining(targetData.getRobberyAccessMap(), robber,
                    RobberyLoader.getCooldownDays());
            robber.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Your guild must wait "
                    + GuildAccessCooldown.formatRemaining(remaining)
                    + " before targeting this player again."));
            return;
        }

        awaitingTarget.remove(robber.getUniqueId());
        endSessionForRobber(robber.getUniqueId(), false);

        StealGuiLayout layout = StealGuiLayout.create(PlayerSlotMap.TOTAL_LOGICAL_SLOTS);
        StealBudget budget = new StealBudget(RobberyLoader.getBudget());
        RobberySession session = new RobberySession(robber.getUniqueId(), victim.getUniqueId(), budget, layout);
        session.setAcceptDeadlineMs(System.currentTimeMillis()
                + RobberyLoader.getAcceptTimeoutSeconds() * 1000L);
        sessionsByRobber.put(robber.getUniqueId(), session);

        sendAcceptMessage(victim, robber);
        robber.sendMessage(ThieveryTexts.msg(ThieveryTexts.WARN + "Waiting for " + victim.getName()
                + " to accept your robbery request..."));

        Bukkit.getScheduler().runTaskLater(Thievery.getInstance(), () -> {
            RobberySession current = sessionsByRobber.get(robber.getUniqueId());
            if (current == session && current.getState() == State.PENDING_ACCEPT
                    && System.currentTimeMillis() > current.getAcceptDeadlineMs()) {
                endSession(session, false);
                if (robber.isOnline()) {
                    robber.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Your robbery request timed out."));
                }
                if (victim.isOnline()) {
                    victim.sendMessage(ThieveryTexts.msg(ThieveryTexts.MUTED + "The robbery request has expired."));
                }
            }
        }, RobberyLoader.getAcceptTimeoutSeconds() * 20L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        StealGuiHolder holder = getStealGuiHolder(event.getView().getTopInventory());
        if (holder != null && holder.getKind() == StealGuiHolder.Kind.ROBBERY) {
            handleRobberyGuiClick(event, player);
            return;
        }
        UUID playerId = player.getUniqueId();
        if (isVictimRestrained(playerId) || isRobberInActiveSession(playerId)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        StealGuiHolder holder = getStealGuiHolder(event.getView().getTopInventory());
        if (holder == null || holder.getKind() != StealGuiHolder.Kind.ROBBERY) {
            return;
        }
        RobberySession session = sessionsByRobber.get(player.getUniqueId());
        if (session != null && session.getState() == State.ACTIVE) {
            endSession(session, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (isVictimRestrained(id) || isRobberInActiveSession(id)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (isVictimRestrained(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (isVictimRestrained(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isVictimRestrained(event.getPlayer().getUniqueId())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!isVictimRestrained(victim.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        awaitingTarget.remove(playerId);

        PlayerInteractCooldown.clear(playerId);

        RobberySession asRobber = sessionsByRobber.get(playerId);
        if (asRobber != null) {
            endSession(asRobber, false);
        }

        RobberySession asVictim = findActiveSessionForVictim(playerId);
        if (asVictim != null) {
            PlayerSlotMap.dropAllExceptIgnored(player);
            endSession(asVictim, false);
        } else if (isVictimRestrained(playerId)) {
            PlayerSlotMap.dropAllExceptIgnored(player);
            victimRestraintLocations.remove(playerId);
        }
    }

    private void handleRobberyGuiClick(InventoryClickEvent event, Player robber) {
        event.setCancelled(true);
        RobberySession session = sessionsByRobber.get(robber.getUniqueId());
        if (session == null || session.getState() != State.ACTIVE) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        int guiSlot = event.getSlot();
        if (clickedItem == null || StealGuiPanes.isNonInteractivePane(clickedItem)
                || StealItemDisplay.isStealPane(clickedItem)) {
            return;
        }

        ClickType click = event.getClick();
        if (click != ClickType.LEFT && click != ClickType.SHIFT_LEFT) {
            return;
        }

        Integer logicalSlot = session.getLayout().getLogicalForGui(guiSlot);
        if (logicalSlot == null) {
            return;
        }

        Player victim = Bukkit.getPlayer(session.getVictimId());
        if (victim == null || !victim.isOnline()) {
            robber.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "The victim is no longer available."));
            endSession(session, true);
            return;
        }

        PlayerStealSource source = new PlayerStealSource(victim);
        Inventory guiInv = event.getView().getTopInventory();
        StealGuiHolder holder = getStealGuiHolder(guiInv);
        StealTakeHandler.performTake(robber, source, session.getBudget(), logicalSlot, guiInv, guiSlot, click,
                clickedItem, () -> refreshRobberyGui(robber, victim, session, guiInv, holder));
    }

    private void openActiveRobbery(Player robber, Player victim, RobberySession session) {
        session.setState(State.ACTIVE);
        session.setActiveEndMs(System.currentTimeMillis() + RobberyLoader.getDurationSeconds() * 1000L);
        victimRestraintLocations.put(victim.getUniqueId(), victim.getLocation().clone());

        Inventory gui = buildRobberyInventory(robber, victim, session);
        robber.openInventory(gui);

        robber.sendMessage(ThieveryTexts.msg(ThieveryTexts.CRITICAL + "Robbery started! Take what you can before time runs out."));
        victim.sendMessage(ThieveryTexts.msg(ThieveryTexts.CRITICAL + "You accepted the robbery and cannot move until it ends."));
    }

    public void tickActiveGuis() {
        for (RobberySession session : new ArrayList<>(sessionsByRobber.values())) {
            if (session.getState() == State.ACTIVE) {
                tickActiveGui(session);
            }
        }
    }

    public void tickActiveGui(RobberySession session) {
        Player robber = Bukkit.getPlayer(session.getRobberId());
        if (robber == null || !robber.isOnline()) {
            return;
        }
        StealGuiHolder holder = getStealGuiHolder(robber.getOpenInventory().getTopInventory());
        if (holder == null || holder.getKind() != StealGuiHolder.Kind.ROBBERY) {
            return;
        }

        long remaining = session.getActiveEndMs() - System.currentTimeMillis();
        if (remaining <= 0) {
            robber.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "The robbery window has ended."));
            robber.closeInventory();
            endSession(session, true);
            return;
        }

        Player victim = Bukkit.getPlayer(session.getVictimId());
        if (victim == null || !victim.isOnline()) {
            robber.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "The victim is no longer available."));
            endSession(session, true);
            return;
        }

        Inventory inv = robber.getOpenInventory().getTopInventory();
        refreshRobberyGui(robber, victim, session, inv, holder);
    }

    private Inventory buildRobberyInventory(Player robber, Player victim, RobberySession session) {
        StealGuiHolder holder = new StealGuiHolder(robber.getUniqueId(), StealGuiHolder.Kind.ROBBERY);
        long remaining = session.getActiveEndMs() - System.currentTimeMillis();
        String title = StealGuiTitle.forRobbery(remaining, session.getBudget());
        PlayerData thiefData = Thievery.getPlayerManager().get(robber.getUniqueId());
        return StealGuiBuilder.buildRobberyGui(holder, session.getLayout(), title, victim,
                session.getBudget(), thiefData);
    }

    private void refreshRobberyGui(Player robber, Player victim, RobberySession session, Inventory guiInv,
            StealGuiHolder holder) {
        PlayerData thiefData = Thievery.getPlayerManager().get(robber.getUniqueId());
        for (Map.Entry<Integer, Integer> entry : session.getLayout().getLogicalSlotToGuiSlot().entrySet()) {
            int logicalSlot = entry.getKey();
            int guiSlot = entry.getValue();
            ItemStack realItem = PlayerSlotMap.getItem(victim, logicalSlot);
            if (realItem == null || realItem.getType().isAir() || StealIgnoreRules.isIgnored(realItem)) {
                continue;
            }
            ItemStack display = StealItemDisplay.buildRepresentation(realItem, session.getBudget().getRemaining(),
                    thiefData);
            if (display != null) {
                guiInv.setItem(guiSlot, display);
            }
        }
        if (holder != null) {
            long remaining = session.getActiveEndMs() - System.currentTimeMillis();
            String title = StealGuiTitle.forRobbery(remaining, session.getBudget());
            StealGuiRefresher.updateTitle(robber, holder, title);
        }
    }

    private static StealGuiHolder getStealGuiHolder(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryHolder inventoryHolder = inventory.getHolder();
        return inventoryHolder instanceof StealGuiHolder stealGuiHolder ? stealGuiHolder : null;
    }

    private void sendAcceptMessage(Player victim, Player robber) {
        net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(
                ThieveryTexts.msg(ThieveryTexts.CRITICAL + robber.getName() + ThieveryTexts.WARN
                        + " wants to rob you. Click "));
        net.md_5.bungee.api.chat.TextComponent accept = new net.md_5.bungee.api.chat.TextComponent(
                ThieveryTexts.msg(ThieveryTexts.SUCCESS + "[ACCEPT]"));
        accept.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/robbery accept"));
        accept.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("Accept the robbery request").create()));
        message.addExtra(accept);
        message.addExtra(ThieveryTexts.msg(ThieveryTexts.WARN + " within "
                + RobberyLoader.getAcceptTimeoutSeconds() + " seconds."));
        victim.spigot().sendMessage(message);
    }

    private RobberySession findPendingSessionForVictim(UUID victimId) {
        for (RobberySession session : sessionsByRobber.values()) {
            if (session.getVictimId().equals(victimId) && session.getState() == State.PENDING_ACCEPT) {
                return session;
            }
        }
        return null;
    }

    private RobberySession findActiveSessionForVictim(UUID victimId) {
        for (RobberySession session : sessionsByRobber.values()) {
            if (session.getVictimId().equals(victimId) && session.getState() == State.ACTIVE) {
                return session;
            }
        }
        return null;
    }

    private void endSessionForRobber(UUID robberId, boolean notify) {
        RobberySession session = sessionsByRobber.remove(robberId);
        if (session != null) {
            releaseVictim(session.getVictimId());
            if (notify) {
                Player victim = Bukkit.getPlayer(session.getVictimId());
                if (victim != null && victim.isOnline()) {
                    victim.sendMessage(ThieveryTexts.msg(ThieveryTexts.MUTED + "The robbery has ended."));
                }
            }
        }
    }

    private void endSession(RobberySession session, boolean notifyVictim) {
        sessionsByRobber.remove(session.getRobberId());
        releaseVictim(session.getVictimId());
        if (notifyVictim) {
            Player victim = Bukkit.getPlayer(session.getVictimId());
            if (victim != null && victim.isOnline()) {
                victim.sendMessage(ThieveryTexts.msg(ThieveryTexts.MUTED + "The robbery has ended."));
            }
        }
    }

    private void releaseVictim(UUID victimId) {
        victimRestraintLocations.remove(victimId);
    }
}
