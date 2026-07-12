package net.tfminecraft.thievery.mask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

public class MaskChannelOverride {

    private final String id;
    private final String format;
    private final int range;
    private final List<String> commands;

    public MaskChannelOverride(String id, ConfigurationSection config) {
        this.id = id;
        this.format = config.getString("format", "");
        this.range = config.getInt("range", 15);
        List<String> cmds = config.getStringList("commands");
        this.commands = cmds != null ? new ArrayList<>(cmds) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getFormat() {
        return format;
    }

    public int getRange() {
        return range;
    }

    public List<String> getCommands() {
        return Collections.unmodifiableList(commands);
    }
}
