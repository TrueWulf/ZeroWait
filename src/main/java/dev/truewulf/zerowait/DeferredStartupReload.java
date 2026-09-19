package dev.truewulf.zerowait;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
//? if >=1.21.2 {
import net.minecraft.client.PeriodicNotificationManager;
//?}
//? if >=1.21.2 {
import net.minecraft.client.renderer.CloudRenderer;
//?}
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
//? if >=1.21.5 {
import net.minecraft.client.resources.DryFoliageColorReloadListener;
//?}
import net.minecraft.client.resources.FoliageColorReloadListener;
import net.minecraft.client.resources.GrassColorReloadListener;
import net.minecraft.client.resources.SplashManager;
//? if >=1.21.6 {
import net.minecraft.client.resources.WaypointStyleManager;
//?}
//? if >=1.21.4 {
import net.minecraft.client.resources.model.EquipmentAssetManager;
//?}
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DeferredStartupReload {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<String> DEFERRED_LISTENERS = Set.of(
            SoundManager.class.getName(),
//? if >=1.21.2 {
            PeriodicNotificationManager.class.getName(),
//?}
//? if >=1.21.2 {
            CloudRenderer.class.getName(),
//?}
            GpuWarnlistManager.class.getName(),
            LevelRenderer.class.getName(),
            BlockEntityRenderDispatcher.class.getName(),
            EntityRenderDispatcher.class.getName(),
//? if >=1.21.5 {
            DryFoliageColorReloadListener.class.getName(),
//?}
            FoliageColorReloadListener.class.getName(),
            GrassColorReloadListener.class.getName(),
//? if >=1.21.6 {
            WaypointStyleManager.class.getName(),
//?}
//? if >=1.21.4 {
            EquipmentAssetManager.class.getName(),
//?}
            SplashManager.class.getName()
    );

    private static final AtomicBoolean INITIAL_SPLIT = new AtomicBoolean(false);

    private static volatile boolean fastPassActive;
    private static volatile DeferredBatch pendingBatch;

    private DeferredStartupReload() {
    }

    public static ReloadInstance createInitialReload(ResourceManager resourceManager,
                                                     List<PreparableReloadListener> listeners,
                                                     Executor backgroundExecutor,
                                                     Executor mainThreadExecutor,
                                                     CompletableFuture<Unit> initialTask,
                                                     boolean profiling) {
        if (!ZeroWaitConfig.get().deferStartupReload || !INITIAL_SPLIT.compareAndSet(false, true)) {
            onFullReload();
            fastPassActive = false;
            return SimpleReloadInstance.create(resourceManager, listeners, backgroundExecutor, mainThreadExecutor, initialTask, profiling);
        }

        List<PreparableReloadListener> immediate = new ArrayList<>(listeners.size());
        List<PreparableReloadListener> deferred = new ArrayList<>();
        List<String> forcedPatterns = ZeroWaitConfig.get().moddedImmediatePatterns;
        for (PreparableReloadListener listener : listeners) {
            String className = listener.getClass().getName();
            if (FontManager.class.isInstance(listener)) {
                immediate.add(listener);
                deferred.add(listener);
            } else if (isForcedImmediate(className, forcedPatterns)) {
                immediate.add(listener);
            } else if (DEFERRED_LISTENERS.contains(className)) {
                deferred.add(listener);
            } else {
                immediate.add(listener);
            }
        }

        DeferredBatch batch = deferred.isEmpty() ? null : new DeferredBatch(resourceManager, deferred, backgroundExecutor, mainThreadExecutor);
        pendingBatch = batch;
        fastPassActive = batch != null;
        if (batch != null) {
            LOGGER.info("ZeroWait deferring {} of {} startup reload listeners", deferred.size(), listeners.size());
        }

        try {
            ReloadInstance reload = SimpleReloadInstance.create(resourceManager, immediate, backgroundExecutor, mainThreadExecutor, initialTask, profiling);
            if (batch != null) {
                batch.initialReloadDone = reload.done();
            }
            long startNanos = System.nanoTime();
            reload.done().whenComplete((ignored, throwable) -> {
                long millis = (System.nanoTime() - startNanos) / 1_000_000L;
                fastPassActive = false;
                if (throwable != null) {
                    LOGGER.error("ZeroWait immediate pass failed after {} ms", millis, throwable);
                } else {
                    LOGGER.info("ZeroWait immediate pass: {} listeners in {} ms", immediate.size(), millis);
                }
            });
            return reload;
        } catch (Throwable t) {
            fastPassActive = false;
            pendingBatch = null;
            throw t;
        }
    }

    private static boolean isForcedImmediate(String className, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (!pattern.isBlank() && className.contains(pattern)) {
                LOGGER.info("ZeroWait forcing listener {} into immediate pass (matches pattern '{}')", className, pattern);
                return true;
            }
        }
        return false;
    }

    private static volatile Minecraft clientRef;

    public static void onClientTick(Minecraft minecraft) {
        clientRef = minecraft;
        DeferredBatch batch = pendingBatch;
        if (batch == null || batch.started || batch.cancelled) {
            return;
        }
        CompletableFuture<?> initialDone = batch.initialReloadDone;
        if (initialDone == null || !initialDone.isDone() || initialDone.isCompletedExceptionally()) {
            if (initialDone != null && initialDone.isCompletedExceptionally()) {
                fastPassActive = false;
                pendingBatch = null;
            }
            return;
        }
        if (minecraft.level != null) {
            startDeferredReload(batch, minecraft);
            return;
        }
//? if <26.2 {
        if (minecraft.screen == null) {
            return;
        }
//?}
        if (batch.ticksAfterInitialReload++ < ZeroWaitConfig.get().deferredReloadStartDelayTicks) {
            return;
        }
        startDeferredReload(batch, minecraft);
    }

    public static void onFullReload() {
        DeferredBatch batch = pendingBatch;
        if (batch != null && !batch.started) {
            batch.cancelled = true;
            pendingBatch = null;
            LOGGER.info("Cancelled deferred startup batch, a full reload superseded it");
        }
    }

    public static boolean isFastPassActive() {
        return fastPassActive;
    }

    public static boolean shouldDeferUnicodeFonts() {
        return fastPassActive && ZeroWaitConfig.get().deferUnicodeFonts;
    }

    public static boolean shouldSkipLoadingOverlay() {
        return fastPassActive && ZeroWaitConfig.get().skipLoadingOverlay;
    }

    private static void startDeferredReload(DeferredBatch batch, Minecraft minecraft) {
        if (batch.cancelled || !batch.markStarted()) {
            return;
        }
        LOGGER.info("Starting deferred startup reload for {} listeners", batch.listeners.size());
        try {
            ReloadInstance reload = SimpleReloadInstance.create(
                    batch.resourceManager,
                    batch.listeners,
                    batch.backgroundExecutor,
                    batch.mainThreadExecutor,
                    CompletableFuture.completedFuture(Unit.INSTANCE),
                    false
            );
            reload.done().whenComplete((ignored, throwable) -> {
                if (pendingBatch == batch) {
                    pendingBatch = null;
                }
                if (throwable != null) {
                    LOGGER.error("Deferred startup reload failed", throwable);
                    scheduleFullReloadFallback(batch, minecraft);
                } else {
                    LOGGER.info("Deferred startup reload finished");
                }
            });
        } catch (Throwable t) {
            LOGGER.error("Failed to launch deferred startup reload", t);
            if (pendingBatch == batch) {
                pendingBatch = null;
            }
            scheduleFullReloadFallback(batch, minecraft);
        }
    }

    private static void scheduleFullReloadFallback(DeferredBatch batch, Minecraft minecraft) {
        if (!batch.requestFullReloadFallback() || minecraft == null) {
            return;
        }
        LOGGER.warn("ZeroWait scheduling a full resource reload to restore the listeners the deferred pass could not apply");
        try {
            minecraft.execute(minecraft::reloadResourcePacks);
        } catch (Throwable t) {
            LOGGER.error("Failed to schedule full reload fallback", t);
        }
    }

    private static final class DeferredBatch {
        final ResourceManager resourceManager;
        final List<PreparableReloadListener> listeners;
        final Executor backgroundExecutor;
        final Executor mainThreadExecutor;
        volatile CompletableFuture<?> initialReloadDone;
        volatile boolean cancelled;
        volatile boolean fallbackScheduled;
        boolean started;
        int ticksAfterInitialReload;

        DeferredBatch(ResourceManager resourceManager,
                      List<PreparableReloadListener> listeners,
                      Executor backgroundExecutor,
                      Executor mainThreadExecutor) {
            this.resourceManager = resourceManager;
            this.listeners = listeners;
            this.backgroundExecutor = backgroundExecutor;
            this.mainThreadExecutor = mainThreadExecutor;
        }

        synchronized boolean markStarted() {
            if (started) {
                return false;
            }
            started = true;
            return true;
        }

        synchronized boolean requestFullReloadFallback() {
            if (fallbackScheduled || cancelled) {
                return false;
            }
            fallbackScheduled = true;
            return true;
        }
    }
}
