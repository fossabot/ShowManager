package me.sunstorm.showmanager.remote;

import lombok.Getter;
import lombok.Setter;
import me.sunstorm.showmanager.ShowManager;
import me.sunstorm.showmanager.Worker;
import me.sunstorm.showmanager.eventsystem.events.remote.DmxRemoteStateEvent;
import me.sunstorm.showmanager.util.DmxAddress;

@Getter
public class DmxRemoteControl {
    private static final int TOLERANCE = 5;
    @Setter private boolean enabled = false;
    private DmxRemoteState state = DmxRemoteState.DISABLED;
    private DmxRemoteState previousState = DmxRemoteState.DISABLED;
    @Setter private DmxAddress address = new DmxAddress(0, 0, 0);

    public void handleData(byte[] dmxData) {
        if (!enabled) {
            state = DmxRemoteState.DISABLED;
            return;
        }
        byte value = dmxData[address.getAddress() - 1];
        if (inToleratedRange(25, value)) {
            state = DmxRemoteState.FORCE_IDLE;
            if (state != previousState) {
                DmxRemoteStateEvent event = new DmxRemoteStateEvent(state, previousState);
                event.call(ShowManager.getInstance().getEventBus());
                previousState = state;
            }
        } else if (inToleratedRange(51, value)) {
            state = DmxRemoteState.PLAYING;
            if (state != previousState) {
                DmxRemoteStateEvent event = new DmxRemoteStateEvent(state, previousState);
                event.call(ShowManager.getInstance().getEventBus());
                previousState = state;
                ShowManager.getInstance().getWorker().play();
            }
        } else if (inToleratedRange(76, value)) {
            state = DmxRemoteState.PAUSE;
            if (state != previousState) {
                DmxRemoteStateEvent event = new DmxRemoteStateEvent(state, previousState);
                event.call(ShowManager.getInstance().getEventBus());
                previousState = state;
                ShowManager.getInstance().getWorker().pause();
            }
        } else if (inToleratedRange(102, value)) {
            state = DmxRemoteState.STOPPED;
            if (state != previousState) {
                DmxRemoteStateEvent event = new DmxRemoteStateEvent(state, previousState);
                event.call(ShowManager.getInstance().getEventBus());
                previousState = state;
                ShowManager.getInstance().getWorker().stop();
            }
        } else {
            state = DmxRemoteState.IDLE;
            if (state != previousState) {
                DmxRemoteStateEvent event = new DmxRemoteStateEvent(state, previousState);
                event.call(ShowManager.getInstance().getEventBus());
                previousState = state;
            }
        }
    }

    private boolean inToleratedRange(int origin, byte value) {
        return value <= origin + TOLERANCE && value >= origin - TOLERANCE;
    }
}
