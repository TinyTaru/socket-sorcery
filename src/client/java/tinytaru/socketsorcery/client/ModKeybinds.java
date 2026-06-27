package tinytaru.socketsorcery.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import tinytaru.socketsorcery.net.ActivateBangleC2SPayload;
import tinytaru.socketsorcery.registry.ModItems;

/** Client keybinds. The Activate Bangle key fires every socketed bangle gem's active behaviour. */
public final class ModKeybinds {

	public static KeyMapping activateBangle;

	public static void register() {
		activateBangle = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.socket-sorcery.activate_bangle",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_R,
				"key.category.socket-sorcery"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (activateBangle.consumeClick()) {
				if (client.player == null) {
					continue;
				}
				if (client.player.getCooldowns().isOnCooldown(ModItems.BANGLE)) {
					client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0F));
				} else {
					ClientPlayNetworking.send(ActivateBangleC2SPayload.INSTANCE);
				}
			}
		});
	}

	private ModKeybinds() {
	}
}
