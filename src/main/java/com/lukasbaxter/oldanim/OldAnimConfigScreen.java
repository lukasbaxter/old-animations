package com.lukasbaxter.oldanim;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;


/**
 * In-game settings, opened with the "Open Config" key (O by default).
 *
 * <p>Only the toggles live here. The numeric fine-tuning for the block pose
 * ({@code blockOffsetX/Y/Z}, {@code blockScale}) is edited in
 * {@code config/oldanimations.json} -- it is rarely touched and would bury the
 * toggles that matter.
 */
public final class OldAnimConfigScreen extends OptionsSubScreen {

    private final OldAnimConfig config = OldAnimConfig.get();

    public OldAnimConfigScreen(@Nullable Screen lastScreen, Options options) {
        super(lastScreen, options, Component.translatable("options.oldanimations.title"));
    }

    /**
     * Crouch camera depth, as a percentage of 26.2's own 0.35 drop. 1.7 dropped
     * 0.08, which is 23%, so the slider runs from "camera does not move" through
     * 1.7 to "26.2 as shipped" and the label shows the resulting drop in blocks.
     */
    private static final float VANILLA_CROUCH_DROP = 0.35f;

    private OptionInstance<Integer> sneakDepthSlider() {
        return new OptionInstance<>(
                "options.oldanimations.sneak_camera_drop",
                OptionInstance.cachedConstantTooltip(
                        Component.translatable("options.oldanimations.sneak_camera_drop.tooltip")),
                (caption, value) -> Component.translatable(
                        "options.oldanimations.sneak_camera_drop.value",
                        String.format("%.2f", value / 100.0f * VANILLA_CROUCH_DROP)),
                new OptionInstance.IntRange(0, 100),
                Math.round(config.sneakCameraDrop * 100.0f),
                value -> config.sneakCameraDrop = value / 100.0f);
    }

    private OptionInstance<Integer> blockTransitionSlider() {
        return new OptionInstance<>(
                "options.oldanimations.block_transition",
                OptionInstance.cachedConstantTooltip(
                        Component.translatable("options.oldanimations.block_transition.tooltip")),
                (caption, value) -> value <= 1
                        ? Component.translatable("options.oldanimations.block_transition.instant")
                        : Component.translatable("options.oldanimations.block_transition.value", value),
                new OptionInstance.IntRange(1, 6),
                Math.max(1, Math.round(config.blockTransitionTicks)),
                value -> config.blockTransitionTicks = value.floatValue());
    }

    private static Component header(String key) {
        return Component.translatable("options.oldanimations.category." + key);
    }

    private static OptionInstance<Boolean> toggle(
            String key, boolean initial, java.util.function.Consumer<Boolean> setter) {
        return OptionInstance.createBoolean(
                "options.oldanimations." + key,
                OptionInstance.cachedConstantTooltip(
                        Component.translatable("options.oldanimations." + key + ".tooltip")),
                initial,
                setter::accept);
    }

    @Override
    protected void addOptions() {
        if (this.list == null) {
            return;
        }

        this.list.addBig(toggle("enabled", config.enabled, v -> config.enabled = v));

        // ---- what 1.7 did that 26.2 does not -----------------------------
        this.list.addHeader(header("sword_blocking"));
        this.list.addSmall(
                toggle("sword_blocking", config.swordBlocking, v -> config.swordBlocking = v),
                toggle("block_hit", config.blockHit, v -> config.blockHit = v));
        this.list.addSmall(
                toggle("third_person_block", config.swordBlockingThirdPerson,
                        v -> config.swordBlockingThirdPerson = v),
                toggle("block_with_axes", config.blockWithAxes, v -> config.blockWithAxes = v));
        this.list.addSmall(
                toggle("block_mining_swing", config.blockMiningSwing,
                        v -> config.blockMiningSwing = v),
                null);
        this.list.addBig(this.blockTransitionSlider());

        this.list.addHeader(header("sneaking"));
        this.list.addSmall(
                toggle("old_sneak_pose", config.oldSneakPose, v -> config.oldSneakPose = v),
                toggle("old_sneak_camera", config.oldSneakCamera,
                        v -> config.oldSneakCamera = v));
        this.list.addSmall(
                toggle("instant_unsneak", config.instantUnsneak,
                        v -> config.instantUnsneak = v),
                toggle("old_block_arm_pose", config.oldBlockArmPose,
                        v -> config.oldBlockArmPose = v));
        this.list.addBig(this.sneakDepthSlider());

        this.list.addHeader(header("feedback"));
        this.list.addSmall(
                toggle("armor_hurt_tint", config.armorHurtTint, v -> config.armorHurtTint = v),
                toggle("no_health_flash", config.noHealthFlash, v -> config.noHealthFlash = v));
        this.list.addSmall(
                toggle("hide_sweep_attack", config.hideSweepAttack,
                        v -> config.hideSweepAttack = v),
                toggle("hide_attack_indicator", config.hideAttackIndicator,
                        v -> config.hideAttackIndicator = v));
        this.list.addSmall(
                toggle("hide_arrow_trail", config.hideOwnArrowTrail,
                        v -> config.hideOwnArrowTrail = v),
                toggle("predict_crits", config.predictCrits,
                        v -> config.predictCrits = v));
        this.list.addSmall(
                toggle("hide_eat_finish", config.hideEatFinishParticles,
                        v -> config.hideEatFinishParticles = v),
                null);

        // ---- preferences, not restorations -------------------------------
        this.list.addHeader(header("preferences"));
        this.list.addSmall(
                toggle("instant_item_swap", config.instantItemSwap,
                        v -> config.instantItemSwap = v),
                toggle("fixed_swing_duration", config.fixedSwingDuration,
                        v -> config.fixedSwingDuration = v));
        this.list.addSmall(
                toggle("no_view_bobbing", config.noViewBobbing,
                        v -> config.noViewBobbing = v),
                toggle("no_fov_effects", config.noFovEffects,
                        v -> config.noFovEffects = v));

        // ---- the two that are not visual-only ----------------------------
        this.list.addHeader(header("gameplay"));
        this.list.addSmall(
                toggle("block_slowdown", config.blockSlowdown,
                        v -> config.blockSlowdown = v),
                toggle("punch_while_using", config.punchWhileUsingItem,
                        v -> config.punchWhileUsingItem = v));
        this.list.addSmall(
                toggle("swing_while_using", config.swingWhileUsingItem,
                        v -> config.swingWhileUsingItem = v),
                null);

        this.list.addHeader(header("diagnostics"));
        this.list.addSmall(
                toggle("debug_readout", config.debugReadout,
                        v -> config.debugReadout = v),
                null);
    }

    @Override
    public void onClose() {
        config.save();
        super.onClose();
    }
}
