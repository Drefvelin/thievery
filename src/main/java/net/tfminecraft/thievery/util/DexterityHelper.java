package net.tfminecraft.thievery.util;

import org.bukkit.entity.Player;

import net.Indyuce.mmocore.api.player.PlayerData;
import net.tfminecraft.thievery.cache.Parameters;

public final class DexterityHelper {

    private DexterityHelper() {}

    public static int getDexterity(Player player) {
        try {
            return PlayerData.get(player.getUniqueId())
                    .getAttributes()
                    .getInstance(Parameters.lockpickAttribute)
                    .getTotal();
        } catch (Exception e) {
            return 0;
        }
    }
}
