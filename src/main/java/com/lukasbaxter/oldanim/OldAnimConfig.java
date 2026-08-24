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

    /** How the first-person sword-block pose is built. */
    public enum BlockPose {
        /**
         * The transform vanilla still uses for non-shield {@code BLOCK} items.
         * Tuned for modern item display transforms, so it looks correct out of
         * the box. This is the recommended default.
         */
        V1_8,
        /**
         * The literal constants from 1.7.10's {@code ItemRenderer.doBlockTransformations()}.
         * Closer to 1.7 on paper, but 1.7 had no per-model display transforms,
         * so it usually needs the offset/scale tuning below to look right.
         */
        V1_7
    }

    // ---- master ----------------------------------------------------------
    public boolean enabled = true;

    // ---- sword blocking / blockhit ---------------------------------------
    /** Hold use (right click) with a sword to show the old blocking pose. */
    public boolean swordBlocking = true;
    /** Also apply the pose in third person (only ever for your own player -- see README). */
    public boolean swordBlockingThirdPerson = true;
    /** Keep playing the swing animation while blocking. This is the 1.7 "blockhit". */
    public boolean blockHit = true;
    /** Which set of block-transform constants to use. */
    public BlockPose blockPose = BlockPose.V1_8;
    /** Also allow blocking with axes, tridents and other {@code #minecraft:swords}-adjacent weapons. */
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
     * Drop the camera instantly on crouch instead of easing 50% per tick.
     * This is the part of "1.7 sneak" that actually changes how the game feels.
     */
    public boolean instantSneakCamera = true;
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
    /** Force every swing to 6 ticks, ignoring per-item swing durations. */
    public boolean fixedSwingDuration = false;

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
