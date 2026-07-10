package net.tfminecraft.thievery.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.thievery.cache.Cache;

public final class TraitChecker {

    private TraitChecker() {}

    public static boolean hasRequiredTraits(Player player) {
        if (Cache.traits.isEmpty()) return true;

        net.tfminecraft.RPCharacters.Objects.PlayerData pd = PlayerManager.get(player);
        if (!pd.hasActiveCharacter()) return false;

        RPCharacter character = pd.getActiveCharacter();
        for (Trait trait : character.getTraits()) {
            if (Cache.traits.contains(trait.getId())) return true;
        }
        return false;
    }

    public static void sendMissingTraitMessage(Player player) {
        player.sendMessage(ChatColor.RED + "You lack the needed character trait(s) for thievery!");
    }
}
