package me.sunstorm.showmanager.audio;

import javazoom.jl.player.advanced.AdvancedPlayer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.sunstorm.showmanager.ShowManager;
import me.sunstorm.showmanager.eventsystem.EventCall;
import me.sunstorm.showmanager.eventsystem.Listener;
import me.sunstorm.showmanager.eventsystem.events.time.*;
import me.sunstorm.showmanager.terminable.Terminable;

@Slf4j
public class AudioPlayer implements Terminable, Listener {
    @Getter
    private boolean enabled = false;
    private AdvancedPlayer player;

    public AudioPlayer() {
        register();
        ShowManager.getInstance().getEventBus().register(this);

    }

    @EventCall
    public void onTimeChange(TimecodeChangeEvent e) {
        if (!enabled) return;

    }

    @EventCall
    public void onTimeStart(TimecodeStartEvent e) {
        if (!enabled) return;
    }

    @EventCall
    public void onTimeStop(TimecodeStopEvent e) {
        if (!enabled) return;
    }

    @EventCall
    public void onTimePause(TimecodePauseEvent e) {
        if (!enabled) return;
    }

    @EventCall
    public void onTimeSet(TimecodeSetEvent e) {
        if (!enabled) return;
    }

    @Override
    public void shutdown() {
        log.info("Shutting down audio...");
        if (player != null)
            player.close();
    }

    public void setEnabled(boolean enabled) {
        if (!enabled && player != null)
            player.stop();
        this.enabled = enabled;
    }
}
