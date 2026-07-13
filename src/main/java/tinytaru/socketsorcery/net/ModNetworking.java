package tinytaru.socketsorcery.net;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.item.AccessoryItem;
import tinytaru.socketsorcery.item.BangleItem;
import tinytaru.socketsorcery.item.Cooldowns;
import tinytaru.socketsorcery.menu.EngravingTableMenu;
import tinytaru.socketsorcery.pattern.Patterns;

/**
 * Registers the mod's custom payloads and their server-side handlers. Payload <em>types</em> are
 * registered on both sides (common init); the receivers only fire server-side.
 */
public final class ModNetworking {

	public static void registerServer() {
		PayloadTypeRegistry.playC2S().register(FinishEngravingC2SPayload.ID, FinishEngravingC2SPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ActivateBangleC2SPayload.ID, ActivateBangleC2SPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(EngraveResultS2CPayload.ID, EngraveResultS2CPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(FinishEngravingC2SPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			if (player.containerMenu instanceof EngravingTableMenu menu) {
				EngraveResult result = menu.tryEngrave(player, payload.carved(), payload.deep());
				ServerPlayNetworking.send(player, new EngraveResultS2CPayload(result.ordinal()));
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(ActivateBangleC2SPayload.ID, (payload, context) ->
				activateBangles(context.player()));
	}

	private static void activateBangles(ServerPlayer player) {
		TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
			HitResult target = Patterns.raycast(player,
					tinytaru.socketsorcery.config.SocketSorceryConfig.get().bangleReach);
			component.forEach((slot, stack) -> {
				if (stack.getItem() instanceof BangleItem && !player.getCooldowns().isOnCooldown(stack.getItem())) {
					AccessoryItem.runBangle(player, stack, target);
					int ticks = Cooldowns.forBangle(stack, player.registryAccess());
					if (ticks > 0) {
						player.getCooldowns().addCooldown(stack.getItem(), ticks);
					}
				}
			});
		});
	}

	private ModNetworking() {
	}
}
