package dev.truewulf.zerowait.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//? if <26.3 {
import com.mojang.blaze3d.platform.GLX;
//?}
import dev.truewulf.zerowait.BootTimings;
import dev.truewulf.zerowait.ZeroWaitConfig;
import net.minecraft.CrashReport;
import net.minecraft.client.main.Main;
import net.minecraft.util.MemoryReserve;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class ClientMainMixin {

    @Inject(method = "main", at = @At("HEAD"))
    private static void zerowait$onMainStart(String[] args, CallbackInfo ci) {
        BootTimings.mark("main entry");
        ZeroWaitConfig.attach(ZeroWaitConfig.load());
        ZeroWaitConfig config = ZeroWaitConfig.get();
//? if <26.3 {
        if (config.prefetchCpuInfo) {
            Thread.ofPlatform().daemon().name("ZeroWait-CpuInfo").start(() -> {
                try {
                    GLX._getCpuInfo();
                } catch (Throwable ignored) {
                }
            });
        }
//?}
    }

    @WrapOperation(
            method = "main",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;preload()V")
    )
    private static void zerowait$fastCrashPreload(Operation<Void> original) {
        if (!ZeroWaitConfig.get().fastCrashPreload) {
            original.call();
            return;
        }
        MemoryReserve.allocate();
        BootTimings.mark("crash preload replaced");
    }
}
