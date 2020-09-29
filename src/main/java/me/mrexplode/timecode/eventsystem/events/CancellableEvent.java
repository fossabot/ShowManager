package me.mrexplode.timecode.eventsystem.events;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancellableEvent extends Event implements Cancellable {
    private boolean cancelled;
}
