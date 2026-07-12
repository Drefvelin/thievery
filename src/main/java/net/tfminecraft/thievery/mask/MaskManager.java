package net.tfminecraft.thievery.mask;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.tfminecraft.thievery.mask.MaskChannelOverride;
import net.tfminecraft.thievery.loader.MaskLoader;
import net.tfminecraft.thievery.mask.MaskResolver;
import net.tfminecraft.thievery.utils.ThieveryTexts;

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
                .map(command -> ThieveryTexts.WHITE + "/" + command)
                .collect(Collectors.joining(ThieveryTexts.ERROR + ", "));

        StringBuilder message = new StringBuilder();
        message.append(ThieveryTexts.ERROR).append("While wearing a mask, use ");
        message.append(ThieveryTexts.WHITE).append("/").append(defaultCommand).append(" <message>");
        if (!others.isEmpty()) {
            message.append(ThieveryTexts.ERROR).append(" or another channel (").append(others)
                    .append(ThieveryTexts.ERROR).append(").");
        } else {
            message.append(ThieveryTexts.ERROR).append(".");
        }
        player.sendMessage(ThieveryTexts.msg(message.toString()));
    }

    public static void broadcastMasked(Player sender, String channelKey, String message) {
        MaskChannelOverride channel = MaskLoader.getChannel(channelKey);
        if (channel == null || channel.getFormat().isEmpty()) {
            return;
        }

        String formatted = MaskResolver.format(channel.getFormat(), message);
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
