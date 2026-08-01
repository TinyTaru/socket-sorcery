package tinytaru.socketsorcery.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import tinytaru.socketsorcery.client.screen.EngravingTableScreen;
import tinytaru.socketsorcery.net.EngraveFeedbackS2CPayload;

/**
 * Client-side receivers for the mod's server→client payloads. Registered from
 * {@link SocketSorceryClient}; the payload <em>types</em> are registered in common init.
 */
public final class ClientNetworking {

	public static void registerClient() {
		ClientPlayNetworking.registerGlobalReceiver(EngraveFeedbackS2CPayload.ID, (payload, context) ->
				context.client().execute(() -> {
					Minecraft mc = context.client();
					if (payload.patternComplete()) {
						mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.3F));
					} else if (!payload.modifiers().isEmpty()) {
						mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F));
					} else {
						return;
					}
					if (mc.screen instanceof EngravingTableScreen screen) {
						if (payload.patternComplete()) {
							screen.onPatternEngraved();
						} else {
							screen.onModifiersFormed(payload.modifiers());
						}
					}
				}));
	}

	private ClientNetworking() {
	}
}
