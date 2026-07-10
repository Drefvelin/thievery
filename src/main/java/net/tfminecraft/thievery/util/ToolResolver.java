package net.tfminecraft.thievery.util;

import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.data.KeyDefinition;
import net.tfminecraft.thievery.data.LockpickDefinition;
import net.tfminecraft.thievery.loader.KeyLoader;
import net.tfminecraft.thievery.loader.LockpickLoader;

public final class ToolResolver {

    private ToolResolver() {}

    public static KeyDefinition resolveKey(ItemStack item) {
        return KeyLoader.resolve(item);
    }

    public static LockpickDefinition resolveLockpick(ItemStack item) {
        return LockpickLoader.resolve(item);
    }

    public static boolean isKey(ItemStack item) {
        return resolveKey(item) != null;
    }

    public static boolean isLockpick(ItemStack item) {
        return resolveLockpick(item) != null;
    }

    public static double getKeyStrength(ItemStack item) {
        KeyDefinition key = resolveKey(item);
        return key != null ? key.getStrength() : 0.0;
    }

    public static double getLockpickStrength(ItemStack item) {
        LockpickDefinition lockpick = resolveLockpick(item);
        return lockpick != null ? lockpick.getStrength() : 0.0;
    }
}
