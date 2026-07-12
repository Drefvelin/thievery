package net.tfminecraft.thievery.util;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Utils.ClueGiver;
import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.data.ChestLockpickSession;
import net.tfminecraft.thievery.data.PlayerData;
import net.tfminecraft.thievery.database.Database;

public final class ClueDropper {

    private ClueDropper() {}

    public static void tryDropChestClue(Player player, ChestLockpickSession session, Block chestBlock,
            Inventory chestInv, int takenChestSlot, int dexterity, double lockpickStrength, double valueTaken) {
        tryDropChestClue(player, session, chestBlock, chestInv, takenChestSlot, dexterity, lockpickStrength,
                valueTaken, false);
    }

    public static void tryDropChestClue(Player player, ChestLockpickSession session, Block chestBlock,
            Inventory chestInv, int takenChestSlot, int dexterity, double lockpickStrength,
            boolean fromBundleTake) {
        tryDropChestClue(player, session, chestBlock, chestInv, takenChestSlot, dexterity, lockpickStrength,
                0, fromBundleTake);
    }

    public static void tryDropChestClue(Player player, ChestLockpickSession session, Block chestBlock,
            Inventory chestInv, int takenChestSlot, int dexterity, double lockpickStrength, double valueTaken,
            boolean fromBundleTake) {
        PlayerData playerData = Thievery.getPlayerManager().get(player.getUniqueId());
        playerData.applyRiskDecay(dexterity);
        double sessionRisk = playerData.getRisk();
        double takeClueChance = RiskCalculator.computeTakeClueChance(sessionRisk, valueTaken);
        if (!shouldDropClue(session.getSuccessfulClueDrops(), Cache.minCluesContainer, takeClueChance)) {
            return;
        }

        String targetKey = session.getTargetKey();
        String clueText = pickClue(player, playerData, targetKey, dexterity, lockpickStrength, sessionRisk);
        if (clueText == null) return;

        ItemStack cluePaper = ClueGiver.createClueItem(clueText);
        if (cluePaper == null || cluePaper.getType().isAir()) return;

        int preferredSlot = fromBundleTake ? -1 : takenChestSlot;
        boolean chestPlaced = placeClueInChest(chestInv, preferredSlot, cluePaper);

        boolean spawnHologram = session.getSuccessfulClueDrops() == 0 || !chestPlaced;
        boolean hologramSpawned = false;
        if (spawnHologram) {
            hologramSpawned = trySpawnHologram(player, player.getLocation(), chestBlock.getLocation(), clueText);
        }

        if (!chestPlaced && !hologramSpawned) return;

        recordSuccess(player, playerData, clueText, targetKey, session);
    }

    public static void tryDropDoorClue(Player player, Location doorCanonical, UUID ownerUUID,
            int dexterity, double lockpickStrength) {
        PlayerData playerData = Thievery.getPlayerManager().get(player.getUniqueId());
        playerData.applyRiskDecay(dexterity);
        double sessionRisk = playerData.getRisk();
        if (!shouldDropClue(0, Cache.minCluesDoor, sessionRisk)) {
            return;
        }

        String targetKey = TargetKeyResolver.resolve(ownerUUID);
        String clueText = pickClue(player, playerData, targetKey, dexterity, lockpickStrength, sessionRisk);
        if (clueText == null) return;

        boolean hologramSpawned = trySpawnHologram(player, doorCanonical, doorCanonical, clueText);
        if (!hologramSpawned) return;

        playerData.recordClueUsed(clueText, targetKey);
        Database.savePlayerData(playerData);
    }

    private static boolean shouldDropClue(int cluesAlreadyDropped, int minClues, double clueChance) {
        if (cluesAlreadyDropped < minClues) {
            return true;
        }
        return Math.random() < clueChance;
    }

    private static String pickClue(Player player, PlayerData playerData, String targetKey,
            int dexterity, double lockpickStrength, double effectiveRisk) {
        net.tfminecraft.RPCharacters.Objects.PlayerData rpData = PlayerManager.get(player);
        if (rpData == null || !rpData.hasActiveCharacter()) return null;

        RPCharacter character = rpData.getActiveCharacter();
        double criticalChance = RiskCalculator.computeCritical(
                effectiveRisk, dexterity, lockpickStrength);
        if (Math.random() < criticalChance && !playerData.isCriticalOnCooldown(targetKey)) {
            playerData.recordCriticalClue(targetKey);
            return Cache.criticalClue.replace("{character_name}", character.getName());
        }

        return ClueGiver.getRandomClueExcluding(character, playerData.getRecentCluesForExclude(targetKey));
    }

    private static boolean placeClueInChest(Inventory inv, int preferredSlot, ItemStack cluePaper) {
        if (preferredSlot >= 0 && canPlaceClueInSlot(inv, preferredSlot)) {
            inv.setItem(preferredSlot, cluePaper.clone());
            return true;
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (i == preferredSlot) {
                continue;
            }
            if (!canPlaceClueInSlot(inv, i)) {
                continue;
            }
            inv.setItem(i, cluePaper.clone());
            return true;
        }
        return false;
    }

    private static boolean canPlaceClueInSlot(Inventory inv, int slot) {
        if (!isEmptySlot(inv, slot)) {
            return false;
        }
        ItemStack existing = inv.getItem(slot);
        return !BundleHandler.isBundle(existing);
    }

    private static boolean isEmptySlot(Inventory inv, int slot) {
        if (slot < 0 || slot >= inv.getSize()) return false;
        ItemStack item = inv.getItem(slot);
        return item == null || item.getType().isAir();
    }

    private static boolean trySpawnHologram(Player player, Location anchor, Location targetBlock, String clueText) {
        return ClueGiver.spawnClueWithText(anchor, targetBlock, player, clueText, true) != null;
    }

    private static void recordSuccess(Player player, PlayerData playerData, String clueText,
            String targetKey, ChestLockpickSession session) {
        playerData.recordClueUsed(clueText, targetKey);
        session.incrementSuccessfulClueDrops();
        Database.savePlayerData(playerData);
    }
}
