package me.sunstorm.showmanager.http.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import me.sunstorm.showmanager.audio.AudioPlayer;
import me.sunstorm.showmanager.audio.marker.Marker;
import me.sunstorm.showmanager.http.WebSocketHandler;
import me.sunstorm.showmanager.injection.Inject;
import me.sunstorm.showmanager.injection.InjectRecipient;
import me.sunstorm.showmanager.util.JsonBuilder;
import me.sunstorm.showmanager.util.Timecode;

@Slf4j
public class AudioController implements InjectRecipient {
    @Inject
    private AudioPlayer player;
    @Inject
    private WebSocketHandler wsHandler;

    public AudioController() {
        inject();
    }

    public void postVolume(Context ctx) {
        JsonObject data = JsonParser.parseString(ctx.body()).getAsJsonObject();
        if (!data.has("volume"))
            throw new BadRequestResponse();
        player.setVolume(data.get("volume").getAsInt());
    }

    public void getInfo(Context ctx) {
        JsonObject data = new JsonObject();
        if (player.getCurrent() == null) {
            data.addProperty("loaded", "");
            data.addProperty("volume", 100);
            data.addProperty("playing", false);
            data.add("markers", new JsonArray());
        } else {
            data.addProperty("loaded", player.getCurrent().getName());
            data.addProperty("volume", (int) (player.getCurrent().getVolume() * 100));
            data.addProperty("playing", player.getCurrent().getClip().isRunning());
            data.add("markers", buildMarkers());
        }
        ctx.json(data);
    }

    public void getMarkers(Context ctx) {
        JsonObject data = new JsonObject();
        data.add("markers", buildMarkers());
        ctx.json(data);
    }

    public void markerJump(Context ctx) {
        JsonObject data = JsonParser.parseString(ctx.body()).getAsJsonObject();
        if (player.getCurrent() == null || !data.has("name"))
            throw new BadRequestResponse();
        val marker = player.getCurrent().getMarkers().stream().filter(m -> m.getLabel().equals(data.get("name").getAsString())).findFirst().get();
        log.info("Jumping to marker {} - {}", marker.getLabel(), marker.getTime().guiFormatted(false));
        marker.jump();
    }

    public void addMarker(Context ctx) {
        JsonObject data = JsonParser.parseString(ctx.body()).getAsJsonObject();
        if (player.getCurrent() == null)
            throw new BadRequestResponse();
        val marker = new Marker(data.get("name").getAsString(), new Timecode(
                data.get("hour").getAsInt(),
                data.get("min").getAsInt(),
                data.get("sec").getAsInt(),
                data.get("frame").getAsInt(),
                25
        ));
        log.info("Adding marker {} - {}", marker.getLabel(), marker.getTime().guiFormatted(true));
        player.getCurrent().getMarkers().add(marker);
        //should have a separate marker event on the bus, but hardwiring here for now. Same for OutputController#update
        //I slowly start breaking my own rules
        wsHandler.broadcast(
                new JsonBuilder()
                .addProperty("type", "audio")
                .addProperty("action", "marker")
                .build()
        );
    }

    public void deleteMarker(Context ctx) {
        JsonObject data = JsonParser.parseString(ctx.body()).getAsJsonObject();
        if (player.getCurrent() == null || !data.has("name"))
            throw new BadRequestResponse();
        boolean success = player.getCurrent().getMarkers().removeIf(m -> m.getLabel().equals(data.get("name").getAsString()));
        if (success)
            log.info("Deleted marker {}", data.get("name").getAsString());
        else
            log.warn("Attempted to delete non-existing marker: {}", data.get("name").getAsString());
        wsHandler.broadcast(
                new JsonBuilder()
                        .addProperty("type", "audio")
                        .addProperty("action", "marker")
                        .build()
        );
    }

    private JsonArray buildMarkers() {
        JsonArray array = new JsonArray();
        player.getCurrent().getMarkers().forEach(m -> array.add(new JsonBuilder().addProperty("label", m.getLabel()).addProperty("time", m.getTime().guiFormatted(false)).build()));
        return array;
    }
}
