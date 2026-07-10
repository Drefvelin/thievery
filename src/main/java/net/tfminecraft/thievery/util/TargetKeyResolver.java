package net.tfminecraft.thievery.util;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;

public final class TargetKeyResolver {

    public static final String NONE = "none";

    private TargetKeyResolver() {}

    public static String resolve(UUID ownerUUID) {
        if (ownerUUID == null) {
            return NONE;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
        String ownerName = owner.getName();
        if (ownerName == null) {
            return NONE;
        }
        Guild guild = FactionManager.getGuildByMember(ownerName);
        if (guild != null) {
            return "guild:" + guild.getId();
        }
        return "player:" + ownerUUID;
    }
}
