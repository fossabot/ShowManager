package me.sunstorm.showmanager.settings;

import com.google.gson.JsonObject;
import me.sunstorm.showmanager.settings.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class SettingsHolder {
    private final String name;

    public SettingsHolder(String name) {
        this.name = name;
    }

    public final void load() {
        Project.current().loadSettingsHolder(this);
    }

    @NotNull
    public abstract JsonObject getData();

    public abstract void onLoad(@NotNull JsonObject object);

    public String getName() {
        return name;
    }
}
