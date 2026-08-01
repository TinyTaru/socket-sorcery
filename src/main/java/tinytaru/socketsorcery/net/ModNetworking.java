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
		PayloadTypeRegistry.serverboundPlay().register(ChiselC2SPayload.ID, ChiselC2SPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ActivateBangleC2SPayload.ID, ActivateBangleC2SPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EngraveFeedbackS2CPayload.ID, EngraveFeedbackS2CPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ChiselC2SPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			if (player.containerMenu instanceof EngravingTableMenu menu) {
				EngraveFeedbackS2CPayload feedback = menu.chisel(player, payload.cell(), payload.action());
				if (feedback != null) {
					ServerPlayNetworking.send(player, feedback);
				}
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
