package net.tfminecraft.thievery.util;

import org.bukkit.entity.Player;

import net.tfminecraft.thievery.holder.StealGuiHolder;

public final class StealGuiRefresher {

    private StealGuiRefresher() {}

    public static void updateTitle(Player player, StealGuiHolder holder, String title) {
        if (player == null || !player.isOnline() || holder == null || title == null) {
            return;
        }
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof StealGuiHolder openHolder)) {
            return;
        }
        if (!openHolder.getPlayerId().equals(holder.getPlayerId()) || openHolder.getKind() != holder.getKind()) {
            return;
        }
        if (title.equals(player.getOpenInventory().getTitle())) {
            return;
        }
        player.getOpenInventory().setTitle(title);
    }
}
