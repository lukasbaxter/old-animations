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
 * <p>The 1.7 crouch camera is <em>asymmetric</em>: dropping into a crouch is
 * instant, but standing back up eases toward the new height at 50% per tick.
 * 26.2 eases in both directions, which makes entering a crouch feel soft, and
 * 1.8 was instant in both directions, which is a different feel again.
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
    /** Vanilla's per-tick approach rate, which 1.7 also used on the way up. */
    private static final float EASE_RATE = 0.5f;

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
            this.eyeHeight = target < previous ? target : previous + (target - previous) * EASE_RATE;
        } else if (config.oldSneakEyeHeight) {
            // Not using the 1.7 curve, but still honour the 1.7 height.
            this.eyeHeight = previous + (target - previous) * EASE_RATE;
        }
    }
}
