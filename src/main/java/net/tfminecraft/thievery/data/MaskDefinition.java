package net.tfminecraft.thievery.data;

import org.bukkit.configuration.ConfigurationSection;

public class MaskDefinition {

    private final String id;
    private final String item;

    public MaskDefinition(String id, String item) {
        this.id = id;
        this.item = item;
    }

    public MaskDefinition(String id, ConfigurationSection config) {
        this.id = id;
        this.item = config.getString("item", "");
    }

    public static MaskDefinition fromItemPath(String id, String itemPath) {
        return new MaskDefinition(id, itemPath);
    }

    public String getId() {
        return id;
    }

    public String getItem() {
        return item;
    }
}
