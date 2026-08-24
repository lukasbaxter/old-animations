package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.BlockingState;
import com.lukasbaxter.oldanim.OldAnimConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First-person sword blocking, and the 1.7 "blockhit" -- the swing arc still
 * playing on top of the block pose.
 *
 * <p>Rather than injecting into the middle of vanilla's use-animation switch
 * (which is brittle), this takes over the whole arm-with-item pass for the one
 * case vanilla has no branch for: a sword held in a block. Everything else
 * falls straight through to vanilla.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow
    public abstract void renderItem(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light);

    // Both of these are private in vanilla, so they are shadowed with a body
    // (Java has no `private abstract`); the body is never executed.
    @Shadow
    private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight) {
        throw new AssertionError("mixin shadow");
    }

    @Shadow
    private void swingArm(float attack, PoseStack poseStack, int invert, HumanoidArm arm) {
        throw new AssertionError("mixin shadow");
    }

    @Inject(
            method = "submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void oldanimations$blockingArm(
            AbstractClientPlayer player,
            float frameInterp,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            CallbackInfo ci) {

        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || !config.swordBlocking) {
            return;
        }
        if (!BlockingState.isBlockingHand(hand) || itemStack.isEmpty()) {
            return;
        }
        // Vanilla draws no arm at all while scoping; keep that.
        if (player.isScoping()) {
            ci.cancel();
            return;
        }

        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;

        poseStack.pushPose();

        // Base hand placement, including the equip/swap drop.
        this.applyItemArmTransform(poseStack, arm, inverseArmHeight);

        // The 1.7 blockhit: the swing arc is composed on top of the block pose
        // instead of replacing it, which is what makes a blockhit read as a hit.
        if (config.blockHit && attack > 0.0f) {
            this.swingArm(attack, poseStack, invert, arm);
        }

        applyBlockPose(poseStack, config, invert);

        this.renderItem(
                player,
                itemStack,
                arm == HumanoidArm.RIGHT
                        ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                poseStack,
                collector,
                light);

        poseStack.popPose();
        ci.cancel();
    }

    /**
     * The 1.7/1.8 sword block pose, expressed exactly for the 26.2 pipeline.
     *
     * <p>1.7 and 1.8 used the same block transform:
     * {@code translate(-0.5, 0.2, 0); Ry(30); Rx(-80); Ry(60)}, applied after
     * {@code transformFirstPersonItem}'s {@code Ry(45)} and {@code scale(0.4)},
     * and in front of a {@code firstperson} display transform of
     * {@code Ry(-135) Rz(25)}.
     *
     * <p>26.2 moved the {@code Ry(45)} and the {@code 0.4} into the item model's
     * display transform, which is now {@code Ry(-90) Rz(25)} with {@code scale 0.68}.
     * Folding the old chain through that change gives:
     *
     * <pre>
     * translate: (-0.5, 0.2, 0) * 0.4 through Ry(45) = (-0.14142136, 0.08, 0.14142136)
     * rotation:  Ry(45)*Ry(30)*Rx(-80)*Ry(60)*Ry(-135) = Ry(75)*Rx(-80)*Ry(-75)
     *            and cancelling the new display Ry(-90) leaves Ry(75)*Rx(-80)*Ry(15)
     * </pre>
     *
     * <p>Verified numerically: this reproduces the true 1.7/1.8 orientation to
     * 0.0000 degrees. Vanilla's own BLOCK branch rounds the same rotation into
     * Euler angles and lands 0.329 degrees off, so this is very slightly closer
     * to the original than vanilla's constants are.
     *
     * <p>Dropping the raw 1.7 numbers into 26.2 without folding in the {@code Ry(45)}
     * and the scale, which this mod did before v1.2.0, is wrong by 57 degrees.
     */
    private static void applyBlockPose(PoseStack poseStack, OldAnimConfig config, int invert) {
        poseStack.translate(invert * -0.14142136f, 0.08f, 0.14142136f);
        poseStack.mulPose(Axis.YP.rotationDegrees(invert * 75.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(-80.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(invert * 15.0f));

        if (config.blockOffsetX != 0.0f || config.blockOffsetY != 0.0f || config.blockOffsetZ != 0.0f) {
            poseStack.translate(invert * config.blockOffsetX, config.blockOffsetY, config.blockOffsetZ);
        }
        if (config.blockScale != 1.0f) {
            poseStack.scale(config.blockScale, config.blockScale, config.blockScale);
        }
    }
}
