package net.tfminecraft.thievery.door;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;

public final class LockAccess {

    private LockAccess() {}

    public static boolean canAccess(Player player, UUID owner, LockState lockState) {
        if (lockState == LockState.PUBLIC) {
            return true;
        }
        if (owner == null) {
            return true;
        }
        if (owner.equals(player.getUniqueId())) {
            return true;
        }
        if (lockState == LockState.PRIVATE) {
            return false;
        }

        OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(owner);
        if (ownerPlayer.getName() == null) {
            return false;
        }

        Guild ownerGuild = FactionManager.getGuildByMember(ownerPlayer.getName());
        Guild openerGuild = FactionManager.getGuildByMember(player.getName());
        if (ownerGuild == null || openerGuild == null) {
            return false;
        }
        return ownerGuild.getId().equals(openerGuild.getId());
    }
}
