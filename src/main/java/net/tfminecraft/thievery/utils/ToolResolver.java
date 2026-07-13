package net.tfminecraft.thievery.utils;

import org.bukkit.inventory.ItemStack;

import net.tfminecraft.thievery.key.KeyCopyHandler;
import net.tfminecraft.thievery.key.KeyDefinition;
import net.tfminecraft.thievery.key.KeychainHandler;
import net.tfminecraft.thievery.player.LockpickDefinition;
import net.tfminecraft.thievery.loader.DoorLoader;
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

    public static boolean isMasterKey(ItemStack item) {
        return KeyCopyHandler.isMasterKey(item);
    }

    public static boolean isDoorKey(ItemStack item) {
        return KeyCopyHandler.isStorableDoorKey(item);
    }

    public static boolean isLockingKey(ItemStack item) {
        return isMasterKey(item) || KeyCopyHandler.isPermanentCopy(item);
    }

    public static boolean isKey(ItemStack item) {
        return isDoorKey(item);
    }

    public static boolean isLockpick(ItemStack item) {
        return resolveLockpick(item) != null;
    }

    public static boolean isKeychain(ItemStack item) {
        return KeychainHandler.isKeychain(item);
    }

    public static boolean isKeychainItem(ItemStack item) {
        return KeychainHandler.isKeychainItem(item);
    }

    public static double getKeyStrength(ItemStack item) {
        if (KeyCopyHandler.isCopyItem(item) || KeyCopyHandler.isMold(item)) {
            return KeyCopyHandler.getSourceStrength(item);
        }
        KeyDefinition key = resolveKey(item);
        return key != null ? key.getStrength() : 0.0;
    }

    public static double getLockpickStrength(ItemStack item) {
        LockpickDefinition lockpick = resolveLockpick(item);
        return lockpick != null ? lockpick.getStrength() : 0.0;
    }

    public static boolean isDebugTool(ItemStack item) {
        return DoorLoader.matchesDebugTool(item);
    }
}
