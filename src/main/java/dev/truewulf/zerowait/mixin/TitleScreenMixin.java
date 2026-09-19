package dev.truewulf.zerowait.mixin;

//? if <26.1 {
import dev.truewulf.zerowait.BootTimings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
        private void zerowait$renderBootTime(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        BootTimings.onMenuShown();
        BootTimings.drawBootLine(graphics);
    }
}
//?}
