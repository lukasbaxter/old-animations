package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7 red armor: armour turns red along with the wearer when they take a hit.
 *
 * <p>26.2 submits every armour layer with {@link OverlayTexture#NO_OVERLAY}, so
 * only bare skin reddens and a fully-armoured player gives almost no visual
 * feedback that they were hit. 1.7 tinted the armour too, which is the cue
 * everyone reads in a fight.
 *
 * <p>The renderer's state parameter is an unbounded generic, so it is checked
 * at runtime rather than typed. Rendering is single-threaded, so stashing the
 * computed overlay between the head of the call and the field reads inside it
 * is safe.
 */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {

    private static final String RENDER_LAYERS =
            "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;"
                    + "Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;"
                    + "Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I"
                    + "Lnet/minecraft/resources/Identifier;II)V";

    @Unique
    private static int oldanimations$overlay = OverlayTexture.NO_OVERLAY;

    @Inject(method = RENDER_LAYERS, at = @At("HEAD"))
    private <S> void oldanimations$captureOverlay(
            EquipmentClientInfo.LayerType layerType,
            ResourceKey<?> equipmentAssetId,
            Model<? super S> model,
            S state,
            ItemStack itemStack,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            Identifier playerTextureOverride,
            int outlineColor,
            int order,
            CallbackInfo ci) {

        OldAnimConfig config = OldAnimConfig.get();
        oldanimations$overlay = config.enabled
                && config.armorHurtTint
                && state instanceof LivingEntityRenderState living
                ? LivingEntityRenderer.getOverlayCoords(living, 0.0f)
                : OverlayTexture.NO_OVERLAY;
    }

    @Redirect(
            method = RENDER_LAYERS,
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/texture/OverlayTexture;NO_OVERLAY:I",
                    opcode = Opcodes.GETSTATIC))
    private int oldanimations$hurtOverlay() {
        return oldanimations$overlay;
    }
}
