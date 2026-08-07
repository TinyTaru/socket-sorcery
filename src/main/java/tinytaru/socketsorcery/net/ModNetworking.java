package tinytaru.socketsorcery.net;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.item.AccessoryItem;
import tinytaru.socketsorcery.item.BangleItem;
import tinytaru.socketsorcery.item.BlankScrollItem;
import tinytaru.socketsorcery.item.ScrollItem;
import tinytaru.socketsorcery.component.ScrollDrawingData;
import tinytaru.socketsorcery.registry.ModComponents;
import tinytaru.socketsorcery.registry.ModItems;
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
		PayloadTypeRegistry.serverboundPlay().register(TranscribeCellC2SPayload.ID, TranscribeCellC2SPayload.CODEC);
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

		ServerPlayNetworking.registerGlobalReceiver(TranscribeCellC2SPayload.ID, (payload, context) ->
				paintScroll(context.player(), payload));
	}

	private static void paintScroll(ServerPlayer player, TranscribeCellC2SPayload payload) {
		int cell = payload.cell();
		if (cell < 0 || cell >= tinytaru.socketsorcery.pattern.Pattern.GRID * tinytaru.socketsorcery.pattern.Pattern.GRID) return;
		ItemStack stack = player.getItemInHand(payload.hand());
		if (!(stack.getItem() instanceof BlankScrollItem)) return;
		// Components belong to a whole stack. Pull one sheet out before it receives its first ink mark,
		// so the untouched sheets remain pristine (and the completed result is only one scroll).
		if (stack.getCount() > 1 && !stack.has(ModComponents.SCROLL_DRAWING)) {
			ItemStack drawingStack = stack.split(1);
			drawingStack.set(ModComponents.SCROLL_DRAWING, ScrollDrawingData.EMPTY);
			player.setItemInHand(payload.hand(), drawingStack);
			player.getInventory().placeItemBackInInventory(stack);
			stack = drawingStack;
		}
		ScrollDrawingData drawing = stack.getOrDefault(ModComponents.SCROLL_DRAWING, ScrollDrawingData.EMPTY);
		if (drawing.isPainted(cell) || !removeOneInk(player)) return;
		drawing = drawing.paint(cell);
		ScrollDrawingData completedDrawing = drawing;
		var match = tinytaru.socketsorcery.pattern.Patterns.all(player.registryAccess())
				.filter(holder -> holder.value().matchesCarved(completedDrawing.painted()))
				.findFirst().orElse(null);
		if (match == null || match.value().scroll().isEmpty()) {
			stack.set(ModComponents.SCROLL_DRAWING, drawing);
			return;
		}
		var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(match.value().scroll().get()).orElse(null);
		if (!(item instanceof ScrollItem)) {
			stack.set(ModComponents.SCROLL_DRAWING, drawing);
			return;
		}
		ItemStack transcribed = new ItemStack(item);
		transcribed.set(ModComponents.TRANSCRIBED_SCROLL, true);
		player.setItemInHand(payload.hand(), transcribed);
	}

	private static boolean removeOneInk(ServerPlayer player) {
		if (player.getAbilities().instabuild) return true;
		for (ItemStack inventoryStack : player.getInventory().getNonEquipmentItems()) {
			if (inventoryStack.is(ModItems.SCROLL_INK)) {
				inventoryStack.shrink(1);
				return true;
			}
		}
		return false;
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
