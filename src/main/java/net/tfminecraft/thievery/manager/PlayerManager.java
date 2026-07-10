package net.tfminecraft.thievery.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.data.PlayerData;
import net.tfminecraft.thievery.database.Database;
import net.tfminecraft.thievery.util.DexterityHelper;

public class PlayerManager implements Listener {

    private static final long POINT_TASK_INTERVAL_TICKS = 20L * 60L;

    private final HashMap<UUID, PlayerData> data = new HashMap<>();
    private BukkitRunnable pointTask;

    public boolean exists(Player p) {
        return data.containsKey(p.getUniqueId());
    }

    public boolean exists(UUID id) {
        return data.containsKey(id);
    }

    public PlayerData get(Player p) {
        return get(p.getUniqueId());
    }

    public PlayerData get(UUID id) {
        if (!exists(id)) add(id);
        return data.get(id);
    }

    public void add(UUID id) {
        if (exists(id)) return;
        PlayerData playerData;
        if (Database.hasPlayerData(id)) {
            playerData = Database.loadPlayerData(id);
            playerData.applyPointGain();
        } else {
            playerData = new PlayerData(id);
        }
        data.put(id, playerData);
    }

    public void init(Player p) {
        if (exists(p)) return;
        add(p.getUniqueId());
    }

    public void unloadAll() {
        stopPointTask();
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            if (exists(id)) {
                Database.savePlayerData(data.get(id));
            }
        }
        data.clear();
    }

    public void loadAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            data.remove(id);
            add(id);
        }
    }

    public void start() {
        startPointTask();
        loadAll();
    }

    public void stop() {
        stopPointTask();
    }

    private void startPointTask() {
        stopPointTask();
        pointTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickPointGain();
            }
        };
        pointTask.runTaskTimer(Thievery.getInstance(), POINT_TASK_INTERVAL_TICKS, POINT_TASK_INTERVAL_TICKS);
    }

    private void stopPointTask() {
        if (pointTask != null) {
            pointTask.cancel();
            pointTask = null;
        }
    }

    private void tickPointGain() {
        for (UUID id : new ArrayList<>(data.keySet())) {
            PlayerData playerData = data.get(id);
            playerData.applyPointGain();
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                playerData.applyRiskDecay(DexterityHelper.getDexterity(player));
            }
        }
    }

    public void save(UUID id) {
        if (!exists(id)) return;
        Database.savePlayerData(get(id));
        data.remove(id);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        init(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        save(e.getPlayer().getUniqueId());
    }
}
