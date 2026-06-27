package tinytaru.socketsorcery.client.screen;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.item.GemItem;
import tinytaru.socketsorcery.menu.EngravingTableMenu;
import tinytaru.socketsorcery.net.FinishEngravingC2SPayload;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.pattern.Modifier;
import tinytaru.socketsorcery.pattern.Modifiers;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;

/**
 * The Engraving Table screen. The gem is shown in the background of a 16x16 grid, one cell per gem
 * pixel; the scroll's symbol is hinted faintly. Left-click chisels an opaque cell deeper (depth 1
 * then 2, capped). Depth 1 carves the base symbol; depth-2 cells form modifiers. There are no
 * modifier hints — the status line just names the modifiers you've correctly formed (learn-by-doing).
 */
public class EngravingTableScreen extends AbstractContainerScreen<EngravingTableMenu> {

	private static final int GRID = Pattern.GRID; // 16
	private static final int CELL = 8;
	private static final int GRID_X = 64;
	private static final int GRID_Y = 18;

	private final int[][] depth = new int[GRID][GRID]; // 0 = bare, 1 = carved, 2 = deep

	// Cached gem pixel data so we don't read the texture every frame.
	private Item cachedGemItem;
	private int[][] gemPixels;   // [row][col] ARGB, null if no gem loaded
	private boolean[][] gemOpaque;

	private Pattern shownPattern;
	private Button confirmButton;
	private Button resetButton;

	public EngravingTableScreen(EngravingTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 200;
		this.imageHeight = 252;
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = 19;
		this.inventoryLabelY = 158;
	}

	@Override
	protected void init() {
		super.init();
		this.confirmButton = this.addRenderableWidget(Button
				.builder(Component.translatable("screen.socket-sorcery.engrave"), b -> sendEngrave())
				.bounds(this.leftPos + 8, this.topPos + 100, 48, 18)
				.build());
		this.resetButton = this.addRenderableWidget(Button
				.builder(Component.translatable("screen.socket-sorcery.reset"), b -> clearCarving())
				.bounds(this.leftPos + 8, this.topPos + 122, 48, 18)
				.build());
		updateButtons();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		ensureGemData();
		Pattern pattern = this.menu.targetPattern();
		if (pattern != shownPattern) {
			shownPattern = pattern;
			clearCarving();
		}
		updateButtons();
	}

