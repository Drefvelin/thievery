package net.tfminecraft.thievery.manager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.util.MaskResolver;

public class MaskProfileBlockListener implements Listener {

    /**
     * Blocks shift-right-click profile views (e.g. OpenRP descriptions) on masked players.
     * Dangerous fix: cancelling {@link PlayerInteractEntityEvent} early can also suppress
     * other plugins' entity-interact handlers that respect cancellation.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onMaskedPlayerProfileView(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Player clicker = event.getPlayer();
        if (!clicker.isSneaking()) {
            return;
        }

        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }

        if (!MaskResolver.isWearingMask(target)) {
            return;
        }

        ItemStack handItem = clicker.getInventory().getItem(event.getHand());
        if (handItem != null && !handItem.getType().isAir()) {
            return;
        }

        event.setCancelled(true);
    }
}
