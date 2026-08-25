package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.HurtTint;
import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the red damage tint up for longer than vanilla's 10 ticks.
 *
 * <p>Vanilla sets {@code hasRedOverlay} from {@code hurtTime > 0 || deathTime > 0}.
 * 1.7 used the same 10 ticks, so this is a preference, not a restoration -- what
 * actually changed between the eras is the attack cooldown thinning out how
 * often you land a hit at all.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;"
                    + "Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void oldanimations$longerHurtTint(
            LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {

        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || config.hurtTintTicks <= 10) {
            return;
        }
        if (HurtTint.isTinted(entity, config.hurtTintTicks)) {
            state.hasRedOverlay = true;
        }
    }
}
