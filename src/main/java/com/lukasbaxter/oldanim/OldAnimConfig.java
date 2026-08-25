package com.lukasbaxter.oldanim;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Every knob the mod exposes. Plain public fields, serialised straight to
 * {@code config/oldanimations.json} with Gson (which ships inside Minecraft).
 *
 * <p>Nothing in here can affect what is sent to a server -- every value only
 * feeds the client's own renderer.
 */
public final class OldAnimConfig {

    // ---- master ----------------------------------------------------------
    public boolean enabled = true;

    // ---- sword blocking / blockhit ---------------------------------------
    /** Hold use (right click) with a sword to show the old blocking pose. */
    public boolean swordBlocking = true;
    /** Also apply the pose in third person (only ever for your own player -- see README). */
    public boolean swordBlockingThirdPerson = true;
    /** Keep playing the swing animation while blocking. This is the 1.7 "blockhit". */
    public boolean blockHit = true;
    /** Also allow the blocking pose for axes, not just swords. */
    public boolean blockWithAxes = false;

    // Fine tuning for the block pose (applied after the preset).
    public float blockOffsetX = 0.0f;
    public float blockOffsetY = 0.0f;
    public float blockOffsetZ = 0.0f;
    public float blockScale = 1.0f;

    // ---- sneak -----------------------------------------------------------
    /** 1.7 third-person crouch pose (legs up, head barely lowered, body/arms unmoved). */
    public boolean oldSneakPose = true;
    /**
     * The 1.7 crouch camera curve: instant on the way down, eased at 50% per
     * tick on the way back up. 26.2 eases in both directions; 1.8 snapped in
     * both directions. The asymmetry is the part you actually feel.
     */
    public boolean oldSneakCamera = true;
    /**
     * How much of 26.2's crouch camera drop to actually apply, as a fraction.
     *
     * <p>26.2 drops the camera from 1.62 to 1.27, a drop of <b>0.35</b>. 1.7
     * dropped it by <b>0.08</b> (1.62 to 1.54) and no more -- so 26.2 crouches
     * more than four times as deep, and any curve applied to that drop reads as
     * four times as much movement.
     *
     * <ul>
     *   <li>{@code 1.0} -- 26.2's full 0.35 drop
     *   <li>{@code 0.2286} -- 1.7's 0.08 drop (the default)
     *   <li>{@code 0.0} -- camera does not drop at all
     * </ul>
     *
     * <p><b>The cost.</b> This moves the camera, not the eye. Block and entity
     * picking originate from {@code Entity.getEyePosition}, which stays at the
     * real 1.27, so while crouched your crosshair sits
     * {@code (1 - sneakCameraDrop) * 0.35} blocks above where the ray actually
     * starts -- 0.27 at the default. Set this back to {@code 1.0} if you would
     * rather have the deep crouch and an honest reticle.
     *
     * <p>Derived from the standing and crouching eye heights at runtime, so
     * player scale attributes still behave.
     */
    public float sneakCameraDrop = 0.22857143f;

    // ---- third-person arms -----------------------------------------------
    /** 1.7 blocking arm: no -30 degree yaw and no head tracking clamp. */
    public boolean oldBlockArmPose = true;

    // ---- first-person item -----------------------------------------------
    // Note: the bow pull and the eat/drink transforms are deliberately absent.
    // Their constants in 26.2 are byte-for-byte the 1.7.10 ones, so there is
    // nothing to restore -- a toggle for them would do literally nothing.
    /**
     * Tint armour red along with its wearer when they take a hit. 26.2 only
     * reddens bare skin, so an armoured player barely flinches visually.
     */
    public boolean armorHurtTint = true;
    /** 1.7 health bar: hearts do not flash white on damage or healing. */
    public boolean noHealthFlash = true;
    /**
     * Hide the attack cooldown indicator, which 1.7 had no equivalent of.
     * Vanilla can already do this under Options > Controls > Attack Indicator;
     * this exists so the whole 1.7 preset sits behind one switch.
     */
    public boolean hideAttackIndicator = true;

