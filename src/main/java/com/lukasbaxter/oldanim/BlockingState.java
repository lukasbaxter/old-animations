package com.lukasbaxter.oldanim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Tracks whether the local player is "blocking" in the 1.7 sense: holding the
 * use key while a sword is in the main hand.
 *
 * <p>Swords have no use action in 26.2, so this state exists purely on this
 * client. It is derived from a key that is already down and from items the
 * client already knows about -- no packet is sent, suppressed or altered, and
 * the server's view of the player is untouched.
 *
 * <p>The state is sampled once per client tick so that the first-person and
 * third-person renderers agree within a frame.
 */
public final class BlockingState {

    private static boolean blocking;

    /** 0 = fully lowered, 1 = fully in the block pose. Eased, not switched. */
    private static float progress;
    private static float progressOld;

    private BlockingState() {
    }

    /** True if the local player should currently be drawn blocking. */
    public static boolean isBlocking() {
        return blocking;
    }

    /** Called once per client tick. */
    public static void tick(Minecraft minecraft) {
        blocking = compute(minecraft);

        progressOld = progress;
        float target = blocking ? 1.0f : 0.0f;
        float ticks = OldAnimConfig.get().blockTransitionTicks;

        if (ticks <= 1.0f) {
            // 1.7's own behaviour: the pose is on or off, with nothing between.
            progress = target;
            progressOld = target;
            return;
        }

        float step = 1.0f / ticks;
        if (progress < target) {
            progress = Math.min(target, progress + step);
        } else if (progress > target) {
            progress = Math.max(target, progress - step);
        }
    }

    /**
     * How far into the block pose the sword is, interpolated for this frame.
     *
     * <p>Off by default ({@code blockTransitionTicks = 1}), because 1.7 had no
     * transition: the pose applied the instant {@code getItemInUseCount() > 0}
     * and dropped the instant it did not. The travel you see between the hit
     * and the block in a real client is the <em>swing arc</em> winding down, not
     * a separate fade -- and that arc is a pure function of swing progress, so
     * it follows the same path every time.
     *
     * <p>Easing on top of that is what made it float: the ramp is driven by how
     * long you happened to hold the button, so with fast clicks it never
     * finishes and the sword sits at whatever intermediate angle the timing
     * landed on. Raise this above 1 if you want the fade anyway.
     */
    public static float blockProgress(InteractionHand hand, float partialTick) {
        if (hand != InteractionHand.MAIN_HAND) {
            return 0.0f;
        }
        return progressOld + (progress - progressOld) * partialTick;
    }

    private static boolean compute(Minecraft minecraft) {
        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || !config.swordBlocking) {
            return false;
        }

        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gui.screen() != null) {
            return false;
        }
        if (player.isSpectator() || player.isUsingItem() || player.isAutoSpinAttack()) {
            return false;
        }
        // A shield (or anything else with a real use action) in the off hand wins:
        // vanilla already animates that, and doubling up looks wrong.
        if (isRealUseItem(player.getOffhandItem())) {
            return false;
        }
        if (!isBlockable(player.getMainHandItem(), config)) {
            return false;
        }
        return minecraft.options.keyUse.isDown();
    }

    /** Would vanilla itself animate a use for this stack? */
    private static boolean isRealUseItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getUseAnimation() != net.minecraft.world.item.ItemUseAnimation.NONE;
    }

    /**
     * True if this is a stack the mod invents a block for (a sword, or an axe
     * when that is enabled). Shields and anything else with a real use
     * animation are excluded, so renderers can use this to tell our fake block
     * apart from one vanilla is already animating.
     */
    public static boolean isBlockableItem(ItemStack stack) {
        return isBlockable(stack, OldAnimConfig.get());
    }

    private static boolean isBlockable(ItemStack stack, OldAnimConfig config) {
        if (stack.isEmpty() || isRealUseItem(stack)) {
            return false;
        }
        if (stack.is(ItemTags.SWORDS)) {
            return true;
        }
        // Tridents deliberately absent: they have a real use animation, so the
        // isRealUseItem check above already excluded them.
        return config.blockWithAxes && stack.is(ItemTags.AXES);
    }

    /**
     * Keeps the swing animation cycling while you hold attack on a block during
     * a block -- the 1.7 "stirring" look.
     *
     * <p>26.2 only swings while something is actually being destroyed:
     * {@code Minecraft.continueAttack} calls {@code player.swing} inside the
     * branch where {@code continueDestroyBlock} returned true. On a server that
     * refuses the break, or in adventure mode, nothing is destroyed, so nothing
     * swings and the blockhit has nothing to compose onto.
     *
     * <p>This restarts the animation locally when vanilla has let it lapse. It
     * uses {@code LivingEntity.swing(hand, false)} rather than
     * {@code LocalPlayer.swing(hand)}, so no swing packet is sent -- this is a
     * local animation only, and other players see exactly what vanilla would
     * have shown them.
     */
    public static void tickMiningSwing(Minecraft minecraft) {
        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || !config.blockMiningSwing || !blocking) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gui.screen() != null) {
            return;
        }
        if (!minecraft.options.keyAttack.isDown()) {
            return;
        }
        // Only when vanilla is not already driving a break of its own -- that is
        // the whole point, and it also stops the two doubling up.
        //
        // This used to also require the crosshair to be on a block, which is why
        // it worked in singleplayer and not on a server: in singleplayer the
        // block is breakable, so the swing you saw was vanilla's own. On a server
        // that refuses the break the fallback has to carry it, and gating on the
        // hit result meant it did not fire. The cost of dropping that check is
        // that holding attack at thin air while blocking also stirs.
        if (minecraft.gameMode != null && minecraft.gameMode.isDestroying()) {
            return;
        }

        // Called every tick on purpose: LivingEntity.swing only restarts once
        // the current swing is past halfway, so this reproduces vanilla's own
        // mining cadence rather than retriggering every tick.
        player.swing(InteractionHand.MAIN_HAND, false);
    }

    /**
     * One line of state on the action bar while blocking, for working out why an
     * animation does or does not fire on a particular server.
     *
     * <pre>
     * blk  are we drawing a block at all
     * atk  is the attack key down
     * dig  is vanilla already destroying a block (its own swing would cover us)
     * swg  is a swing currently running
     * anim swing progress, 0 to 1
     * hit  what the crosshair is on
     * gm   game mode as the client understands it
     * </pre>
     */
    public static void tickDebugReadout(Minecraft minecraft) {
        OldAnimConfig config = OldAnimConfig.get();
        if (!config.enabled || !config.debugReadout) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        String hit = minecraft.hitResult == null
                ? "null"
                : minecraft.hitResult.getType().name();
        String mode = minecraft.gameMode == null
                ? "null"
                : String.valueOf(minecraft.gameMode.getPlayerMode());
        boolean digging = minecraft.gameMode != null && minecraft.gameMode.isDestroying();

        minecraft.gui.hud.setOverlayMessage(
                net.minecraft.network.chat.Component.literal(String.format(
                        "blk=%d atk=%d dig=%d swg=%d anim=%.2f hit=%s gm=%s",
                        blocking ? 1 : 0,
                        minecraft.options.keyAttack.isDown() ? 1 : 0,
                        digging ? 1 : 0,
                        player.swinging ? 1 : 0,
                        player.getAttackAnim(1.0f),
                        hit,
                        mode)),
                false);
    }

    /** True if the given hand is the one holding the blocked item. */
    public static boolean isBlockingHand(InteractionHand hand) {
        return blocking && hand == InteractionHand.MAIN_HAND;
    }
}
