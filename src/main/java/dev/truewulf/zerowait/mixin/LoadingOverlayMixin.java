package dev.truewulf.zerowait.mixin;

import dev.truewulf.zerowait.BootTimings;
import dev.truewulf.zerowait.DeferredStartupReload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private ReloadInstance reload;

    @Shadow
    @Final
    private Consumer<Optional<Throwable>> onFinish;

    @Unique
    private boolean zerowait$finished;

    @Unique
    private boolean zerowait$markedFirstFrame;

//? if <26.1 {
    @Inject(method = "render", at = @At("TAIL"))
    private void zerowait$markOverlayFrame(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!zerowait$markedFirstFrame) {
            zerowait$markedFirstFrame = true;
            BootTimings.mark("overlay first frame");
        }
    }
//?}

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void zerowait$finishImmediately(CallbackInfo ci) {
        if (!DeferredStartupReload.shouldSkipLoadingOverlay() || this.zerowait$finished || !this.reload.isDone()) {
            return;
        }
        this.zerowait$finished = true;
        try {
            this.reload.checkExceptions();
            this.onFinish.accept(Optional.empty());
        } catch (Throwable t) {
            this.onFinish.accept(Optional.of(t));
        }
//? if <26.2 {
        this.minecraft.setOverlay(null);
//?}
        ci.cancel();
    }
}
