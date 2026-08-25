package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.BlockingState;
import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Two things in the click paths.
 *
 * <h2>The use-item swing, while blocking</h2>
 * A right click that interacts with something makes {@code startUseItem} call
 * {@code player.swing(hand)}. That is fine normally, but the mod's block is
 * held on the same button, so every press while blocking played a swing arc on
 * top of the block pose -- releasing and quickly re-blocking read as the sword
 * letting go and re-blocking a second time. 1.7 never showed this because
 * blocking <em>was</em> the right click and consumed it; 26.2 still runs the
 * normal interaction underneath, which is what lets you open a door with a
 * sword out.
 *
 * <p>Only the animation is dropped. The {@code ServerboundSwingPacket} that
 * {@code LocalPlayer.swing} would have sent is sent by hand instead, so the
 * traffic is byte-identical to vanilla's and other players still see the arm
 * move. Nothing is suppressed on the wire.
 *
 * <h2>Attacking while an item is in use</h2> -- bow punching, and block-hitting with a
 * drawn bow. Off unless {@code punchWhileUsingItem} is enabled.
 *
 * <p>1.7 allowed this by simply not checking. Its {@code clickMouse} called
 * {@code thePlayer.swingItem()} with no reference to {@code isUsingItem()} at
 * all, so drawing a bow never stopped you swinging. 26.2's {@code startAttack}
 * bails out on {@code LocalPlayer.isHandsBusy()}, which is set while an item is
 * in use, so the click does nothing.
 *
 * <p>This redirects that one call, and only when the reason the hands are busy
 * is an item in use -- a boat's oars still block attacking, as they should.
 *
 * <p><strong>This one sends packets vanilla would not.</strong> Every other
 * behaviour in this mod is either drawing-only or, in the case of the blocking
 * slowdown, a self-imposed handicap. This makes attacks happen that a vanilla
 * client would have swallowed, which is exactly the shape of thing a server
 * anticheat looks for. It is genuine 1.7 behaviour and it is your call, but it
 * is off by default and it is the one setting here that could get you flagged.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Redirect(
            method = "startUseItem()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;"
                            + "swing(Lnet/minecraft/world/InteractionHand;)V"))
    private void oldanimations$noUseSwingWhileBlocking(LocalPlayer player, InteractionHand hand) {
        OldAnimConfig config = OldAnimConfig.get();
        if (config.enabled && config.swordBlocking && BlockingState.isBlocking()) {
            // Same packet vanilla would have sent, without the local animation.
            player.connection.send(new ServerboundSwingPacket(hand));
            return;
        }
        player.swing(hand);
    }

    @Redirect(
            method = "startAttack()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"))
    private boolean oldanimations$punchWhileUsingItem(LocalPlayer player) {
        OldAnimConfig config = OldAnimConfig.get();
        if (config.enabled && config.punchWhileUsingItem && player.isUsingItem()) {
            return false;
        }
        return player.isHandsBusy();
    }
}
