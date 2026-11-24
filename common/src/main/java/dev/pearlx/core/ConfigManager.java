package dev.pearlx.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Loads, validates and persists the Pearl X configuration. */
public final class ConfigManager {

    private final Path file;
    private final List<Runnable> listeners = new ArrayList<>();
    private PearlXConfig config = new PearlXConfig();
    private volatile boolean dirty;

    public ConfigManager(Path file) {
        this.file = file;
    }

    public void load() {
        PearlXConfig loaded = Json.read(file, PearlXConfig.class);
        if (loaded == null) {
            config = new PearlXConfig();
            config.validate();
            saveNow();
            Log.info("Created default configuration at " + file);
            return;
        }
        config = loaded;
        config.validate();
        Log.info("Loaded configuration from " + file);
    }

    public PearlXConfig get() {
        return config;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /** Called by the GUI after every edit: values apply immediately, the file is written shortly. */
    public void requestSave() {
        dirty = true;
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    /** Called once per client tick; writes at most one small file when something changed. */
    public void flushIfDirty() {
        if (!dirty) {
            return;
        }
        dirty = false;
        saveNow();
    }

    public void saveNow() {
        config.validate();
        try {
            Json.writeAtomic(file, Json.GSON.toJson(config));
        } catch (IOException e) {
            Log.error("Could not save configuration", e);
        }
    }

    public void resetToDefaults() {
        config = new PearlXConfig();
        config.validate();
        saveNow();
        dirty = false;
        for (Runnable listener : listeners) {
            listener.run();
        }
        Log.info("Configuration reset to defaults");
    }

    public Path file() {
        return file;
    }
}
