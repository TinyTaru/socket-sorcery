package tinytaru.socketsorcery.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import tinytaru.socketsorcery.client.screen.EngravingTableScreen;
import tinytaru.socketsorcery.net.EngraveResult;
import tinytaru.socketsorcery.net.EngraveResultS2CPayload;

/**
 * Client-side receivers for the mod's server→client payloads. Registered from
 * {@link SocketSorceryClient}; the payload <em>types</em> are registered in common init.
 */
public final class ClientNetworking {

	public static void registerClient() {
		ClientPlayNetworking.registerGlobalReceiver(EngraveResultS2CPayload.ID, (payload, context) -> {
			EngraveResult result = payload.result();
			context.client().execute(() -> {
				Minecraft mc = context.client();
				mc.getSoundManager().play(SimpleSoundInstance.forUI(
						result.success() ? SoundEvents.PLAYER_LEVELUP : SoundEvents.VILLAGER_NO,
						result.success() ? 1.3F : 1.0F));
				if (mc.screen instanceof EngravingTableScreen screen) {
					screen.onEngraveResult(result);
				}
			});
		});
	}

	private ClientNetworking() {
	}
}
