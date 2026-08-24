package com.lukasbaxter.oldanim;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OldAnimations implements ClientModInitializer {

    public static final String MOD_ID = "oldanimations";
    public static final Logger LOGGER = LoggerFactory.getLogger("Old Animations");

    /** Renders as the lang key "key.category.oldanimations.main". */
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static KeyMapping openConfigKey;
    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        // Touch the config once at startup so a malformed file is reported now
        // rather than in the middle of the first frame.
        OldAnimConfig.get();

        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.oldanimations.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                CATEGORY));

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.oldanimations.toggle",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            BlockingState.tick(minecraft);

            while (openConfigKey.consumeClick()) {
                minecraft.gui.setScreen(new OldAnimConfigScreen(minecraft.gui.screen(), minecraft.options));
            }

            while (toggleKey.consumeClick()) {
                OldAnimConfig config = OldAnimConfig.get();
                config.enabled = !config.enabled;
                config.save();
                minecraft.gui.hud.setOverlayMessage(
                        Component.translatable(config.enabled
                                ? "text.oldanimations.toggled_on"
                                : "text.oldanimations.toggled_off"),
                        false);
            }
        });

        LOGGER.info("Old Animations ready (client-side only, nothing is sent to servers)");
    }
}
