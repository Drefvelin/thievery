package net.tfminecraft.thievery.steal;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import net.tfminecraft.thievery.steal.StealGuiHolder;
import net.tfminecraft.thievery.steal.session.StealSession;
import net.tfminecraft.thievery.steal.StealBudget;
import net.tfminecraft.thievery.steal.StealGui;
import net.tfminecraft.thievery.steal.StealGui;

public abstract class StealReference {

    private final UUID thiefId;
    private final StealGuiHolder holder;

    protected StealReference(UUID thiefId, StealGuiHolder.Kind kind) {
        this.thiefId = thiefId;
        this.holder = new StealGuiHolder(thiefId, kind);
    }

    public UUID getThiefId() {
        return thiefId;
    }

    public StealGuiHolder getHolder() {
        return holder;
    }

    public StealGuiHolder.Kind getKind() {
        return holder.getKind();
    }

    public abstract StealSession getSession();

    public StealBudget getBudget() {
        return getSession().getBudget();
    }

    public StealGui.Layout getLayout() {
        return getSession().getLayout();
    }

    public abstract String buildTitle(Player thief);

    public void tick(Player thief) {
        updateTitle(thief);
    }

    protected void updateTitle(Player thief) {
        StealGui.updateTitle(thief, holder, buildTitle(thief));
    }

    public void handleClick(InventoryClickEvent event, Player thief) {
        event.setCancelled(true);
        handleStealClick(event, thief);
    }

    protected abstract void handleStealClick(InventoryClickEvent event, Player thief);

    public abstract void onClose(Player thief);

    public void onOpen(Player thief, Inventory gui) {
    }
}
