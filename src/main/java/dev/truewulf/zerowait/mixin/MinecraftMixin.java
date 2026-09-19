package dev.truewulf.zerowait.mixin;

//? if >=1.21.2 {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//?}
import dev.truewulf.zerowait.BootExecutors;
import dev.truewulf.zerowait.DeferredStartupReload;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=1.21.2 {
import net.minecraft.TracingExecutor;
import java.util.concurrent.Executor;
//?}

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

//? if >=1.21.2 {
    @WrapOperation(
            method = {"<init>", "reloadResourcePacks"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/TracingExecutor;forName(Ljava/lang/String;)Ljava/util/concurrent/Executor;"
            )
    )
    private Executor zerowait$resourceLoadExecutor(TracingExecutor executor, String name, Operation<Executor> original) {
        return BootExecutors.resourceLoad(executor, name);
    }
//?}

    @Inject(method = "tick", at = @At("TAIL"))
    private void zerowait$onTick(CallbackInfo ci) {
        DeferredStartupReload.onClientTick((Minecraft) (Object) this);
    }
}
