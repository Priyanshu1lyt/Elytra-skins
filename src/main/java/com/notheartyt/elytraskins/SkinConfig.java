package com.notheartyt.elytraskins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Local settings, saved to config/elytraskins.json. */
public final class SkinConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int selected = -1;
    public boolean randomizer = false;
    public List<Integer> favorites = new ArrayList<>();
    public List<Integer> recent = new ArrayList<>();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("elytraskins.json");
    }

    public static SkinConfig load() {
        try {
            Path p = file();
            if (Files.exists(p)) {
                SkinConfig c = GSON.fromJson(Files.readString(p), SkinConfig.class);
                if (c != null) {
                    if (c.favorites == null) c.favorites = new ArrayList<>();
                    if (c.recent == null) c.recent = new ArrayList<>();
                    if (c.selected >= SkinLibrary.COUNT) c.selected = -1;
                    return c;
                }
            }
        } catch (Exception ignored) {
        }
        return new SkinConfig();
    }

    public void save() {
        try {
            Files.writeString(file(), GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }
}
