package net.nekozouneko.playerguard.task;

import net.nekozouneko.playerguard.visitlog.VisitorLogService;
import org.bukkit.scheduler.BukkitRunnable;

public class VisitorLogFlushTask extends BukkitRunnable {

    private final VisitorLogService service;

    public VisitorLogFlushTask(VisitorLogService service) {
        this.service = service;
    }

    @Override
    public void run() {
        service.flushIfDirty();
    }
}
