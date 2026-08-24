package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third-person biped poses.
 *
 * <h2>Crouching</h2>
 * 26.2 poses a crouch as:
 * <pre>head.y += 4.2   body.y += 3.2   arms.y += 3.2   legs.z += 4</pre>
 * 1.7.10's {@code ModelBiped} instead did:
 * <pre>head.y = 1.0    body.y unchanged   arms.y unchanged   legs.z = 4   legs.y = 9 (from 12)</pre>
 * so the deltas below convert one into the other. They are relative, which keeps
 * them correct for baby/scaled meshes that bake a Y offset into the part poses.
 *
 * <h2>Blocking arm</h2>
 * Vanilla's blocking arm yaws 30 degrees inward and tracks the head. 1.7 did
 * neither -- it only pitched the arm by {@code xRot * 0.5 - 3 * PI/10}.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    // Converts the 26.2 crouch offsets into the 1.7 ones.
    private static final float CROUCH_HEAD_Y_DELTA = 1.0f - 4.2f;   // -3.2
    private static final float CROUCH_BODY_Y_DELTA = -3.2f;
    private static final float CROUCH_ARM_Y_DELTA = -3.2f;
    private static final float CROUCH_LEG_Y_DELTA = -3.0f;          // 12 -> 9

    /** 1.7: {@code -(PI/10) * heldItemRight} with heldItemRight == 3. */
    private static final float OLD_BLOCK_ARM_PITCH = -0.9424779f;

    @Shadow @org.spongepowered.asm.mixin.Final public ModelPart head;
    @Shadow @org.spongepowered.asm.mixin.Final public ModelPart body;
    @Shadow @org.spongepowered.asm.mixin.Final public ModelPart rightArm;
    @Shadow @org.spongepowered.asm.mixin.Final public ModelPart leftArm;
    @Shadow @org.spongepowered.asm.mixin.Final public ModelPart rightLeg;
    @Shadow @org.spongepowered.asm.mixin.Final public ModelPart leftLeg;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL"))
    private void oldanimations$oldCrouchPose(HumanoidRenderState state, CallbackInfo ci) {
        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || !config.oldSneakPose || !state.isCrouching) {
            return;
        }

        // Safe to adjust at TAIL: nothing after the crouch block in setupAnim
        // writes part Y (the arm bob only touches xRot/zRot).
        this.head.y += CROUCH_HEAD_Y_DELTA;
        this.body.y += CROUCH_BODY_Y_DELTA;
        this.rightArm.y += CROUCH_ARM_Y_DELTA;
        this.leftArm.y += CROUCH_ARM_Y_DELTA;
        this.rightLeg.y += CROUCH_LEG_Y_DELTA;
        this.leftLeg.y += CROUCH_LEG_Y_DELTA;
    }

    @Inject(
            method = "poseBlockingArm(Lnet/minecraft/client/model/geom/ModelPart;Z)V",
            at = @At("HEAD"),
            cancellable = true)
    private void oldanimations$oldBlockingArm(ModelPart arm, boolean right, CallbackInfo ci) {
        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || !config.oldBlockArmPose) {
            return;
        }

        arm.xRot = arm.xRot * 0.5f + OLD_BLOCK_ARM_PITCH;
        // 1.7 zeroed arm yaw rather than yawing the arm inward and tracking the head.
        arm.yRot = 0.0f;
        ci.cancel();
    }
}
