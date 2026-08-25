package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns off view bobbing from inside the mod.
 *
 * <p>26.2's bob is byte-for-byte 1.7's -- same {@code sin(g * PI) * bob * 0.5}
 * translate, same 3 degree Z roll, same 5 degree X -- so this is not restoring
 * anything, it is switching off something both versions had. It exists because
 * 26.2 no longer surfaces the vanilla View Bobbing option in the video settings
 * where you can reach it.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void oldanimations$noViewBobbing(
            CameraRenderState camera, PoseStack poseStack, CallbackInfo ci) {
        OldAnimConfig config = OldAnimConfig.get();
        if (config.enabled && config.noViewBobbing) {
            ci.cancel();
        }
    }
}