    /** Force every swing to 6 ticks, ignoring per-item swing durations. */
    public boolean fixedSwingDuration = false;

    /**
     * Skip the equip animation and put a newly selected item straight in your
     * hand at full height.
     *
     * <p>Not a 1.7 restoration -- 1.7 ramped the item up at 0.4 per tick just
     * like 26.2 does. This is here because the dip is the one bit of the swap
     * you feel when switching mid-fight.
     */
    public boolean instantItemSwap = true;

    /**
     * Snap the camera back up when you release sneak instead of easing.
     *
     * <p>Not 1.7 -- 1.7's crouch camera eased on the way up over about four
     * ticks (see {@code oldSneakCamera}). On by default anyway: even at the
     * correct 60%-per-tick rate the ease is the part of the sneak that reads as
     * sluggish, and it is what most 1.8.9 PvP clients feel like. Turn it off
     * for the accurate curve.
     */
    public boolean instantUnsneak = true;

    /**
     * Slow you to 20% movement while blocking, and stop you sprinting, the way
     * 1.7 did for any item in use.
     *
     * <p><strong>Off by default, and the one setting here that is not purely
     * visual.</strong> Everything else in this mod only changes what is drawn.
     * This changes where you actually go, so your position packets differ from
     * what they would otherwise be. Nothing an anticheat objects to -- you are
     * moving slower, never faster -- but it is a real handicap, and in 26.2
     * blocking buys you no damage reduction to pay for it.
     */
    public boolean blockSlowdown = false;

    /**
     * Let you attack while an item is in use -- bow punching, and block-hitting
     * with a drawn bow.
     *
     * <p>1.7 allowed this by never checking: its {@code clickMouse} swung the
     * arm with no reference to {@code isUsingItem()}. 26.2's {@code startAttack}
     * bails on {@code isHandsBusy()} instead.
     *
     * <p><strong>Off by default, and the riskiest setting here.</strong> Unlike
     * {@code blockSlowdown}, which only handicaps you, this makes attack packets
     * happen that a vanilla client would have swallowed. That is the shape of
     * thing a server anticheat looks for. Genuine 1.7 behaviour, your call.
     */
    public boolean punchWhileUsingItem = false;

    /**
     * Drop the crit particle trail from arrows you fired yourself.
     *
     * <p>A preference rather than a restoration: 1.7 spawned the same trail.
     * Other players' arrows keep theirs, so a fully drawn shot coming at you
     * still reads as one.
     */
    public boolean hideOwnArrowTrail = true;

    /**
     * Keep the swing animation cycling while you hold attack on a block during
     * a block, even when nothing is actually being destroyed.
     *
     * <p>26.2 only swings while a break is progressing, so on a server that
     * refuses the break -- or in adventure mode -- the blockhit has nothing to
     * compose onto and the sword sits still. Local animation only: no swing
     * packet is sent, so other players see exactly what vanilla would show.
     */
    public boolean blockMiningSwing = true;

    /**
     * Ticks the sword takes to travel between the normal pose and the block
     * pose, in either direction.
     *
     * <p>{@code 1} means no transition at all, which is what 1.7 did: the pose
     * applied the instant {@code getItemInUseCount() > 0} and dropped the
     * instant it did not.
     *
     * <p>Default since v1.10.0, because a fade is the wrong mechanism for this.
     * The travel you see between the hit and the block in a real client is the
     * swing arc winding down, and that arc is a pure function of swing progress
     * -- the same path every time. A fade is driven by how long you happened to
     * hold the button instead, so with fast clicks it never finishes and the
     * sword floats at whatever angle the timing landed on. Raise it above 1 if
     * you want the fade anyway.
     */
    public float blockTransitionTicks = 2.0f;

