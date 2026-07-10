package net.tfminecraft.thievery.manager;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.tfminecraft.thievery.data.MaskChannelOverride;
import net.tfminecraft.thievery.loader.MaskLoader;
import net.tfminecraft.thievery.util.MaskFormatter;

public final class MaskManager {

    private static final Set<String> EXCLUDED_COMMANDS = Set.of(
            "ooc", "admin", "a", "helper", "h", "dm", "narrate");

    private MaskManager() {}

    public static boolean isExcludedCommand(String commandLabel) {
        if (commandLabel == null) return false;
        return EXCLUDED_COMMANDS.contains(commandLabel.toLowerCase(Locale.ROOT));
    }

    public static void sendMaskedChatBlockedMessage(Player player) {
        List<String> commands = MaskLoader.getMaskedChannelCommands();
        String defaultCommand = MaskLoader.DEFAULT_MASKED_COMMAND;
        String others = commands.stream()
                .filter(command -> !command.equalsIgnoreCase(defaultCommand))
                .map(command -> ChatColor.WHITE + "/" + command)
                .collect(Collectors.joining(ChatColor.RED + ", "));

        StringBuilder message = new StringBuilder();
        message.append(ChatColor.RED).append("While wearing a mask, use ");
        message.append(ChatColor.WHITE).append("/").append(defaultCommand).append(" <message>");
        if (!others.isEmpty()) {
            message.append(ChatColor.RED).append(" or another channel (").append(others).append(ChatColor.RED).append(").");
        } else {
            message.append(ChatColor.RED).append(".");
        }
        player.sendMessage(message.toString());
    }

    public static void broadcastMasked(Player sender, String channelKey, String message) {
        MaskChannelOverride channel = MaskLoader.getChannel(channelKey);
        if (channel == null || channel.getFormat().isEmpty()) {
            return;
        }

        String formatted = MaskFormatter.format(channel.getFormat(), message);
        int range = channel.getRange();
        if (range <= 0) {
            sender.getServer().broadcastMessage(formatted);
            return;
        }

        Location origin = sender.getLocation();
        double rangeSq = (double) range * range;
        for (Player target : sender.getWorld().getPlayers()) {
            if (target.getLocation().distanceSquared(origin) <= rangeSq) {
                target.sendMessage(formatted);
            }
        }
    }
}
