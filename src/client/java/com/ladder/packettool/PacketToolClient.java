package com.ladder.packettool;

import com.ladder.packettool.ui.PacketToolScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public final class PacketToolClient implements ClientModInitializer {

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.packettool.open",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.packettool"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // open screen
            while (openKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new PacketToolScreen());
            }
        });
    }
}
