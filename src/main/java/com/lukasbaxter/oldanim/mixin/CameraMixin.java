package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import com.lukasbaxter.oldanim.OldAnimations;
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
 * <p>Vanilla eases the camera toward the current eye height by 50% per tick
 * ({@code eyeHeight += (target - eyeHeight) * 0.5}), which is what makes modern
 * crouching feel soft. 1.7 had no easing at all, and crouched at 1.54 rather
 * than 1.27.
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

        if (config.instantSneakCamera) {
            // Snap, and kill the interpolation the renderer does between
            // eyeHeightOld and eyeHeight.
            this.eyeHeight = target;
            this.eyeHeightOld = target;
        } else if (config.oldSneakEyeHeight) {
            // Keep vanilla easing but toward the 1.7 height.
            this.eyeHeight = this.eyeHeightOld + (target - this.eyeHeightOld) * 0.5f;
        }
    }
}
