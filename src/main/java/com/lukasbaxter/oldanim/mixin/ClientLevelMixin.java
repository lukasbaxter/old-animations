package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops the sweep attack arc.
 *
 * <p>This one <em>is</em> a restoration: the sweep is a 1.9 addition, and it is
 * the reason spam-clicking in 26.2 looks like the game is picking between
 * several swings. The arm itself is unchanged from 1.7 -- same 6 tick duration
 * from {@code SwingAnimation.DEFAULT}, same haste/fatigue adjustment, same
 * "only restart past halfway" rule -- but a full-cooldown hit throws a large
 * white arc in front of you and a partial one does not, so an irregular click
 * rate reads as an irregular animation.
 *
 * <p>The particle is spawned by the server and arrives as an ordinary particle
 * packet, so this only stops it being drawn on this client. Other players still
 * see their own.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    private boolean oldanimations$isSweep(ParticleOptions options) {
        OldAnimConfig config = OldAnimConfig.get();
        return config.enabled
                && config.hideSweepAttack
                && options.getType() == ParticleTypes.SWEEP_ATTACK;
    }

    @Inject(
            method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            at = @At("HEAD"),
            cancellable = true)
    private void oldanimations$noSweep(
            ParticleOptions options, double x, double y, double z,
            double dx, double dy, double dz, CallbackInfo ci) {
        if (this.oldanimations$isSweep(options)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V",
            at = @At("HEAD"),
            cancellable = true)
    private void oldanimations$noSweepForced(
            ParticleOptions options, boolean force, boolean decreased,
            double x, double y, double z, double dx, double dy, double dz, CallbackInfo ci) {
        if (this.oldanimations$isSweep(options)) {
            ci.cancel();
        }
    }
}
