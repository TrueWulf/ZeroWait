package dev.truewulf.zerowait.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.truewulf.zerowait.DeferredStartupReload;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.List;

@Mixin(ReloadableResourceManager.class)
public abstract class ReloadableResourceManagerMixin {

    @Shadow
    @Final
    private PackType type;

    @WrapOperation(
            method = "createReload",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/SimpleReloadInstance;create(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Z)Lnet/minecraft/server/packs/resources/ReloadInstance;"
            )
    )
    private ReloadInstance zerowait$splitStartupReload(ResourceManager resourceManager,
                                                      List<PreparableReloadListener> listeners,
                                                      Executor backgroundExecutor,
                                                      Executor mainThreadExecutor,
                                                      CompletableFuture<Unit> initialTask,
                                                      boolean profiling,
                                                      Operation<ReloadInstance> original) {
        if (type != PackType.CLIENT_RESOURCES) {
            return original.call(resourceManager, listeners, backgroundExecutor, mainThreadExecutor, initialTask, profiling);
        }
        return DeferredStartupReload.createInitialReload(resourceManager, listeners, backgroundExecutor, mainThreadExecutor, initialTask, profiling);
    }
}
