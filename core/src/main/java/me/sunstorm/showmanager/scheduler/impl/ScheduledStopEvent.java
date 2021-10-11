package me.sunstorm.showmanager.scheduler.impl;

import com.google.gson.JsonObject;
import me.sunstorm.showmanager.Constants;
import me.sunstorm.showmanager.Worker;
import me.sunstorm.showmanager.injection.Inject;
import me.sunstorm.showmanager.scheduler.AbstractScheduledEvent;
import me.sunstorm.showmanager.util.Timecode;

public class ScheduledStopEvent extends AbstractScheduledEvent {
    @Inject
    private Worker worker;

    public ScheduledStopEvent(Timecode executeTime) {
        super(executeTime, "stop");
        inject(false);
    }

    @Override
    public void execute() {
        worker.stop();
    }
}
