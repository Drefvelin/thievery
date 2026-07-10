package net.tfminecraft.thievery.data;

import java.util.HashSet;
import java.util.Set;

import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.loader.CategoryLoader;

public class LoadoutSession {

    public enum ToggleResult {
        TOGGLED_ON,
        TOGGLED_OFF,
        NO_CHANGE,
        ALLOCATION_FULL,
        NOT_ENOUGH_BANK,
        UNKNOWN_CATEGORY
    }

    private final Set<String> savedActive;
    private final Set<String> draftActive;
    private final Set<String> sessionUnlocked;
    private int draftBank;

    public LoadoutSession(Set<String> savedActive, Set<String> draftActive, Set<String> sessionUnlocked, int draftBank) {
        this.savedActive = new HashSet<>(savedActive);
        this.draftActive = new HashSet<>(draftActive);
        this.sessionUnlocked = new HashSet<>(sessionUnlocked);
        this.draftBank = draftBank;
    }

    public static LoadoutSession from(PlayerData playerData) {
        Set<String> active = new HashSet<>(playerData.getActiveCategories());
        return new LoadoutSession(active, active, active, playerData.getPoints());
    }

    public int getDraftBank() {
        return draftBank;
    }

    public Set<String> getDraftActive() {
        return draftActive;
    }

    public boolean isSessionUnlocked(String categoryId) {
        return sessionUnlocked.contains(categoryId);
    }

    public int getDraftAllocated() {
        return PlayerData.getAllocatedCost(draftActive);
    }

    public boolean canConfirm() {
        return getDraftAllocated() <= Cache.categoryPoints;
    }

    public ToggleResult toggleCategory(String categoryId) {
        ItemCategory category = CategoryLoader.getById(categoryId);
        if (category == null) return ToggleResult.UNKNOWN_CATEGORY;

        if (draftActive.contains(categoryId)) {
            draftActive.remove(categoryId);
            if (!savedActive.contains(categoryId)) {
                draftBank = Math.min(draftBank + category.getCost(), Cache.categoryPoints);
            }
            return ToggleResult.TOGGLED_OFF;
        }

        if (getDraftAllocated() + category.getCost() > Cache.categoryPoints) {
            return ToggleResult.ALLOCATION_FULL;
        }

        if (!sessionUnlocked.contains(categoryId)) {
            if (draftBank < category.getCost()) {
                return ToggleResult.NOT_ENOUGH_BANK;
            }
            draftBank -= category.getCost();
            sessionUnlocked.add(categoryId);
        }

        draftActive.add(categoryId);
        return ToggleResult.TOGGLED_ON;
    }
}
