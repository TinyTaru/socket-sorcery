package tinytaru.socketsorcery.client;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.item.BangleItem;
import tinytaru.socketsorcery.item.Cooldowns;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * Draws a small bangle cooldown indicator near the hotbar while the bangle ability is recharging
 * (a worn bangle gets no vanilla cooldown overlay). The icon is the worn bangle — rendered through
 * {@link AccessoryItemRenderer}, so it shows its gems — with a vanilla-style draining sweep.
 */
public final class BangleCooldownHud {

	// Tracks the cooldown→ready edge for the one-shot chime + flash.
	private static boolean wasOnCooldown;
	private static long readyFlashUntil;

	public static void register() {
		HudRenderCallback.EVENT.register(BangleCooldownHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.options.hideGui) {
			return;
		}

		boolean onCooldown = player.getCooldowns().isOnCooldown(ModItems.BANGLE);
		long now = player.level().getGameTime();
		if (wasOnCooldown && !onCooldown) {
			readyFlashUntil = now + 12;
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.4F));
		}
		wasOnCooldown = onCooldown;

		int x = minecraft.getWindow().getGuiScaledWidth() / 2 - 91 - 20; // just left of the hotbar
		int y = minecraft.getWindow().getGuiScaledHeight() - 19;

		if (!onCooldown) {
			// Brief "ready" flash right after the cooldown ends, then nothing.
			if (now < readyFlashUntil) {
				graphics.renderItem(wornBangle(player), x, y);
				int a = (int) (0x80 * ((readyFlashUntil - now) / 12.0));
				graphics.fill(x - 1, y - 1, x + 17, y + 17, (a << 24) | 0x55FF55);
			}
			return;
		}

		float percent = player.getCooldowns().getCooldownPercent(ModItems.BANGLE, delta.getGameTimeDeltaPartialTick(true));
		graphics.renderItem(wornBangle(player), x, y);

		// Draining sweep on top of the item: overlay covers the bottom `percent` fraction.
		int filled = Math.round(16 * percent);
		graphics.pose().pushPose();
		graphics.pose().translate(0.0F, 0.0F, 250.0F);
		graphics.fill(x, y + 16 - filled, x + 16, y + 16, 0x9AFFFFFF);
		graphics.pose().popPose();

		// Remaining-seconds countdown, estimated from the worn bangle's total cooldown.
		int total = Cooldowns.forBangle(wornBangle(player));
		if (total > 0) {
			String text = String.format("%.1f", total * percent / 20.0F);
			int tw = minecraft.font.width(text);
			graphics.pose().pushPose();
			graphics.pose().translate(0.0F, 0.0F, 260.0F);
			graphics.drawString(minecraft.font, text, x + 8 - tw / 2, y + 4, 0xFFFFFFFF, true);
			graphics.pose().popPose();
		}
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
