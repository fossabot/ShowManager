package me.sunstorm.showmanager.audio;

import com.google.common.base.Stopwatch;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import me.sunstorm.showmanager.ShowManager;
import me.sunstorm.showmanager.eventsystem.events.audio.AudioLoadEvent;
import me.sunstorm.showmanager.eventsystem.events.audio.AudioPauseEvent;
import me.sunstorm.showmanager.eventsystem.events.audio.AudioStartEvent;
import me.sunstorm.showmanager.eventsystem.events.audio.AudioStopEvent;
import me.sunstorm.showmanager.injection.InjectRecipient;
import me.sunstorm.showmanager.util.Sampler;
import me.sunstorm.showmanager.util.Timecode;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

import static me.sunstorm.showmanager.util.SilentClose.close;

@Slf4j
@Getter
@RequiredArgsConstructor
public class AudioTrack implements InjectRecipient {
    @NonNull
    private Timecode startTime;
    private final File file;
    private boolean loaded = false;
    private boolean paused = false;
    @Nullable private Timecode endTime;
    @Nullable private transient Clip clip;
    @Nullable private transient float[] samples;
    @Nullable private transient AudioInputStream stream;

    public AudioTrack loadTrack(Mixer mixer) {
        discard();
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("Loading track {}...", file.getName());
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            stream = AudioSystem.getAudioInputStream(in);
            AudioFormat format = stream.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
            samples = Sampler.sample(stream);
            val sourceInfo = new DataLine.Info(Clip.class, format, ((int) stream.getFrameLength() * format.getFrameSize()));
            clip = (Clip) mixer.getLine(sourceInfo);
            clip.flush();
            clip.open(stream);
            clip.addLineListener(lineEvent -> {
                if (lineEvent.getType() == LineEvent.Type.STOP && !paused) {
                    AudioStopEvent event = new AudioStopEvent(this);
                    event.call(ShowManager.getInstance().getEventBus());
                }
            });
            endTime = startTime.add(new Timecode(clip.getMicrosecondLength() / 1000));
            loaded = true;
            AudioLoadEvent event = new AudioLoadEvent(this);
            event.call(ShowManager.getInstance().getEventBus());
            log.info("Loaded track {} in {} ms", file.getName(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        } catch (IOException e) {
            log.error("Failed to load track", e);
        } catch (UnsupportedAudioFileException e) {
            log.error("Track audio type not supported", e);
        } catch (LineUnavailableException e) {
            log.error("Failed to open mixer line", e);
        }
        return this;
    }

    public void play() {
        if (clip != null) {
            AudioStartEvent event = new AudioStartEvent(this);
            event.call(ShowManager.getInstance().getEventBus());
            if (!event.isCancelled()) {
                clip.start();
                paused = false;
            }
        } else {
            log.warn("Tried to play track {} without loading", file.getName());
        }
    }

    public void pause() {
        if (clip != null) {
            AudioPauseEvent event = new AudioPauseEvent(this);
            event.call(ShowManager.getInstance().getEventBus());
            if (!event.isCancelled()) {
                paused = true;
                clip.stop();
            }
        } else {
            log.warn("Tried to pause track {} without loading", file.getName());
        }
    }

    public void stop() {
        if (clip != null) {
            AudioStopEvent event = new AudioStopEvent(this);
            event.call(ShowManager.getInstance().getEventBus());
            if (!event.isCancelled())
                clip.stop();
        } else {
            log.warn("Tried to stop track {} without loading", file.getName());
        }
    }

    public void discard() {
        stop();
        if (clip != null) {
            clip.close();
        }
        if (stream != null) {
            close(stream);
        }
        loaded = false;
    }

    public void setStartTime(Timecode time) {
        if (clip != null && clip.isRunning()) {
            log.warn("Moving a track on the timeline while it's playing not allowed");
            return;
        }
        startTime = time;
        endTime = startTime.add(new Timecode(clip.getMicrosecondLength() / 1000));
    }

    public String getName() {
        return file.getName().substring(0, file.getName().lastIndexOf('.'));
    }
}
