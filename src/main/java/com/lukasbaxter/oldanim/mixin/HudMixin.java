package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7 health bar: hearts do not flash white when you take or regain damage.
 *
 * <p>26.2 sets {@code healthBlinkTime} to a future tick and alternates the heart
 * sprite while it is in the future. Zeroing it after vanilla's own update leaves
 * every other part of the health bar (the lagging "ghost" hearts, the low-health
 * jitter) exactly as it was.
 */
@Mixin(Hud.class)
public abstract class HudMixin {

    @Shadow private long healthBlinkTime;

    @Inject(method = "extractPlayerHealth(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("TAIL"))
    private void oldanimations$noHeartFlash(CallbackInfo ci) {
        OldAnimConfig config = OldAnimConfig.get();
        if (config.enabled && config.noHealthFlash) {
            this.healthBlinkTime = 0L;
        }
    }
}
