package com.playerspotter;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class PlayerSpotterClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SpotterConfig.load();

		KeyBinding openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.playerspotter.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyBinding.Category.MISC));
		KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.playerspotter.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, KeyBinding.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (toggleKey.wasPressed()) {
				SpotterConfig cfg = SpotterConfig.INSTANCE;
				cfg.enabled = !cfg.enabled;
				SpotterConfig.save();
				if (mc.player != null) {
					mc.player.sendMessage(Text.literal("Player Spotter: ")
						.append(Text.literal(cfg.enabled ? "ON" : "OFF")
							.formatted(cfg.enabled ? Formatting.GREEN : Formatting.RED)), true);
				}
			}
			while (openKey.wasPressed()) {
				mc.setScreen(new SpotterScreen(mc.currentScreen));
			}
		});

		HudRenderCallback.EVENT.register(SpotterHud::render);
	}
}
