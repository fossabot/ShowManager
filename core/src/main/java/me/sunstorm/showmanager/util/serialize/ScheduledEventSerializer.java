package me.sunstorm.showmanager.util.serialize;

import com.google.gson.*;
import com.illposed.osc.OSCMessage;
import me.sunstorm.showmanager.ShowManager;
import me.sunstorm.showmanager.modules.scheduler.ScheduledEvent;
import me.sunstorm.showmanager.modules.scheduler.impl.ScheduledJumpEvent;
import me.sunstorm.showmanager.modules.scheduler.impl.ScheduledOscEvent;
import me.sunstorm.showmanager.modules.scheduler.impl.ScheduledPauseEvent;
import me.sunstorm.showmanager.modules.scheduler.impl.ScheduledStopEvent;
import me.sunstorm.showmanager.util.Timecode;

import java.lang.reflect.Type;

public class ScheduledEventSerializer implements JsonSerializer<ScheduledEvent>, JsonDeserializer<ScheduledEvent> {

    @Override
    public JsonElement serialize(ScheduledEvent src, Type typeOfSrc, JsonSerializationContext context) {
        return src.getData();
    }

    @Override
    public ScheduledEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject data = json.getAsJsonObject();
        String type = data.get("type").getAsString();
        Timecode time = context.deserialize(data.get("time"), Timecode.class);
        var instance =  switch (type) {
            case "jump" -> new ScheduledJumpEvent(time, context.deserialize(data.get("jumpTime"), Timecode.class));
            case "osc" -> new ScheduledOscEvent(time, context.deserialize(data.get("packet"), OSCMessage.class));
            case "pause" -> new ScheduledPauseEvent(time);
            case "stop" -> new ScheduledStopEvent(time);
            default -> null;
        };
        if (ShowManager.FEATHER != null && instance != null) {
            ShowManager.FEATHER.injectFields(instance);
        }
        return instance;
    }
}
