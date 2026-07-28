package tinytaru.socketsorcery.net;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
		PayloadTypeRegistry.serverboundPlay().register(FinishEngravingC2SPayload.ID, FinishEngravingC2SPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ActivateBangleC2SPayload.ID, ActivateBangleC2SPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EngraveResultS2CPayload.ID, EngraveResultS2CPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(FinishEngravingC2SPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			if (player.containerMenu instanceof EngravingTableMenu menu) {
				EngraveResult result = menu.tryEngrave(player, payload.carved(), payload.deep(), payload.downgrades());
				ServerPlayNetworking.send(player, new EngraveResultS2CPayload(result.ordinal()));
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(ActivateBangleC2SPayload.ID, (payload, context) ->
				activateBangles(context.player()));
	}

	private static void activateBangles(ServerPlayer player) {
		HitResult target = Patterns.raycast(player,
				tinytaru.socketsorcery.config.SocketSorceryConfig.get().bangleReach);
		for (TrinketSlotAccess slot : TrinketsApi.getAttachment(player)
				.equipped(stack -> stack.getItem() instanceof BangleItem, false)) {
			ItemStack stack = slot.get();
			if (player.getCooldowns().isOnCooldown(stack)) {
				continue;
			}
			AccessoryItem.runBangle(player, stack, target);
			int ticks = Cooldowns.forBangle(stack, player.registryAccess());
			if (ticks > 0) {
				player.getCooldowns().addCooldown(stack, ticks);
			}
		}
	}

	private ModNetworking() {
	}
}
