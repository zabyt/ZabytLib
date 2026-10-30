package com.zabyt.lib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;


public abstract class SimpleConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("zabytlib");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private transient String modId;

    protected SimpleConfig(String modId) {
        this.modId = modId;
    }

    private static Path pathFor(String modId) {
        return FabricLoader.getInstance().getConfigDir().resolve(modId + ".json");
    }

    public void save() {
        try {
            Path path = pathFor(modId);
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to save config", modId, e);
        }
    }

    public static <T extends SimpleConfig> T load(String modId, Class<T> type) {
        Path path = pathFor(modId);
        T config;
        try {
            if (Files.exists(path)) {
                config = GSON.fromJson(Files.readString(path), type);
                if (config == null) {
                    config = construct(type);
                }
            } else {
                config = construct(type);
            }
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to load config, using defaults", modId, e);
            try {
                config = construct(type);
            } catch (Exception fatal) {
                throw new IllegalStateException("Cannot construct config " + type, fatal);
            }
        }
        ((SimpleConfig) config).modId = modId;
        com.zabyt.lib.ZabytLib.registerConfigUser(modId);
        return config;
    }

    private static <T extends SimpleConfig> T construct(Class<T> type) throws Exception {
        var constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
