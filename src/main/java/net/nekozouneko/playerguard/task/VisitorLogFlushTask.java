package net.nekozouneko.playerguard.task;

import net.nekozouneko.playerguard.visitlog.VisitorLogService;

public class VisitorLogFlushTask implements Runnable {

    private final VisitorLogService service;

    public VisitorLogFlushTask(VisitorLogService service) {
        this.service = service;
    }

    @Override
    public void run() {
        service.flushIfDirty();
    }
}
