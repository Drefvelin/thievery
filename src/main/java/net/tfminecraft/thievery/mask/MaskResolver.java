package net.tfminecraft.thievery.mask;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.thievery.loader.MaskLoader;

public final class MaskResolver {

    private MaskResolver() {}

    public static boolean isMaskItem(ItemStack item) {
        return MaskLoader.resolveMask(item) != null;
    }

    public static boolean isWearingMask(Player player) {
        return isMaskItem(player.getInventory().getHelmet());
    }

    public static String format(String template, String rawMessage) {
        String withMessage = template.replace("{message}", sanitize(rawMessage));
        String withCodes = withMessage.replace('&', '\u00A7');
        return StringFormatter.formatHex(withCodes);
    }

    public static String sanitize(String message) {
        if (message == null) return "";
        return message.replaceAll("(?i)[§&][0-9a-fk-or]", "");
    }
}
