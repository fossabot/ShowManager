package me.sunstorm.showmanager.eventsystem.events.audio;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.sunstorm.showmanager.audio.AudioTrack;
import me.sunstorm.showmanager.eventsystem.events.Event;

@Getter
@AllArgsConstructor
public class AudioLoadEvent extends Event {
    private final AudioTrack track;
}
