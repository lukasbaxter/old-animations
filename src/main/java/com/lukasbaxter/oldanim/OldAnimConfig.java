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
     * Use 1.7's crouch eye height (1.54) instead of the modern one (1.27).
     *
     * <p>Off by default on purpose: this moves the camera only. Block and entity
     * picking still originate from the real 1.27 eye position, so while crouched
     * the crosshair sits slightly below where you are actually aiming. Turn it on
     * if you want the look, leave it off if you want your aim to match the reticle.
     */
    public boolean oldSneakEyeHeight = false;

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
     * <p>Also not 1.7: 1.7's crouch camera eased on the way up (see
     * {@code oldSneakCamera}). Turn this on if the ease still reads as too
     * heavy -- it is a bigger drop in 26.2 than it ever was in 1.7, because
     * 26.2 crouches to 1.27 where 1.7 only went to 1.54.
     */
    public boolean instantUnsneak = false;

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
