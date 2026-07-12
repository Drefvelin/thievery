package net.tfminecraft.thievery.steal;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.player.PlayerData;
import net.tfminecraft.thievery.robbery.RobberySession;
import net.tfminecraft.thievery.robbery.RobberySession.State;
import net.tfminecraft.thievery.steal.StealGuiHolder;
import net.tfminecraft.thievery.loader.RobberyLoader;
import net.tfminecraft.thievery.steal.session.StealSession;
import net.tfminecraft.thievery.steal.PlayerSlotMap;
import net.tfminecraft.thievery.steal.source.PlayerStealSource;
import net.tfminecraft.thievery.steal.StealGui;
import net.tfminecraft.thievery.steal.StealGui;
import net.tfminecraft.thievery.steal.StealGui;
import net.tfminecraft.thievery.steal.StealIgnoreRules;
import net.tfminecraft.thievery.steal.StealItemDisplay;
import net.tfminecraft.thievery.steal.source.StealSource;
import net.tfminecraft.thievery.steal.StealTakeHandler;
import net.tfminecraft.thievery.utils.ThieveryTexts;

public class RobberyStealReference extends StealReference {

    private final RobberySession session;
    private final Runnable onEnd;

    public RobberyStealReference(RobberySession session, Runnable onEnd) {
        super(session.getRobberId(), StealGuiHolder.Kind.ROBBERY);
        this.session = session;
        this.onEnd = onEnd;
    }

    @Override
    public StealSession getSession() {
        return session;
    }

    @Override
    public String buildTitle(Player thief) {
        long remaining = session.getActiveEndMs() - System.currentTimeMillis();
        return StealGui.forRobbery(remaining, session.getBudget());
    }

    @Override
    public void tick(Player thief) {
        long remaining = session.getActiveEndMs() - System.currentTimeMillis();
        if (remaining <= 0) {
            thief.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "The robbery window has ended."));
            thief.closeInventory();
            onEnd.run();
            return;
        }
        Player victim = Bukkit.getPlayer(session.getVictimId());
        if (victim == null || !victim.isOnline()) {
            thief.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "The victim is no longer available."));
            onEnd.run();
            return;
        }
        Inventory inv = thief.getOpenInventory().getTopInventory();
        refreshGui(thief, victim, inv);
    }

    @Override
    protected void handleStealClick(InventoryClickEvent event, Player robber) {
        if (session.getState() != State.ACTIVE) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        int guiSlot = event.getSlot();
        if (clickedItem == null || StealGui.isNonInteractivePane(clickedItem)
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
            onEnd.run();
            return;
        }

        Inventory guiInv = event.getView().getTopInventory();
        PlayerStealSource source = new PlayerStealSource(victim);
        StealTakeHandler.performTake(robber, source, session.getBudget(), logicalSlot, guiInv, guiSlot, click,
                clickedItem, () -> refreshGui(robber, victim, guiInv));
    }

    @Override
    public void onClose(Player thief) {
        if (session.getState() == State.ACTIVE) {
            onEnd.run();
        }
    }

    public Inventory buildInventory(Player robber, Player victim) {
        long remaining = session.getActiveEndMs() - System.currentTimeMillis();
        String title = StealGui.forRobbery(remaining, session.getBudget());
        PlayerData thiefData = Thievery.getPlayerManager().get(robber.getUniqueId());
        return StealGui.buildRobberyGui(getHolder(), session.getLayout(), title, victim,
                session.getBudget(), thiefData);
    }

    public void refreshGui(Player robber, Player victim, Inventory guiInv) {
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
        updateTitle(robber);
    }
}
