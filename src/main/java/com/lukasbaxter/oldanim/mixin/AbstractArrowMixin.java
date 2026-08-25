package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Drops the crit particle trail from arrows you fired yourself.
 *
 * <p>A preference, not a restoration -- 1.7's {@code EntityArrow.onUpdate}
 * spawned the same {@code "crit"} stream behind a critical arrow that 26.2 does.
 * The trail is only in the way for the person who fired it, so this leaves
 * everyone else's arrows alone: a fully drawn shot coming at you still reads as
 * one.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Redirect(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle("
                            + "Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void oldanimations$hideOwnArrowTrail(
            Level level,
            ParticleOptions options,
            double x, double y, double z,
            double dx, double dy, double dz) {

        OldAnimConfig config = OldAnimConfig.get();
        if (config.enabled && config.hideOwnArrowTrail) {
            Entity owner = ((AbstractArrow) (Object) this).getOwner();
            if (owner != null && owner == Minecraft.getInstance().player) {
                return;
            }
        }
        level.addParticle(options, x, y, z, dx, dy, dz);
    }
}
