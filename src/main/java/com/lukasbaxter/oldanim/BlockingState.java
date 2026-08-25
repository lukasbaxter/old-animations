package com.lukasbaxter.oldanim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

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

    private BlockingState() {
    }

    /** True if the local player should currently be drawn blocking. */
    public static boolean isBlocking() {
        return blocking;
    }

    /** Called once per client tick. */
    public static void tick(Minecraft minecraft) {
        blocking = compute(minecraft);
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
        if (player == null || player.swinging || minecraft.gui.screen() != null) {
            return;
        }
        if (!minecraft.options.keyAttack.isDown()) {
            return;
        }
        // Only while actually aimed at a block, so holding attack at thin air
        // does not turn into a permanent windmill.
        if (minecraft.hitResult == null
                || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        player.swing(InteractionHand.MAIN_HAND, false);
    }

    /** True if the given hand is the one holding the blocked item. */
    public static boolean isBlockingHand(InteractionHand hand) {
        return blocking && hand == InteractionHand.MAIN_HAND;
    }
}
