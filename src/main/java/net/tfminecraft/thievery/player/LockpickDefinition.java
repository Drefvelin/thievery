package net.tfminecraft.thievery.player;

import org.bukkit.configuration.ConfigurationSection;

public class LockpickDefinition {

    private final String id;
    private final String item;
    private final double strength;
    private final int capacity;

    public LockpickDefinition(String id, ConfigurationSection config) {
        this.id = id;
        this.item = config.getString("item", "v.paper");
        this.strength = config.getDouble("strength", 0.0);
        this.capacity = config.getInt("capacity", 30);
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

    public int getCapacity() {
        return capacity;
    }
}
