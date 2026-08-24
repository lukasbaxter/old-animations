package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.BlockingState;
import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reports {@link HumanoidModel.ArmPose#BLOCK} for a sword the local player is
 * "blocking" with, so the third-person model poses the arm the way 1.7 did.
 *
 * <p>This only ever fires for the client's own player. Swords have no use
 * action in 26.2, so the server never tells anyone that a remote player is
 * blocking -- that information simply does not exist on the wire. Other
 * players therefore cannot be shown blocking, and pretending otherwise would
 * mean inventing state.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(
            method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("HEAD"),
            cancellable = true)
    private static void oldanimations$swordBlockPose(
            Avatar avatar,
            ItemStack itemInHand,
            InteractionHand hand,
            CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {

        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || !config.swordBlocking || !config.swordBlockingThirdPerson) {
            return;
        }
        if (hand != InteractionHand.MAIN_HAND || itemInHand.isEmpty()) {
            return;
        }
        if (avatar != Minecraft.getInstance().player || !BlockingState.isBlocking()) {
            return;
        }

        cir.setReturnValue(HumanoidModel.ArmPose.BLOCK);
    }
}
