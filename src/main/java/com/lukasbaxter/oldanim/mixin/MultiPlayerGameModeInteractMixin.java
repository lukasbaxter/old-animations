package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.BlockingState;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops a right click that opened a chest or flipped a lever from also raising
 * the sword.
 *
 * <p>In 1.7 the block was the fallback for a right click nothing else wanted:
 * the interaction ran first and blocking only happened if it did not consume the
 * click. 26.2 runs the interaction underneath our block regardless, so without
 * this a chest opens <em>and</em> the sword goes up.
 *
 * <p>The latch is cleared when the use key comes back up, so holding the button
 * on a lever does not block either -- you have to let go and press again, which
 * is what 1.7 did.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeInteractMixin {

    @Inject(
            method = "useItemOn(Lnet/minecraft/client/player/LocalPlayer;"
                    + "Lnet/minecraft/world/InteractionHand;"
                    + "Lnet/minecraft/world/phys/BlockHitResult;)"
                    + "Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void oldanimations$interactionTakesTheClick(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {

        if (cir.getReturnValue() instanceof InteractionResult.Success) {
            BlockingState.interactionConsumedClick();
        }
    }
}
