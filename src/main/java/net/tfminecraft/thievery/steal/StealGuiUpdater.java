package net.tfminecraft.thievery.steal;

import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.steal.StealManager;

import org.bukkit.scheduler.BukkitTask;

public class StealGuiUpdater {

    private final StealManager stealManager;
    private BukkitTask task;

    public StealGuiUpdater(StealManager stealManager) {
        this.stealManager = stealManager;
    }

    public void start() {
        stop();
        task = Thievery.getInstance().getServer().getScheduler().runTaskTimer(Thievery.getInstance(),
                stealManager::tickAll, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
