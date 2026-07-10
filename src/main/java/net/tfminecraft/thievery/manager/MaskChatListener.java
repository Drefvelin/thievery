package net.tfminecraft.thievery.manager;

import java.util.Locale;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.loader.MaskLoader;
import net.tfminecraft.thievery.util.MaskResolver;

public class MaskChatListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!MaskResolver.isWearingMask(player)) {
            return;
        }

        String raw = event.getMessage().stripLeading();
        if (!raw.startsWith("/")) {
            return;
        }

        String withoutSlash = raw.substring(1);
        int space = withoutSlash.indexOf(' ');
        String label = (space < 0 ? withoutSlash : withoutSlash.substring(0, space)).toLowerCase(Locale.ROOT);
        String message = space < 0 ? "" : withoutSlash.substring(space + 1).stripLeading();

        if (MaskManager.isExcludedCommand(label)) {
            return;
        }

        String channelKey = MaskLoader.resolveChannelFromCommand(label);
        if (channelKey == null || message.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        MaskManager.broadcastMasked(player, channelKey, message);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!MaskResolver.isWearingMask(player)) {
            return;
        }

        String message = event.getMessage();
        if (message == null || message.isBlank()) {
            return;
        }

        event.setCancelled(true);
        Thievery.getInstance().getServer().getScheduler().runTask(Thievery.getInstance(), () -> {
            MaskManager.sendMaskedChatBlockedMessage(player);
        });
    }
}
