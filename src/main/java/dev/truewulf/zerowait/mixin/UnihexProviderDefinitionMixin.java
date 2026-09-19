package dev.truewulf.zerowait.mixin;

import com.mojang.blaze3d.font.GlyphProvider;
import dev.truewulf.zerowait.DeferredStartupReload;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.client.gui.font.providers.UnihexProvider;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(UnihexProvider.Definition.class)
public abstract class UnihexProviderDefinitionMixin {

    private static final GlyphProvider EMPTY_PROVIDER = new GlyphProvider() {
        @Override
        public IntSet getSupportedGlyphs() {
            return IntSets.EMPTY_SET;
        }
    };

    @Inject(method = "load", at = @At("HEAD"), cancellable = true)
    private void zerowait$skipUnihexDuringStartup(ResourceManager resourceManager, CallbackInfoReturnable<GlyphProvider> cir) {
        if (DeferredStartupReload.shouldDeferUnicodeFonts()) {
            cir.setReturnValue(EMPTY_PROVIDER);
        }
    }
}