	private void ensureGemData() {
		Item gem = this.menu.gemStack().getItem();
		if (!(gem instanceof GemItem)) {
			gem = null;
		}
		if (gem == cachedGemItem) {
			return;
		}
		cachedGemItem = gem;
		gemPixels = null;
		gemOpaque = null;
		clearCarving();
		if (gem == null) {
			return;
		}

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(gem);
		ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
				itemId.getNamespace(), "textures/item/" + itemId.getPath() + ".png");
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
		if (resource.isEmpty()) {
			return;
		}
		try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
			if (image.getWidth() < GRID || image.getHeight() < GRID) {
				return;
			}
			int[][] pixels = new int[GRID][GRID];
			boolean[][] opaque = new boolean[GRID][GRID];
			for (int row = 0; row < GRID; row++) {
				for (int col = 0; col < GRID; col++) {
					int abgr = image.getPixelRGBA(col, row); // NativeImage packs RGBA as 0xAABBGGRR
					int a = (abgr >>> 24) & 0xFF;
					int b = (abgr >>> 16) & 0xFF;
					int g = (abgr >>> 8) & 0xFF;
					int r = abgr & 0xFF;
					opaque[row][col] = a > 16;
					pixels[row][col] = (a << 24) | (r << 16) | (g << 8) | b;
				}
			}
			gemPixels = pixels;
			gemOpaque = opaque;
		} catch (IOException ignored) {
			// leave gem data null; the grid simply shows nothing
		}
	}

	private long[] carvedBits() {
		long[] bits = GridBits.empty();
		for (int row = 0; row < GRID; row++) {
			for (int col = 0; col < GRID; col++) {
				if (depth[row][col] >= 1) {
					GridBits.set(bits, row, col);
				}
			}
		}
		return bits;
	}

	private long[] deepBits() {
		long[] bits = GridBits.empty();
		for (int row = 0; row < GRID; row++) {
			for (int col = 0; col < GRID; col++) {
				if (depth[row][col] >= 2) {
					GridBits.set(bits, row, col);
				}
			}
		}
		return bits;
	}

	/** The applied modifier set if the current carve is a valid engraving, else null. */
	private Set<ResourceLocation> validModifiers() {
		Pattern pattern = this.menu.targetPattern();
		if (pattern == null) {
			return null;
		}
		long[] carved = carvedBits();
		long[] deep = deepBits();
		Set<ResourceLocation> modifiers = Modifiers.decode(pattern, deep);
		if (modifiers == null || !GridBits.equal(carved, GridBits.or(pattern.maskBits(), deep))) {
			return null;
		}
		return modifiers;
	}

	private boolean anyCarved() {
		for (int[] row : depth) {
			for (int d : row) {
				if (d > 0) {
					return true;
				}
			}
		}
		return false;
	}

	private void clearCarving() {
		for (int[] row : depth) {
			java.util.Arrays.fill(row, 0);
		}
		updateButtons();
	}

	private void updateButtons() {
		if (confirmButton != null) {
			confirmButton.active = this.menu.canEngrave() && validModifiers() != null;
		}
		if (resetButton != null) {
			resetButton.active = anyCarved();
		}
	}

	private void sendEngrave() {
		if (this.menu.canEngrave() && validModifiers() != null) {
			ClientPlayNetworking.send(new FinishEngravingC2SPayload(carvedBits(), deepBits()));
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.menu.canEngrave() && gemOpaque != null) {
			int cell = cellAt(mouseX, mouseY);
			if (cell >= 0) {
				int row = cell / GRID;
				int col = cell % GRID;
				if (gemOpaque[row][col]) { // can't chisel transparent pixels
					depth[row][col] = Math.min(depth[row][col] + 1, 2); // chisel deeper, capped
					updateButtons();
				}
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private int cellAt(double mouseX, double mouseY) {
		int ox = this.leftPos + GRID_X;
		int oy = this.topPos + GRID_Y;
		if (mouseX < ox || mouseY < oy || mouseX >= ox + GRID * CELL || mouseY >= oy + GRID * CELL) {
			return -1;
		}
		int col = (int) ((mouseX - ox) / CELL);
		int row = (int) ((mouseY - oy) / CELL);
		return row * GRID + col;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int left = this.leftPos;
		int top = this.topPos;
		g.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFFC6C6C6);
		g.fill(left, top, left + this.imageWidth, top + 1, 0xFFFFFFFF);
		g.fill(left, top, left + 1, top + this.imageHeight, 0xFFFFFFFF);
		g.fill(left + this.imageWidth - 1, top, left + this.imageWidth, top + this.imageHeight, 0xFF555555);
		g.fill(left, top + this.imageHeight - 1, left + this.imageWidth, top + this.imageHeight, 0xFF555555);

		for (var slot : this.menu.slots) {
			if (slot.isActive()) {
				drawSlot(g, left + slot.x, top + slot.y);
			}
		}

		renderGrid(g);
	}

	private static void drawSlot(GuiGraphics g, int x, int y) {
		g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
		g.fill(x - 1, y - 1, x + 17, y, 0xFF8B8B8B);
		g.fill(x - 1, y - 1, x, y + 17, 0xFF8B8B8B);
	}

	private void renderGrid(GuiGraphics g) {
		Pattern pattern = this.menu.targetPattern();
		int ox = this.leftPos + GRID_X;
		int oy = this.topPos + GRID_Y;
		int size = GRID * CELL;
		g.fill(ox - 2, oy - 2, ox + size + 2, oy + size + 2, 0xFF101010);

		for (int row = 0; row < GRID; row++) {
			for (int col = 0; col < GRID; col++) {
				int x1 = ox + col * CELL;
				int y1 = oy + row * CELL;
				int x2 = x1 + CELL;
				int y2 = y1 + CELL;

				boolean opaque = gemOpaque != null && gemOpaque[row][col];
				if (opaque) {
					g.fill(x1, y1, x2, y2, gemPixels[row][col]);
				}

				int d = depth[row][col];
				if (d >= 2) {
					g.fill(x1, y1, x2, y2, 0xEE070707); // deep cut — slightly darker
				} else if (d == 1) {
					g.fill(x1, y1, x2, y2, 0xCC141414); // carved groove
				} else if (opaque && pattern != null && pattern.isCellCarved(row, col)) {
					g.fill(x1, y1, x2, y2, 0x66FFFFFF); // faint hint of the base symbol
				}
			}
		}

		for (int i = 0; i <= GRID; i++) {
			g.fill(ox + i * CELL, oy, ox + i * CELL + 1, oy + size, 0x18FFFFFF);
			g.fill(ox, oy + i * CELL, ox + size, oy + i * CELL + 1, 0x18FFFFFF);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		super.renderLabels(g, mouseX, mouseY);
		Component status = statusLine();
		if (status != null) {
			g.drawString(this.font, status, 8, 147, 0xFF555555, false);
		}
	}

	private Component statusLine() {
		ItemStack gem = this.menu.gemStack();
		Pattern pattern = this.menu.targetPattern();
		if (pattern == null) {
			return Component.translatable("screen.socket-sorcery.need_scroll").withStyle(ChatFormatting.DARK_GRAY);
		}
		if (!(gem.getItem() instanceof GemItem)) {
			return Component.translatable("screen.socket-sorcery.need_gem").withStyle(ChatFormatting.DARK_GRAY);
		}
		if (!Patterns.canEngrave(gem.getItem(), pattern.id())) {
			return Component.translatable("screen.socket-sorcery.incompatible").withStyle(ChatFormatting.DARK_RED);
		}
		if (!this.menu.canEngrave()) {
			return Component.translatable("screen.socket-sorcery.need_chisel").withStyle(ChatFormatting.DARK_GRAY);
		}

		// Base symbol not fully carved yet: still forming the base.
		if (!GridBits.subset(pattern.maskBits(), carvedBits())) {
			return pattern.coloredName();
		}
		Set<ResourceLocation> modifiers = validModifiers();
		if (modifiers == null) {
			return Component.translatable("screen.socket-sorcery.invalid_engraving").withStyle(ChatFormatting.DARK_RED);
		}
		MutableComponent line = pattern.coloredName();
		for (ResourceLocation id : modifiers) {
			Modifier modifier = Modifiers.get(id);
			if (modifier != null) {
				line.append(Component.literal(" + ").withStyle(ChatFormatting.DARK_GRAY)).append(modifier.coloredName());
			}
		}
		return line;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		this.renderTooltip(g, mouseX, mouseY);
	}
}
