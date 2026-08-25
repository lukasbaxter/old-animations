package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Predicted 1.7/1.8 crit particles. Off unless {@code predictCrits} is enabled.
 *
 * <h2>Why a prediction and not a restoration</h2>
 * The client half of crit particles is unchanged: 26.2 spawns them exactly the
 * way 1.8 did, from {@code ClientboundAnimatePacket} action 4 into
 * {@code ParticleEngine.createTrackingEmitter(entity, ParticleTypes.CRIT)}. What
 * changed is who decides. 1.7 and 1.8 critted on any falling hit; 1.9 combat
 * added the attack cooldown, so a 26.2 server only crits when the cooldown is
 * essentially full and sends no packet otherwise. That decision is server-side
 * and a client cannot move it.
 *
 * <p>So this spawns the emitter locally when the hit satisfies 1.7's own
 * condition, taken from {@code EntityPlayer.attackTargetEntityWithCurrentItem}:
 * <pre>
 * fallDistance &gt; 0 &amp;&amp; !onGround &amp;&amp; !isOnLadder() &amp;&amp; !isInWater()
 *   &amp;&amp; !isPotionActive(blindness) &amp;&amp; ridingEntity == null
 *   &amp;&amp; target instanceof EntityLivingBase
 * </pre>
 * (There is deliberately no sprinting term. 1.7 and 1.8 both let you crit while
 * sprinting; "no crits while sprinting" is a 1.9 rule.)
 *
 * <h2>What this costs you</h2>
 * <strong>The particles can lie.</strong> They say "1.8 would have critted
 * this", not "this hit dealt crit damage". On a 26.2-combat server a hit with a
 * partial cooldown will show particles and do normal damage. If the server does
 * crit, it still sends its own packet, so you get both emitters at once.
 *
 * <p>Nothing is sent and nothing is suppressed -- the particles exist only on
 * this client, and no other player sees them.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(
            method = "attack(Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/minecraft/world/entity/Entity;)V",
            at = @At("TAIL"))
    private void oldanimations$predictCritParticles(Player player, Entity target, CallbackInfo ci) {
        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || !config.predictCrits) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (player != minecraft.player) {
            return;
        }
        if (!(target instanceof LivingEntity)) {
            return;
        }
        if (!(player.fallDistance > 0.0)
                || player.onGround()
                || player.onClimbable()
                || player.isInWater()
                || player.hasEffect(MobEffects.BLINDNESS)
                || player.isPassenger()) {
            return;
        }

        minecraft.particleEngine.createTrackingEmitter(target, ParticleTypes.CRIT);
    }
}
