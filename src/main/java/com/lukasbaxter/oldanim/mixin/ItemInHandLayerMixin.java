package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.BlockingState;
import com.lukasbaxter.oldanim.OldAnimConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The third-person half of the block pose: rotating the <em>item</em> so the
 * sword lies across the chest instead of pointing up along the raised arm.
 *
 * <h2>Why this is a separate thing from the arm</h2>
 * 1.7 and 1.8 produced the same-looking block two different ways. 1.8 yawed the
 * arm 30 degrees inward and let the sword follow it. 1.7 left the arm yaw at
 * zero -- {@code ModelBiped} only did {@code rotateAngleX * 0.5 - (PI/10) * 3}
 * -- and instead rotated the held item inside
 * {@code RenderPlayer.renderEquippedItems}:
 * <pre>
 * translate(0.05, 0, -0.1); Ry(-50); Rx(-10); Rz(-60)
 * </pre>
 * {@link HumanoidModelMixin} already restores the 1.7 arm (no yaw). Without the
 * item half as well, the sword just points straight up.
 *
 * <h2>Folding it into 26.2</h2>
 * Those numbers cannot be pasted in. In 1.7 they sat immediately after
 * {@code bipedRightArm.postRender()} plus {@code translate(-0.0625, 0.4375,
 * 0.0625)}, and in front of {@code translate(0, 0.1875, 0)}, {@code scale(0.625,
 * -0.625, 0.625)}, {@code Rx(-100)}, {@code Ry(45)}. 26.2 reaches the item by a
 * completely different route -- {@code translateToHand}, {@code Rx(-90)},
 * {@code Ry(180)}, {@code translate(1/16, 2/16, -10/16)} -- and then applies the
 * model's own {@code thirdperson_righthand} display transform, which 1.7 had no
 * concept of.
 *
 * <p>So the constants below are the 1.7 insert re-expressed at the frame 26.2
 * hands us. Writing the 1.7 insert as {@code D} and the arm-to-insert step as
 * {@code A17 = translate(-0.0625, 0.4375, 0.0625)}, the motion the block gives
 * the sword <em>in arm-bone space</em> is {@code A17 * D * A17^-1}. Conjugating
 * that through 26.2's own arm-to-item step {@code A26 = Rx(-90) * Ry(180) *
 * translate(1/16, 2/16, -10/16)} gives what to apply here:
 * <pre>
 * X = A26^-1 * A17 * D * A17^-1 * A26
 * </pre>
 * which comes out as the translation and 80.4557-degree rotation below. Because
 * this is a conjugation, the sword moves relative to the arm by exactly what it
 * moved by in 1.7, and it does so without depending on the display transform --
 * everything downstream of the injection point cancels out of the arithmetic.
 *
 * <p>The pivot is the 1.7 arm frame, not the item's own origin, which is why the
 * translation is far larger than the {@code (0.05, 0, -0.1)} nudge it came from.
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    /** X, right arm. Mirrored for a left-handed main arm. */
    private static final float BLOCK_X = -0.30829775f;
    private static final float BLOCK_Y = -0.08273418f;
    private static final float BLOCK_Z = 0.12773331f;

    /** 80.4557 degrees about (-0.220024, 0.748384, 0.625708). */
    private static final Quaternionf BLOCK_ROT_RIGHT =
            new Quaternionf(-0.14209775f, 0.48332835f, 0.40410038f, 0.76348204f);
    /** The same rotation mirrored across the YZ plane. */
    private static final Quaternionf BLOCK_ROT_LEFT =
            new Quaternionf(-0.14209775f, -0.48332835f, -0.40410038f, 0.76348204f);

    @Inject(
            method = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;"
                    + "Lnet/minecraft/client/renderer/item/ItemStackRenderState;"
                    + "Lnet/minecraft/world/item/ItemStack;"
                    + "Lnet/minecraft/world/entity/HumanoidArm;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;"
                            + "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void oldanimations$blockedItemPose(
            ArmedEntityRenderState state,
            ItemStackRenderState itemState,
            ItemStack itemStack,
            HumanoidArm arm,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            CallbackInfo ci) {

        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || !config.swordBlocking || !config.swordBlockingThirdPerson) {
            return;
        }
        if (!BlockingState.isBlocking()) {
            return;
        }

        // ArmPose.BLOCK is also what vanilla reports for a raised shield, and a
        // shield already has its own pose. Only the items we invent a block for
        // get the 1.7 sword transform -- which also keeps this off other
        // players, since AvatarRendererMixin only ever returns BLOCK for the
        // local player's sword.
        HumanoidModel.ArmPose pose =
                arm == HumanoidArm.RIGHT ? state.rightArmPose : state.leftArmPose;
        if (pose != HumanoidModel.ArmPose.BLOCK || !BlockingState.isBlockableItem(itemStack)) {
            return;
        }

        boolean left = arm == HumanoidArm.LEFT;
        poseStack.translate(left ? -BLOCK_X : BLOCK_X, BLOCK_Y, BLOCK_Z);
        poseStack.mulPose(left ? BLOCK_ROT_LEFT : BLOCK_ROT_RIGHT);
    }
}
