package net.tfminecraft.thievery.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Utils.ClueGiver;

public final class ClueChecker {

    private ClueChecker() {}

    public static boolean hasEnoughClues(Player player) {
        return ClueGiver.hasEnoughClues(player);
    }

    public static boolean isClueItem(ItemStack item) {
        return ClueGiver.isClueItem(item);
    }

    public static void sendInsufficientCluesMessage(Player player) {
        net.tfminecraft.RPCharacters.Objects.PlayerData pd = PlayerManager.get(player);
        if (pd == null || !pd.hasActiveCharacter()) {
            player.sendTitle(" ", ThieveryTexts.msg(ThieveryTexts.ERROR + "No Character!"), 5, 50, 5);
            player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "You do not have an active character!"));
            player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Create one with " + ThieveryTexts.WARN
                    + "/rpcharacter create"));
            return;
        }

        RPCharacter character = pd.getActiveCharacter();
        int have = character.getPlayerClues().size();
        int need = character.getCluesNeeded();
        player.sendTitle(" ", ThieveryTexts.msg(ThieveryTexts.ERROR + "More Clues Needed!"), 5, 50, 5);
        player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Your character does not have enough clues ("
                + ThieveryTexts.WARN + have + ThieveryTexts.ERROR + "/" + ThieveryTexts.WARN + need
                + ThieveryTexts.ERROR + ")."));
        player.sendMessage(ThieveryTexts.msg(ThieveryTexts.ERROR + "Add clues with " + ThieveryTexts.WARN
                + "/rpcharacter clues"));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }
}
