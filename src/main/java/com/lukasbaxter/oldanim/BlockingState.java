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

    /**
     * Why {@link #compute} last said no, for the debug readout. When something
     * behaves on one server and not another, the useful question is which gate
     * failed, and that is not answerable by reading the source.
     */
    private static String reason = "start";

    /**
     * Set when a right click was consumed by a block interaction -- opening a
     * chest, flipping a lever -- and cleared when the key comes back up. 1.7's
     * block was the fallback for a right click nothing else wanted, so a click
     * that opened something should not also raise the sword.
     */
    private static boolean useConsumed;

    /** Rolling count of blocking on/off transitions, for spotting a flicker. */
    private static final boolean[] RECENT = new boolean[20];
    private static int recentIndex;

    /** 0 = fully lowered, 1 = fully in the block pose. Eased, not switched. */
    private static float progress;
    private static float progressOld;

    private BlockingState() {
    }

    /** True if the local player should currently be drawn blocking. */
    public static boolean isBlocking() {
        return blocking;
    }

    /** A right click was taken by a block interaction; do not block on it. */
    public static void interactionConsumedClick() {
        useConsumed = true;
    }

    /** Called once per client tick. */
    public static void tick(Minecraft minecraft) {
        if (!minecraft.options.keyUse.isDown()) {
            useConsumed = false;
        }
        blocking = compute(minecraft);

        RECENT[recentIndex] = blocking;
        recentIndex = (recentIndex + 1) % RECENT.length;

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
            reason = "disabled";
            return false;
        }

        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gui.screen() != null) {
            reason = "no-world";
            return false;
        }
        if (player.isSpectator() || player.isAutoSpinAttack()) {
            reason = player.isSpectator() ? "spectator" : "spin-attack";
            return false;
        }
        // Deliberately NOT gated on player.isUsingItem().
        //
        // LocalPlayer.onSyncedDataUpdated will call startUsingItem() whenever the
        // server's LIVING_ENTITY_FLAGS say so, so the server can put your own
        // client into "using item" state at will -- and some do. Bailing out on
        // that made the block pose flicker in and out on those servers while
        // working perfectly in singleplayer, and took the mining stir and the
        // block transition down with it, since both hang off this flag.
        //
        // The check was only ever standing in for "is something else already
        // animating this hand", and the two isRealUseItem tests below answer that
        // precisely: a sword has no use animation of its own, so a use state
        // attached to one is not something vanilla is drawing.
        // A shield (or anything else with a real use action) in the off hand wins:
        // vanilla already animates that, and doubling up looks wrong.
        if (isRealUseItem(player.getOffhandItem())) {
            reason = "offhand-use";
            return false;
        }
        if (!isBlockable(player.getMainHandItem(), config)) {
            ItemStack main = player.getMainHandItem();
            reason = main.isEmpty() ? "empty-hand"
                    : isRealUseItemIgnoringBlock(main) ? "main-has-use:" + main.getUseAnimation()
                    : "not-a-sword";
            return false;
        }
        if (!minecraft.options.keyUse.isDown()) {
            reason = "use-key-up";
            return false;
        }
        if (useConsumed) {
            reason = "interaction";
            return false;
        }
        reason = "ok";
        return true;
    }

    /**
     * Would vanilla itself animate a use for this stack that is not a block?
     *
     * <p>{@code BLOCK} is deliberately not counted. A server can hand you a
     * sword carrying that use animation to emulate 1.8 blocking on its own side,
     * and Hypixel does exactly that. Treating it as "vanilla has this covered"
     * was wrong twice over: vanilla's version is driven by the server's use
     * state, so it flickers with the round trip, and it is not the 1.7 pose. The
     * mod owns blocking, so it should take those items rather than stand aside.
     *
     * <p>Only the main hand relaxes this -- see {@link #isBlockable}, which also
     * demands a sword or an axe, so a shield (also {@code BLOCK}) cannot slip
     * through. In the off hand a shield still wins outright.
     */
    private static boolean isRealUseItem(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getUseAnimation() != net.minecraft.world.item.ItemUseAnimation.NONE;
    }

    /** As above, but tolerating a server-applied BLOCK animation on your weapon. */
    private static boolean isRealUseItemIgnoringBlock(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        net.minecraft.world.item.ItemUseAnimation animation = stack.getUseAnimation();
        return animation != net.minecraft.world.item.ItemUseAnimation.NONE
                && animation != net.minecraft.world.item.ItemUseAnimation.BLOCK;
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
        if (stack.isEmpty() || isRealUseItemIgnoringBlock(stack)) {
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
        if (!config.enabled || !config.blockMiningSwing) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gui.screen() != null) {
            return;
        }
        // Either a block we are drawing, or a real item use that the attack gate
        // has been opened for. 26.2's continueAttack bails outright on
        // isUsingItem(), so a bow draw or a gapple gets no swing from vanilla at
        // all and the stir has to come from here.
        boolean usingWithAttacksAllowed = config.punchWhileUsingItem && player.isUsingItem();
        if (!blocking && !usingWithAttacksAllowed) {
            return;
        }
        if (!minecraft.options.keyAttack.isDown()) {
            return;
        }
        // Only when vanilla is not already driving a break of its own -- that is
        // the whole point, and it also stops the two doubling up.
        if (minecraft.gameMode != null && minecraft.gameMode.isDestroying()) {
            return;
        }
        // And only while actually aimed at a block in reach. Swinging at open air
        // is not mining, so it should not stir.
        //
        // This gate was dropped in v1.11.0 while chasing the stir not firing on
        // Hypixel, on the theory that it was the thing blocking it. It was not --
        // that was `blocking` being false, fixed in v1.15.0 -- so it is back.
        if (minecraft.hitResult == null
                || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
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
     * flips how many times blocking flipped in the last second (0 = steady)
     * why  which gate said no, when blk is 0
     * use  does the client think an item is in use (the server can set this)
     * punch is Attack While Using An Item switched on
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
                        "blk=%d flips=%d why=%s use=%d punch=%d atk=%d dig=%d swg=%d anim=%.2f hit=%s gm=%s",
                        blocking ? 1 : 0,
                        recentFlips(),
                        reason,
                        player.isUsingItem() ? 1 : 0,
                        config.punchWhileUsingItem ? 1 : 0,
                        minecraft.options.keyAttack.isDown() ? 1 : 0,
                        digging ? 1 : 0,
                        player.swinging ? 1 : 0,
                        player.getAttackAnim(1.0f),
                        hit,
                        mode)),
                false);
    }

    /**
     * How many times blocking flipped on or off in the last second. Steady
     * blocking is 0; anything else is the flicker, and the number says how fast.
     */
    private static int recentFlips() {
        int flips = 0;
        for (int i = 1; i < RECENT.length; i++) {
            int a = (recentIndex + i - 1) % RECENT.length;
            int b = (recentIndex + i) % RECENT.length;
            if (RECENT[a] != RECENT[b]) {
                flips++;
            }
        }
        return flips;
    }

    /** True if the given hand is the one holding the blocked item. */
    public static boolean isBlockingHand(InteractionHand hand) {
        return blocking && hand == InteractionHand.MAIN_HAND;
    }
}
