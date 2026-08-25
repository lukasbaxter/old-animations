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

        this.list.addSmall(
                toggle("sword_blocking", config.swordBlocking, v -> config.swordBlocking = v),
                toggle("block_hit", config.blockHit, v -> config.blockHit = v));

        this.list.addSmall(
                toggle("third_person_block", config.swordBlockingThirdPerson,
                        v -> config.swordBlockingThirdPerson = v),
                toggle("block_with_axes", config.blockWithAxes, v -> config.blockWithAxes = v));

        this.list.addSmall(
                toggle("old_sneak_pose", config.oldSneakPose, v -> config.oldSneakPose = v),
                toggle("old_sneak_camera", config.oldSneakCamera,
                        v -> config.oldSneakCamera = v));

        this.list.addSmall(
                toggle("old_block_arm_pose", config.oldBlockArmPose, v -> config.oldBlockArmPose = v),
                null);

        this.list.addSmall(
                toggle("armor_hurt_tint", config.armorHurtTint, v -> config.armorHurtTint = v),
                toggle("no_health_flash", config.noHealthFlash, v -> config.noHealthFlash = v));

        this.list.addSmall(
                toggle("hide_attack_indicator", config.hideAttackIndicator,
                        v -> config.hideAttackIndicator = v),
                null);

        this.list.addSmall(
                toggle("fixed_swing_duration", config.fixedSwingDuration,
                        v -> config.fixedSwingDuration = v),
                toggle("instant_item_swap", config.instantItemSwap,
                        v -> config.instantItemSwap = v));

        this.list.addSmall(
                toggle("instant_unsneak", config.instantUnsneak,
                        v -> config.instantUnsneak = v),
                toggle("hide_arrow_trail", config.hideOwnArrowTrail,
                        v -> config.hideOwnArrowTrail = v));

        this.list.addSmall(
                toggle("block_slowdown", config.blockSlowdown,
                        v -> config.blockSlowdown = v),
                toggle("punch_while_using", config.punchWhileUsingItem,
                        v -> config.punchWhileUsingItem = v));
    }

    @Override
    public void onClose() {
        config.save();
        super.onClose();
    }
}
