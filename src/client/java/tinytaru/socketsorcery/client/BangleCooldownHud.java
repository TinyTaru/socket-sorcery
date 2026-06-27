package tinytaru.socketsorcery.client;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.item.BangleItem;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * Draws a small bangle cooldown indicator near the hotbar while the bangle ability is recharging
 * (a worn bangle gets no vanilla cooldown overlay). The icon is the worn bangle — rendered through
 * {@link AccessoryItemRenderer}, so it shows its gems — with a vanilla-style draining sweep.
 */
public final class BangleCooldownHud {

	public static void register() {
		HudRenderCallback.EVENT.register(BangleCooldownHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.options.hideGui) {
			return;
		}
		float percent = player.getCooldowns()
				.getCooldownPercent(ModItems.BANGLE, delta.getGameTimeDeltaPartialTick(true));
		if (percent <= 0.0F) {
			return;
		}

		int x = minecraft.getWindow().getGuiScaledWidth() / 2 - 91 - 20; // just left of the hotbar
		int y = minecraft.getWindow().getGuiScaledHeight() - 19;

		graphics.renderItem(wornBangle(player), x, y);

		// draining sweep on top of the item: overlay covers the bottom `percent` fraction
		int filled = Math.round(16 * percent);
		graphics.pose().pushPose();
		graphics.pose().translate(0.0F, 0.0F, 250.0F);
		graphics.fill(x, y + 16 - filled, x + 16, y + 16, 0x9AFFFFFF);
		graphics.pose().popPose();
	}

	private static ItemStack wornBangle(LocalPlayer player) {
		ItemStack[] found = { new ItemStack(ModItems.BANGLE) };
		TrinketsApi.getTrinketComponent(player).ifPresent(component ->
				component.forEach((reference, stack) -> {
					if (stack.getItem() instanceof BangleItem) {
						found[0] = stack;
					}
				}));
		return found[0];
	}

	private BangleCooldownHud() {
	}
}
