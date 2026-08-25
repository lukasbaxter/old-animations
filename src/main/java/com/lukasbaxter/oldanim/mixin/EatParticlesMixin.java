package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops the burst of food particles fired the moment you finish eating.
 *
 * <p>Worth being clear that this is not a 26.2 quirk. 1.7 did exactly the same:
 * {@code onItemUseFinish} called {@code updateItemUse(itemInUse, 16)}, a
 * 16-particle burst, against the 5 that the periodic chewing spawns. It reads as
 * food still falling out of your mouth after the animation has ended because the
 * burst is spawned at your eye and then left behind as you keep moving.
 *
 * <p>Only the finishing burst goes, and only for your own player. The chewing
 * particles stay, and so does everyone else's food.
 */
@Mixin(LivingEntity.class)
public abstract class EatParticlesMixin {

    /** The periodic burst is 5; the one on completion is 16. */
    private static final int FINISH_BURST_THRESHOLD = 16;

    @Inject(method = "spawnItemParticles", at = @At("HEAD"), cancellable = true)
    private void oldanimations$noEatFinishBurst(ItemStack stack, int count, CallbackInfo ci) {
        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || !config.hideEatFinishParticles) {
            return;
        }
        if (count < FINISH_BURST_THRESHOLD) {
            return;
        }
        if ((Object) this == Minecraft.getInstance().player) {
            ci.cancel();
        }
    }
}
