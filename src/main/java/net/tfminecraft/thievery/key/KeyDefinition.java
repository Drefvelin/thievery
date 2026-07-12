package net.tfminecraft.thievery.key;

import org.bukkit.configuration.ConfigurationSection;

public class KeyDefinition {

    private final String id;
    private final String item;
    private final double strength;

    public KeyDefinition(String id, ConfigurationSection config) {
        this.id = id;
        this.item = config.getString("item", "v.paper");
        this.strength = config.getDouble("strength", 1.0);
    }

    public String getId() {
        return id;
    }

    public String getItem() {
        return item;
    }

    public double getStrength() {
        return strength;
    }
}
