package net.tfminecraft.util;

import org.bukkit.NamespacedKey;

import net.tfminecraft.thievery.Thievery;

public class Keys {
    public static final NamespacedKey keyUUIDKey = new NamespacedKey(Thievery.getInstance(), "door_key_uuid");
    public static final NamespacedKey keyStrength = new NamespacedKey(Thievery.getInstance(), "door_key_strength");
    public static final NamespacedKey lockpickStrength = new NamespacedKey(Thievery.getInstance(), "lockpick_strength");
}
