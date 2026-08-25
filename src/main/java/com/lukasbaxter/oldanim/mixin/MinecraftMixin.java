package com.lukasbaxter.oldanim.mixin;

import com.lukasbaxter.oldanim.OldAnimConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Attacking while an item is in use -- bow punching, and block-hitting with a
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
