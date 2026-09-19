package dev.truewulf.zerowait;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ZeroWaitConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "zerowait.json";

    public boolean deferStartupReload = true;
    public boolean deferUnicodeFonts = true;
    public boolean skipLoadingOverlay = true;
    public boolean dedicatedReloadExecutor = true;
    public boolean fastCrashPreload = true;
    public boolean prefetchCpuInfo = true;
    public boolean showBootTimeOverlay = true;
    public int deferredReloadStartDelayTicks = 0;
    public java.util.List<String> moddedImmediatePatterns = new java.util.ArrayList<>();

    private static volatile ZeroWaitConfig active = new ZeroWaitConfig();

    public static ZeroWaitConfig get() {
        return active;
    }

    public static void attach(ZeroWaitConfig config) {
        active = config;
    }

    private void normalize() {
        if (deferredReloadStartDelayTicks < 0) {
            deferredReloadStartDelayTicks = 0;
        }
    }

    public static ZeroWaitConfig load() {
        Path path = resolvePath();
        if (path == null) {
            return new ZeroWaitConfig();
        }
        ZeroWaitConfig config = new ZeroWaitConfig();
        try {
            if (Files.exists(path)) {
                ZeroWaitConfig loaded = GSON.fromJson(Files.readString(path), ZeroWaitConfig.class);
                if (loaded != null) {
                    loaded.normalize();
                    return loaded;
                }
            } else {
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(config));
            }
        } catch (IOException | RuntimeException e) {
            ZeroWait.LOGGER.error("Failed to read config, using defaults", e);
        }
        return config;
    }

    private static Path resolvePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
