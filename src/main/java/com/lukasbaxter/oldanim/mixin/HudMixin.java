package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HUD bits that 1.7 did not have.
 *
 * <h2>Heart flash</h2>
 * 26.2 pushes {@code healthBlinkTime} into the future and alternates the heart
 * sprite while it is there. Zeroing it after vanilla's own update leaves the
 * lagging "ghost" hearts and the low-health jitter untouched.
 *
 * <h2>Attack cooldown indicator</h2>
 * Vanilla already has an {@code attackIndicator} option with an OFF setting, so
 * this only exists so the whole 1.7 preset lives behind one switch. The redirect
 * inspects the value rather than the call site, so it cannot accidentally
 * swallow a different option read in the same method.
 */
@Mixin(Hud.class)
public abstract class HudMixin {

    @Shadow private long healthBlinkTime;

    @Inject(
            method = "extractPlayerHealth(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
            at = @At("TAIL"))
    private void oldanimations$noHeartFlash(CallbackInfo ci) {
        OldAnimConfig config = OldAnimConfig.get();
        if (config.enabled && config.noHealthFlash) {
            this.healthBlinkTime = 0L;
        }
    }

    @Redirect(
            method = {
                    "extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
                    "extractItemHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object oldanimations$hideAttackIndicator(OptionInstance<?> option) {
        Object value = option.get();
        OldAnimConfig config = OldAnimConfig.get();
        if (config.enabled && config.hideAttackIndicator && value instanceof AttackIndicatorStatus) {
            return AttackIndicatorStatus.OFF;
        }
        return value;
    }
}
