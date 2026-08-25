package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.BlockingState;
import com.lukasbaxter.oldanim.OldAnimConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Quaternionf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
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

    @Shadow private ItemStack mainHandItem;
    @Shadow private ItemStack offHandItem;
    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;
    @Shadow private float offHandHeight;
    @Shadow private float oOffHandHeight;

    /**
     * Instant item swap.
     *
     * <p>Vanilla's {@code tick} walks the hand height toward its target by at
     * most 0.4 a tick, and only swaps which stack is drawn once that height has
     * reached the bottom -- that is the dip you see when changing slots. With
     * this on, the newly held stack becomes the drawn one immediately and the
     * height is pinned at full, so there is nothing to animate.
     *
     * <p>Left alone while the hands are busy, so vanilla can still tuck the
     * item away when it needs to.
     */
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void oldanimations$instantItemSwap(CallbackInfo ci) {
        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || !config.instantItemSwap) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isHandsBusy()) {
            return;
        }

        this.mainHandItem = player.getMainHandItem();
        this.offHandItem = player.getOffhandItem();
        this.mainHandHeight = 1.0f;
        this.oMainHandHeight = 1.0f;
        this.offHandHeight = 1.0f;
        this.oOffHandHeight = 1.0f;
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
        float blockProgress = BlockingState.blockProgress(hand, frameInterp);
        if (blockProgress <= 0.0f || itemStack.isEmpty()) {
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

        // The 1.7 blockhit.
        //
        // 26.2's swingArm() does two things: a positional bob, then
        // applyItemArmAttackTransform (which is Ry(45) + the swing rotations +
        // scale(0.4)). 1.7 applied NEITHER of those wholesale while an item was
        // in use. Its bob sat in the `else` branch that blocking skipped, and
        // the Ry(45) and the 0.4 are already folded into applyBlockPose below --
        // so calling swingArm here scaled the sword to 0.4 and yawed it another
        // 45 degrees on every click, which is what threw it off screen.
        //
        // What 1.7 did apply, unconditionally, was the swing rotations, sitting
        // between the Ry(45) and the scale:
        //
        //     Ry(45); Ry(-f*20); Rz(-g*20); Rx(-g*80); scale(0.4); blockTransform
        //
        // Since applyBlockPose is the fold of Ry(45) * scale(0.4) * block, the
        // swing goes in front of it conjugated back out of that frame:
        // Ry(45) * SWING * Ry(-45).
        if (config.blockHit && attack > 0.0f) {
            float f = Mth.sin(attack * attack * (float) Math.PI);
            float g = Mth.sin(Mth.sqrt(attack) * (float) Math.PI);
            poseStack.mulPose(Axis.YP.rotationDegrees(invert * 45.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(invert * -f * 20.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(invert * -g * 20.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(-g * 80.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(invert * -45.0f));
        }

        applyBlockPose(poseStack, config, invert, blockProgress);

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
    private static void applyBlockPose(
            PoseStack poseStack, OldAnimConfig config, int invert, float progress) {

        // Blended in rather than switched on. The pose is composed once as a
        // single rotation so it can be slerped out of the rest pose, which is
        // what makes a fast blockhit read as one sword travelling instead of
        // two swords alternating. At progress 1 this is bit-identical to
        // applying the three rotations in sequence.
        Quaternionf pose = new Quaternionf()
                .rotateY((float) Math.toRadians(invert * 75.0f))
                .rotateX((float) Math.toRadians(-80.0f))
                .rotateY((float) Math.toRadians(invert * 15.0f));

        poseStack.translate(
                invert * -0.14142136f * progress,
                0.08f * progress,
                0.14142136f * progress);
        poseStack.mulPose(progress >= 1.0f ? pose : new Quaternionf().slerp(pose, progress));

        if (config.blockOffsetX != 0.0f || config.blockOffsetY != 0.0f || config.blockOffsetZ != 0.0f) {
            poseStack.translate(invert * config.blockOffsetX, config.blockOffsetY, config.blockOffsetZ);
        }
        if (config.blockScale != 1.0f) {
            poseStack.scale(config.blockScale, config.blockScale, config.blockScale);
        }
    }
}
