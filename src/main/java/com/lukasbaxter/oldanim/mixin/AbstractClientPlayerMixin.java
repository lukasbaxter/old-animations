package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pins the field of view so it stops moving when your speed does.
 *
 * <p>This is the modifier vanilla scales by its FOV Effects setting: sprinting,
 * speed potions and flying all push it. Returning a flat 1 leaves the FOV at
 * whatever you set it to and nothing else touches it.
 *
 * <p>Like the bobbing switch, this is here because 26.2 stopped surfacing the
 * vanilla FOV Effects slider, not because 1.7 behaved differently.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getFieldOfViewModifier(ZF)F", at = @At("HEAD"), cancellable = true)
    private void oldanimations$noFovEffects(
            boolean isFirstPerson, float fovEffectScale, CallbackInfoReturnable<Float> cir) {
        OldAnimConfig config = OldAnimConfig.get();
        if (config.enabled && config.noFovEffects) {
            cir.setReturnValue(1.0f);
        }
    }
}
