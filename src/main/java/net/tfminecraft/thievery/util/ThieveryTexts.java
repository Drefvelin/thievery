package net.tfminecraft.thievery.util;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public final class ThieveryTexts {

    public static final String SUCCESS = "#87d65c";
    public static final String ERROR = "#d65c5c";
    public static final String CRITICAL = "#a84343";
    public static final String WARN = "#d6cf69";
    public static final String ACCENT = "#c9a24f";
    public static final String INFO = "#56ccf2";
    public static final String STAFF = "#c4b896";

    public static final String MUTED = "§7";
    public static final String WHITE = "§f";
    public static final String DARK = "§8";

    private ThieveryTexts() {}

    public static String format(String raw) {
        return StringFormatter.formatHex(raw);
    }

    public static String msg(String raw) {
        return format(raw);
    }
}
