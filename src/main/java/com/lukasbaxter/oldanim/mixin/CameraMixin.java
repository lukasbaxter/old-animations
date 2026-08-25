package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7 sneak camera.
 *
 * <p>The 1.7 crouch camera is <em>asymmetric</em>, and it is asymmetric by
 * accident rather than by design. 1.7 never lowered the eye height at all: it
 * set {@code ySize = 0.2} while sneak was held, and {@code Entity.moveEntity}
 * multiplied {@code ySize} by {@code 0.4} every tick. The camera sat at
 * {@code posY = bbMinY + yOffset - ySize}, so holding sneak parked it exactly
 * {@code 0.2 * 0.4 = 0.08} low from the very first tick (instant), and letting
 * go left {@code ySize} decaying 0.08, 0.032, 0.0128... back to zero -- 60% of
 * the remaining gap closed per tick, gone in about four ticks.
 *
 * <p>26.2 eases in both directions instead.
 *
 * <p>Getting this backwards is easy -- an earlier version of this mod snapped
 * both ways, which is the 1.8 behaviour, not the 1.7 one.
 *
 * <p>Only the camera is touched. Block and entity picking read
 * {@code Entity#getEyePosition}, which this does not go near, so reach and
 * targeting stay exactly vanilla.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    /** 1.7.10 crouch eye height. */
    private static final float OLD_CROUCH_EYE_HEIGHT = 1.54f;
    /** 26.2 crouch eye height, used to keep the override proportional to entity scale. */
    private static final float NEW_CROUCH_EYE_HEIGHT = 1.27f;
    /**
     * Fraction of the remaining gap closed per tick on the way back up.
     * 1.7's {@code ySize *= 0.4F} leaves 40% of the gap, so it closes 60%.
     * This was 0.5 before v1.4.0, which made standing up drag noticeably
     * longer than 1.7 did.
     */
    private static final float EASE_RATE = 0.6f;

    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;
    @Shadow @Nullable private Entity entity;

    @Inject(method = "tick", at = @At("TAIL"))
    private void oldanimations$oldSneakCamera(CallbackInfo ci) {
        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || this.entity == null) {
            return;
        }

        float target = this.entity.getEyeHeight();
        if (config.oldSneakEyeHeight && this.entity.getPose() == Pose.CROUCHING) {
            // Scale-proportional so player scale attributes still behave.
            target *= OLD_CROUCH_EYE_HEIGHT / NEW_CROUCH_EYE_HEIGHT;
        }

        // Vanilla has already shifted eyeHeightOld to last tick's value, so it is
        // the correct starting point for either curve.
        float previous = this.eyeHeightOld;

        if (config.oldSneakCamera) {
            if (target < previous || config.instantUnsneak) {
                // Going down is instant in 1.7. instantUnsneak makes coming back
                // up instant too, which is not 1.7 -- it just feels tighter.
                this.eyeHeight = target;
            } else {
                this.eyeHeight = previous + (target - previous) * EASE_RATE;
            }
        } else if (config.oldSneakEyeHeight) {
            // Not using the 1.7 curve, but still honour the 1.7 height.
            this.eyeHeight = previous + (target - previous) * EASE_RATE;
        }
    }
}
