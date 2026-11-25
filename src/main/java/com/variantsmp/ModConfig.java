package com.variantsmp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig extends Settings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int DATA_VERSION = 1;

    public int dataVersion = DATA_VERSION;
    public Settings defaults = new Settings();

    private transient Path path;

    public static ModConfig load(Path path) {
        ModConfig config = null;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                config = GSON.fromJson(reader, ModConfig.class);
            } catch (Exception e) {
                VariantSMP.LOGGER.error("variantSMP: could not read {}, using defaults", path, e);
            }
        }
        if (config == null) config = new ModConfig();
        config.path = path;
        config.dataVersion = DATA_VERSION;
        if (config.defaults == null) config.defaults = new Settings();
        config.defaults.sanitize();
        config.sanitize();
        config.pearlCatchEnabled = true;
        config.save();
        return config;
    }

    public void resetSettings() {
        copyFrom(defaults);
        sanitize();
        save();
    }

    public void saveAsDefaults() {
        defaults.copyFrom(this);
        save();
    }

    public void save() {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            VariantSMP.LOGGER.error("variantSMP: could not save {}", path, e);
        }
    }
}
