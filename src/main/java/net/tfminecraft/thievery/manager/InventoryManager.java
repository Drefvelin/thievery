package net.tfminecraft.thievery.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.cache.Cache;
import net.tfminecraft.thievery.data.ItemCategory;
import net.tfminecraft.thievery.data.LoadoutSession;
import net.tfminecraft.thievery.data.LoadoutSession.ToggleResult;
import net.tfminecraft.thievery.data.PlayerData;
import net.tfminecraft.thievery.holder.LoadoutHolder;
import net.tfminecraft.thievery.loader.CategoryLoader;
import net.tfminecraft.util.Keys;

public class InventoryManager implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int CATEGORIES_PER_PAGE = 45;
    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_CANCEL = 48;
    private static final int SLOT_CONFIRM = 49;
    private static final int SLOT_NEXT_PAGE = 53;

    private final Map<UUID, LoadoutSession> sessions = new HashMap<>();

    public void openLoadout(Player player) {
        PlayerData playerData = Thievery.getPlayerManager().get(player);
        sessions.put(player.getUniqueId(), LoadoutSession.from(playerData));
        renderLoadout(player, 0);
    }

    private void renderLoadout(Player player, int page) {
        LoadoutSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        List<ItemCategory> categories = CategoryLoader.getAsList();
        int maxPage = Math.max(0, (categories.size() - 1) / CATEGORIES_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, maxPage));

        Inventory inv = Bukkit.createInventory(
                new LoadoutHolder(player.getUniqueId(), safePage),
                INVENTORY_SIZE,
                buildTitle(session)
        );

        int start = safePage * CATEGORIES_PER_PAGE;
        int end = Math.min(start + CATEGORIES_PER_PAGE, categories.size());
        for (int i = start; i < end; i++) {
            ItemCategory category = categories.get(i);
            boolean active = session.getDraftActive().contains(category.getId());
            boolean unlocked = session.isSessionUnlocked(category.getId());
            ItemStack icon = category.getIconItem(active, unlocked && !active);
            if (icon != null) {
                inv.setItem(i - start, icon);
            }
        }

        fillBottomBar(inv, safePage, maxPage);
        player.openInventory(inv);
    }

    private String buildTitle(LoadoutSession session) {
        return ChatColor.DARK_GRAY + "Thievery Loadout " + ChatColor.GRAY + "("
                + ChatColor.GREEN + session.getDraftBank() + ChatColor.GRAY + " bank · "
                + ChatColor.YELLOW + session.getDraftAllocated() + ChatColor.GRAY + "/"
                + ChatColor.GREEN + Cache.categoryPoints + ChatColor.GRAY + ")";
    }

    private void fillBottomBar(Inventory inv, int page, int maxPage) {
        for (int slot = 45; slot < INVENTORY_SIZE; slot++) {
            if (slot != SLOT_PREV_PAGE && slot != SLOT_CANCEL && slot != SLOT_CONFIRM && slot != SLOT_NEXT_PAGE) {
                inv.setItem(slot, createFiller());
            }
        }

        inv.setItem(SLOT_CANCEL, createButton(Material.RED_DYE, "§cCancel", "§7Discard changes"));
        inv.setItem(SLOT_CONFIRM, createButton(Material.LIME_DYE, "§aConfirm", "§7Apply loadout"));

        if (page > 0) {
            inv.setItem(SLOT_PREV_PAGE, createButton(Material.ARROW, "§ePrevious Page", null));
        } else {
            inv.setItem(SLOT_PREV_PAGE, createFiller());
        }

        if (page < maxPage) {
            inv.setItem(SLOT_NEXT_PAGE, createButton(Material.ARROW, "§eNext Page", null));
        } else {
            inv.setItem(SLOT_NEXT_PAGE, createFiller());
        }
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§8 ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createButton(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (loreLine != null) {
                meta.setLore(List.of(loreLine));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void cancelSession(Player player) {
        sessions.remove(player.getUniqueId());
        player.closeInventory();
    }

    private void applySession(Player player) {
        LoadoutSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;

        PlayerData playerData = Thievery.getPlayerManager().get(player);
        playerData.setActiveCategories(List.copyOf(session.getDraftActive()));
        playerData.setPoints(session.getDraftBank());
        player.closeInventory();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof LoadoutHolder holder)) return;
        if (!holder.getPlayerId().equals(player.getUniqueId())) return;

        e.setCancelled(true);

        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getView().getTopInventory())) {
            return;
        }

        LoadoutSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        int slot = e.getSlot();

        if (slot >= 0 && slot < CATEGORIES_PER_PAGE) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            String categoryId = clicked.getItemMeta().getPersistentDataContainer()
                    .get(Keys.categoryId, PersistentDataType.STRING);
            if (categoryId == null) return;

            ToggleResult result = session.toggleCategory(categoryId);
            switch (result) {
                case TOGGLED_ON, TOGGLED_OFF -> {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
                    renderLoadout(player, holder.getPage());
                }
                case NO_CHANGE -> {}
                case ALLOCATION_FULL -> {
                    player.sendMessage(ChatColor.RED + "You cannot allocate more than " + Cache.categoryPoints + " points.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                case NOT_ENOUGH_BANK -> {
                    player.sendMessage(ChatColor.RED + "You do not have enough bank points for that category.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                case UNKNOWN_CATEGORY -> {
                    player.sendMessage(ChatColor.RED + "Unknown category.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
            return;
        }

        if (slot == SLOT_PREV_PAGE && holder.getPage() > 0) {
            renderLoadout(player, holder.getPage() - 1);
            return;
        }

        if (slot == SLOT_NEXT_PAGE) {
            List<ItemCategory> categories = CategoryLoader.getAsList();
            int maxPage = Math.max(0, (categories.size() - 1) / CATEGORIES_PER_PAGE);
            if (holder.getPage() < maxPage) {
                renderLoadout(player, holder.getPage() + 1);
            }
            return;
        }

        if (slot == SLOT_CANCEL) {
            cancelSession(player);
            return;
        }

        if (slot == SLOT_CONFIRM) {
            if (!session.canConfirm()) {
                player.sendMessage(ChatColor.RED + "You cannot confirm this loadout.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
            applySession(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof LoadoutHolder holder)) return;
        if (!holder.getPlayerId().equals(player.getUniqueId())) return;

        sessions.remove(player.getUniqueId());
    }
}
