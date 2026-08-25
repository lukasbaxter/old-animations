package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
 * targeting stay exactly vanilla -- at the cost that resizing the drop with
 * {@code sneakCameraDrop} leaves the crosshair sitting above the ray origin
 * while crouched. See that field for the numbers.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    /**
     * Fraction of the remaining gap closed per tick on the way back up.
     * 1.7's {@code ySize *= 0.4F} leaves 40% of the gap, so it closes 60%.
     * This was 0.5 before v1.4.0, which made standing up drag longer than 1.7.
     */
    private static final float EASE_RATE = 0.6f;
    /** 26.2's own rate, used when the 1.7 curve is off but the drop is resized. */
    private static final float VANILLA_EASE_RATE = 0.5f;

    /**
     * How long a pose that disagrees with your own input is treated as the
     * server echoing your action back at you rather than as a real change.
     * See {@code oldanimations$isCrouching}.
     */
    private static final int ECHO_TICKS = 5;

    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;
    @Shadow @Nullable private Entity entity;

    @Unique private int oldanimations$poseDisagreeTicks;

    /**
     * Whether the camera should be crouched, immune to MC-248973.
     *
     * <p>That bug: tap sneak once on a server and the camera dips twice. The
     * client crouches immediately, the server is told, and then the server sends
     * the state back -- so the client applies its own action a second time, a
     * round trip later. Vanilla's easing mostly hides it; snapping instantly
     * does not, which is why it shows up here as a double bounce.
     *
     * <p>For your own player the input is the truth and the synced pose is an
     * echo of it, so a pose that disagrees with the key you are holding is
     * ignored -- but only for {@link #ECHO_TICKS}. A disagreement that persists
     * past that is real (you are wedged under a block and cannot stand up), and
     * then the pose wins.
     */
    @Unique
    private boolean oldanimations$isCrouching(Entity entity) {
        boolean posed = entity.getPose() == Pose.CROUCHING;
        LocalPlayer player = Minecraft.getInstance().player;
        if (entity != player || player == null) {
            return posed;
        }

        boolean held = player.input.keyPresses.shift();
        if (posed == held) {
            this.oldanimations$poseDisagreeTicks = 0;
            return held;
        }

        this.oldanimations$poseDisagreeTicks++;
        return this.oldanimations$poseDisagreeTicks > ECHO_TICKS ? posed : held;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void oldanimations$oldSneakCamera(CallbackInfo ci) {
        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || this.entity == null) {
            return;
        }

        float target = this.entity.getEyeHeight();
        float standing = this.entity.getEyeHeight(Pose.STANDING);
        boolean crouching = this.oldanimations$isCrouching(this.entity);

        if (crouching) {
            // Resize the crouch drop rather than hard-coding 1.54, so this stays
            // correct under player scale attributes.
            float crouched = this.entity.getEyeHeight(Pose.CROUCHING);
            target = standing - (standing - crouched) * config.sneakCameraDrop;
        } else if (this.entity.getPose() == Pose.CROUCHING) {
            // The pose says crouched but your own key says otherwise, so this is
            // the echo. Stay standing rather than dipping a second time.
            target = standing;
        }

        // Vanilla has already shifted eyeHeightOld to last tick's value, so it is
        // the correct starting point for either curve.
        float previous = this.eyeHeightOld;

        if (config.oldSneakCamera) {
            if (target < previous || config.instantUnsneak) {
                // Going down is instant in 1.7. instantUnsneak makes coming back
                // up instant too, which is not 1.7, just tighter.
                this.eyeHeight = target;
            } else {
                this.eyeHeight = previous + (target - previous) * EASE_RATE;
            }
        } else if (config.sneakCameraDrop != 1.0f) {
            // Not using the 1.7 curve, but still honour the resized drop.
            this.eyeHeight = previous + (target - previous) * VANILLA_EASE_RATE;
        }
    }
}
