package net.tfminecraft.thievery.util;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public final class MaskFormatter {

    private MaskFormatter() {}

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
