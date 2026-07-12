package net.tfminecraft.thievery.manager;

import org.bukkit.scheduler.BukkitTask;

import net.tfminecraft.thievery.Thievery;

public class StealGuiUpdater {

    private final RobberyManager robberyManager;
    private final PickpocketManager pickpocketManager;
    private final ContainerManager containerManager;
    private BukkitTask task;

    public StealGuiUpdater(RobberyManager robberyManager, PickpocketManager pickpocketManager,
            ContainerManager containerManager) {
        this.robberyManager = robberyManager;
        this.pickpocketManager = pickpocketManager;
        this.containerManager = containerManager;
    }

    public void start() {
        stop();
        task = Thievery.getInstance().getServer().getScheduler().runTaskTimer(Thievery.getInstance(), () -> {
            robberyManager.tickActiveGuis();
            pickpocketManager.tickOpenGuis();
            containerManager.tickOpenChestGuis();
        }, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
