package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional: pin the swing animation to 1.7's flat 6 ticks.
 *
 * <p>26.2 reads the duration from the held item's {@code SwingAnimation}
 * component, whose default is already 6 -- so for ordinary swords and tools
 * this changes nothing. It only bites for items that ship a longer or shorter
 * swing (spears and the like). Off by default for that reason.
 *
 * <p>Scoped to client-side players so the integrated server's own
 * {@code ServerPlayer} ticking is left alone.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    private static final int OLD_SWING_DURATION = 6;

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void oldanimations$fixedSwingDuration(CallbackInfoReturnable<Integer> cir) {
        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || !config.fixedSwingDuration) {
            return;
        }
        if (!((Object) this instanceof AbstractClientPlayer)) {
            return;
        }
        cir.setReturnValue(OLD_SWING_DURATION);
    }
}
