package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.BlockingState;
import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The 1.7 blocking slowdown, off unless {@code blockSlowdown} is enabled.
 *
 * <p>1.7's {@code EntityPlayerSP.onLivingUpdate} did this to any item in use,
 * blocking included:
 * <pre>
 * if (isUsingItem() &amp;&amp; !isRiding()) {
 *     movementInput.moveStrafe *= 0.2F;
 *     movementInput.moveForward *= 0.2F;
 *     sprintToggleTimer = 0;
 * }
 * </pre>
 * and it refused to start a sprint while {@code isUsingItem()}.
 *
 * <p>26.2 keeps the same two mechanisms, just in different places:
 * {@code modifyInput} scales the move vector, and {@code isSlowDueToUsingItem}
 * gates sprinting. A sword block is invisible to both because the sword is
 * never really "in use", so this hooks each one.
 *
 * <p>This is the only part of the mod that is not purely visual. It changes the
 * movement input, so the position packets that follow differ from what they
 * would otherwise be. It is a handicap rather than an advantage -- you move
 * slower, never faster -- but it is a real gameplay change, which is why it is
 * opt-in and why the rest of the mod stays on the other side of that line.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    /** 1.7's factor on both movement axes while an item was in use. */
    private static final float BLOCK_MOVEMENT_SCALE = 0.2f;

    private boolean oldanimations$slowedByBlocking() {
        OldAnimConfig config = OldAnimConfig.get();
        return config.enabled && config.blockSlowdown && BlockingState.isBlocking();
    }

    @Inject(
            method = "modifyInput(Lnet/minecraft/world/phys/Vec2;)Lnet/minecraft/world/phys/Vec2;",
            at = @At("RETURN"),
            cancellable = true)
    private void oldanimations$slowWhileBlocking(Vec2 input, CallbackInfoReturnable<Vec2> cir) {
        if (!this.oldanimations$slowedByBlocking()) {
            return;
        }
        // 1.7 exempted riders, and so does vanilla's own use-item slowdown.
        if (((LocalPlayer) (Object) this).isPassenger()) {
            return;
        }
        cir.setReturnValue(cir.getReturnValue().scale(BLOCK_MOVEMENT_SCALE));
    }

    @Inject(method = "isSlowDueToUsingItem()Z", at = @At("HEAD"), cancellable = true)
    private void oldanimations$noSprintWhileBlocking(CallbackInfoReturnable<Boolean> cir) {
        if (this.oldanimations$slowedByBlocking()) {
            cir.setReturnValue(true);
        }
    }
}
