package tinytaru.socketsorcery.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.net.ActivateBangleC2SPayload;
import tinytaru.socketsorcery.registry.ModItems;

/** Client keybinds. The Activate Bangle key fires every socketed bangle gem's active behaviour. */
public final class ModKeybinds {

	/** Key categories are a registered {@link KeyMapping.Category} keyed by id now, not a bare string. */
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(SocketSorcery.id("main"));

	public static KeyMapping activateBangle;

	public static void register() {
		activateBangle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.socket-sorcery.activate_bangle",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_R,
				CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (activateBangle.consumeClick()) {
				if (client.player == null) {
					continue;
				}
				if (client.player.getCooldowns().isOnCooldown(ModItems.BANGLE.getDefaultInstance())) {
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
