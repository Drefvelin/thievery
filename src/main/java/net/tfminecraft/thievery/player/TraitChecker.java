package net.tfminecraft.thievery.player;

import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import java.util.List;

import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.utils.ThieveryTexts;

public final class TraitChecker {

    private TraitChecker() {}

    public static boolean hasTraits(Player player, List<String> configuredTraits) {
        if (configuredTraits == null || configuredTraits.isEmpty()) {
            return true;
        }
        net.tfminecraft.RPCharacters.Objects.PlayerData pd = PlayerManager.get(player);
        if (!pd.hasActiveCharacter()) {
            return false;
        }
        RPCharacter character = pd.getActiveCharacter();
        for (Trait trait : character.getTraits()) {
            if (configuredTraits.contains(trait.getId())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasRequiredTraits(Player player) {
        return hasTraits(player, Cache.traits);
    }

    public static void sendMissingTraitMessage(Player player, String context) {
        player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You lack the needed character trait(s) for " + context + "!"));
    }

    public static void sendMissingTraitMessage(Player player) {
        player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You lack the needed character trait(s) for thievery!"));
    }
}