    /**
     * Spawn crit particles locally whenever a hit meets 1.7/1.8's crit condition.
     *
     * <p>The client half is unchanged in 26.2 -- it spawns crit particles the
     * same way 1.8 did. What changed is who decides: 1.9 combat gated crits
     * behind the attack cooldown, so a 26.2 server sends no crit packet unless
     * the cooldown is essentially full. That is server-side.
     *
     * <p><strong>Off by default because the particles can lie.</strong> They say
     * "1.8 would have critted this", not "this hit dealt crit damage". If the
     * server does crit as well you get both emitters. Nothing is sent and
     * nothing is suppressed; the particles exist only on this client.
     */
    public boolean predictCrits = false;

    /**
     * Drop the sweep attack arc.
     *
     * <p>A real restoration: the sweep is a 1.9 addition, and it is why
     * spam-clicking in 26.2 looks like the game is choosing between several
     * swings. The arm is unchanged from 1.7 -- same 6 tick duration from
     * {@code SwingAnimation.DEFAULT}, same haste and fatigue adjustment, same
     * "only restart past halfway" rule -- but a full-cooldown hit throws a large
     * white arc and a partial one does not, so an irregular click rate reads as
     * an irregular animation.
     */
    public boolean hideSweepAttack = true;

    /**
     * Switch off view bobbing.
     *
     * <p>Not a restoration -- 26.2's bob is byte-for-byte 1.7's. It is here
     * because 26.2 stopped surfacing the vanilla View Bobbing option where you
     * can reach it.
     */
    public boolean noViewBobbing = true;

    /**
     * Stop the field of view moving when your speed does: sprinting, speed
     * potions and flying all leave it alone.
     *
     * <p>Also not a restoration, and here for the same reason -- 26.2 stopped
     * surfacing the vanilla FOV Effects slider.
     */
    public boolean noFovEffects = true;

    /**
     * How many ticks the red damage tint stays up. Vanilla is 10, and so was
     * 1.7's, so anything above 10 is a preference rather than a restoration.
     *
     * <p>What actually thinned the tint out between the eras is the attack
     * cooldown: you land fewer hits, so it fires less often even though each
     * flash is the same length. Stretching each flash is the part of that a
     * client can do something about. Set to {@code 10} for vanilla.
     */
    public int hurtTintTicks = 20;

    /**
     * Show a one-line state readout on the action bar while you are blocking.
     *
     * <p>Purely a diagnostic. It exists because "the stirring animation works in
     * singleplayer but not on this server" cannot be answered by reading the
     * source -- the interesting values are the ones the server puts you in.
     */
    public boolean debugReadout = false;

    /**
     * Keep the arm swinging while an item is in use -- mid-bow-draw, mid-gapple.
     *
     * <p>A restoration: 1.7 applied the swing rotations unconditionally, and only
     * the positional bob sat in the branch that using an item skipped. 26.2 skips
     * the swing outright.
     *
     * <p>Does nothing on its own. Something still has to make you swing while
     * using an item, which is {@code punchWhileUsingItem}.
     */
    public boolean swingWhileUsingItem = true;

    /**
     * Drop the burst of food particles that fires the moment you finish eating.
     *
     * <p>Not a 26.2 quirk, whatever it looks like: 1.7 did exactly the same, a
     * 16-particle burst from {@code onItemUseFinish}. It reads as food still
     * falling out of your mouth after the animation ends because the burst is
     * spawned at your eye and then left behind as you keep moving. Off by
     * default because it is faithful; turn it on if you would rather not see it.
     */
    public boolean hideEatFinishParticles = false;

    // ---- plumbing --------------------------------------------------------

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("oldanimations.json");

    private static OldAnimConfig instance;

    public static OldAnimConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static OldAnimConfig load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                OldAnimConfig loaded = GSON.fromJson(reader, OldAnimConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (IOException | RuntimeException e) {
                OldAnimations.LOGGER.warn("Could not read {}, falling back to defaults", PATH, e);
            }
        }
        OldAnimConfig fresh = new OldAnimConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            OldAnimations.LOGGER.warn("Could not write {}", PATH, e);
        }
    }
}
