package tinytaru.socketsorcery.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import tinytaru.socketsorcery.menu.SocketContainer;
import tinytaru.socketsorcery.menu.SocketingBenchMenu;

/**
 * The Socketing Bench screen: an accessory slot above a row of socket slots. Socket slots beyond the
 * accessory's capacity are drawn locked.
 */
public class SocketingBenchScreen extends AbstractContainerScreen<SocketingBenchMenu> {

	private static final int SOCKET_SLOT_START = 1;

	public SocketingBenchScreen(SocketingBenchMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 184;
		this.inventoryLabelY = this.imageHeight - 94;
		this.titleLabelY = 6;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);
		g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, 0xFFFFFFFF);
		g.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.imageHeight, 0xFFFFFFFF);
		g.fill(this.leftPos + this.imageWidth - 1, this.topPos, this.leftPos + this.imageWidth,
				this.topPos + this.imageHeight, 0xFF555555);
		g.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth,
				this.topPos + this.imageHeight, 0xFF555555);

		for (Slot slot : this.menu.slots) {
			if (slot.isActive()) {
				drawSlot(g, this.leftPos + slot.x, this.topPos + slot.y, false);
			}
		}

		// Locked socket slots beyond the placed accessory's capacity.
		for (int i = 0; i < SocketContainer.MAX_SLOTS; i++) {
			if (i >= this.menu.capacity()) {
				Slot slot = this.menu.slots.get(SOCKET_SLOT_START + i);
				drawSlot(g, this.leftPos + slot.x, this.topPos + slot.y, true);
			}
		}
	}

	private static void drawSlot(GuiGraphics g, int x, int y, boolean locked) {
		if (locked) {
			g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF4A4A4A);
			g.fill(x + 3, y + 7, x + 13, y + 9, 0xFF2A2A2A); // a dash, hinting "locked"
		} else {
			g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
			g.fill(x - 1, y - 1, x + 17, y, 0xFF8B8B8B);
			g.fill(x - 1, y - 1, x, y + 17, 0xFF8B8B8B);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		super.renderLabels(g, mouseX, mouseY);
		Component status;
		if (this.menu.capacity() == 0) {
			status = Component.translatable("screen.socket-sorcery.place_accessory").withStyle(ChatFormatting.DARK_GRAY);
		} else {
			status = Component.translatable("screen.socket-sorcery.socket_count",
					this.menu.socketCount(), this.menu.capacity()).withStyle(ChatFormatting.DARK_GRAY);
		}
		g.drawString(this.font, status, 8, 40, 0xFF555555, false);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		this.renderTooltip(g, mouseX, mouseY);
	}
}
