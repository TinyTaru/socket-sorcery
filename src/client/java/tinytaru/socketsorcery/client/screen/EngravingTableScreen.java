package tinytaru.socketsorcery.client.screen;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.menu.EngravingTableMenu;
import tinytaru.socketsorcery.net.EngraveResult;
import tinytaru.socketsorcery.net.FinishEngravingC2SPayload;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.pattern.Modifier;
import tinytaru.socketsorcery.pattern.Modifiers;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;

/**
 * The Engraving Table screen. The gem is shown in the background of a 16x16 grid, one cell per gem
 * pixel; the scroll's symbol is hinted faintly. Left-click chisels an opaque cell deeper (depth 1
 * then 2, capped). Depth 1 carves the base symbol; depth-2 cells form modifiers. There are no
 * modifier hints — the status line just names the modifiers you've correctly formed (learn-by-doing).
 *
 * <p>Pattern/modifier definitions come from the synced dynamic registries, resolved through the
 * menu's captured registry view.
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

	private Holder.Reference<Pattern> shownPattern;
	private Button confirmButton;
	private Button resetButton;

	// Feedback state: a short cell flash after a chisel stroke, and the last engrave result banner.
	private int carveFlashRow = -1;
	private int carveFlashCol = -1;
	private int carveFlashTicks;
	private EngraveResult lastResult;
	private int resultTicks;

	public EngravingTableScreen(EngravingTableMenu menu, Inventory inventory, Component title) {
		// imageWidth/imageHeight are final now and come from the constructor.
		super(menu, inventory, title, 200, 252);
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
		Holder.Reference<Pattern> pattern = this.menu.targetPattern();
		if (pattern != shownPattern) {
			shownPattern = pattern;
			clearCarving();
		}
		if (carveFlashTicks > 0) {
			carveFlashTicks--;
		}
		if (resultTicks > 0) {
			resultTicks--;
		}
		updateButtons();
	}

	/** Called from the client networking handler when the server reports an engraving outcome. */
	public void onEngraveResult(EngraveResult result) {
		this.lastResult = result;
		this.resultTicks = result.success() ? 30 : 45;
		if (result.success()) {
			clearCarving();
		}
	}

	private void ensureGemData() {
		ItemStack gemStack = this.menu.gemStack();
		Item gem = gemStack.isEmpty() ? null : gemStack.getItem();
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

		Identifier itemId = BuiltInRegistries.ITEM.getKey(gem);
		Identifier texture = Identifier.fromNamespaceAndPath(
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
					// NativeImage's public accessor is ARGB now, which is already what we store.
					int argb = image.getPixel(col, row);
					opaque[row][col] = ((argb >>> 24) & 0xFF) > 16;
					pixels[row][col] = argb;
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
	private Set<Identifier> validModifiers() {
		Holder.Reference<Pattern> holder = this.menu.targetPattern();
		if (holder == null) {
			return null;
		}
		Pattern pattern = holder.value();
		long[] carved = carvedBits();
		long[] deep = deepBits();
		Set<Identifier> modifiers = Modifiers.decode(this.menu.registries(), pattern, deep);
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

	/** Mouse input arrives as a {@link MouseButtonEvent} now, carrying position and button together. */
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int button = event.button();
		// Left-click chisels a cell deeper (0→1→2); right-click eases it back (2→1→0).
		if ((button == 0 || button == 1) && this.menu.canEngrave() && gemOpaque != null) {
			int cell = cellAt(event.x(), event.y());
			if (cell >= 0) {
				int row = cell / GRID;
				int col = cell % GRID;
				if (gemOpaque[row][col]) { // can't chisel transparent pixels
					int before = depth[row][col];
					depth[row][col] = button == 0 ? Math.min(before + 1, 2) : Math.max(before - 1, 0);
					if (depth[row][col] != before) {
						carveFlashRow = row;
						carveFlashCol = col;
						carveFlashTicks = 4;
						playCarveSound(depth[row][col]);
					}
					updateButtons();
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void playCarveSound(int depthNow) {
		float pitch = depthNow >= 2 ? 1.5F : (depthNow == 1 ? 1.1F : 0.8F);
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_HIT, pitch));
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

	/**
	 * The panel, the carving grid and the engraved-gem preview. {@code renderBg} is gone: the
	 * background goes at the head of {@code extractContents} and anything that used to be drawn on
	 * top in {@code render} goes after the super call, since drawing is submission-ordered now.
	 */
	@Override
	public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		extractPanel(g, mouseX, mouseY);
		super.extractContents(g, mouseX, mouseY, partialTick);
		ItemStack preview = previewStack();
		if (preview != null) {
			g.item(preview, this.leftPos + 42, this.topPos + 70);
		}
	}

	private void extractPanel(GuiGraphicsExtractor g, int mouseX, int mouseY) {
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

		renderGrid(g, mouseX, mouseY);
	}

	private static void drawSlot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
		g.fill(x - 1, y - 1, x + 17, y, 0xFF8B8B8B);
		g.fill(x - 1, y - 1, x, y + 17, 0xFF8B8B8B);
	}

	private void renderGrid(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		Holder.Reference<Pattern> holder = this.menu.targetPattern();
		Pattern pattern = holder == null ? null : holder.value();
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

		// Tint the cells of each correctly-formed modifier in its own colour: feedback on what you've
		// shaped, without revealing where unformed modifiers live (the discovery is the point).
		Set<Identifier> valid = pattern == null ? null : validModifiers();
		if (valid != null) {
			for (Identifier id : valid) {
				Holder.Reference<Modifier> modifier = Modifiers.get(this.menu.registries(), id);
				if (modifier == null) {
					continue;
				}
				long[] cells = modifier.value().cellMask(pattern);
				if (cells == null) {
					continue;
				}
				int tint = 0x80000000 | (modifier.value().color() & 0xFFFFFF);
				for (int idx = 0; idx < GRID * GRID; idx++) {
					if (GridBits.getIndex(cells, idx)) {
						int x1 = ox + (idx % GRID) * CELL;
						int y1 = oy + (idx / GRID) * CELL;
						g.fill(x1, y1, x1 + CELL, y1 + CELL, tint);
					}
				}
			}
		}

		// Brief white flash on the cell just chiselled.
		if (carveFlashTicks > 0 && carveFlashRow >= 0) {
			int alpha = Math.min(0xFF, 0x30 + 0x30 * carveFlashTicks);
			int x1 = ox + carveFlashCol * CELL;
			int y1 = oy + carveFlashRow * CELL;
			g.fill(x1, y1, x1 + CELL, y1 + CELL, (alpha << 24) | 0xFFFFFF);
		}

		// Hover highlight: white over a carveable pixel, faint red over a transparent one.
		int hover = cellAt(mouseX, mouseY);
		if (hover >= 0 && this.menu.canEngrave()) {
			int row = hover / GRID;
			int col = hover % GRID;
			boolean opaque = gemOpaque != null && gemOpaque[row][col];
			int x1 = ox + col * CELL;
			int y1 = oy + row * CELL;
			g.fill(x1, y1, x1 + CELL, y1 + CELL, opaque ? 0x55FFFFFF : 0x33FF5555);
		}

		for (int i = 0; i <= GRID; i++) {
			g.fill(ox + i * CELL, oy, ox + i * CELL + 1, oy + size, 0x18FFFFFF);
			g.fill(ox, oy + i * CELL, ox + size, oy + i * CELL + 1, 0x18FFFFFF);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		super.extractLabels(g, mouseX, mouseY);
		Component status = statusLine();
		if (status != null) {
			g.text(this.font, status, 8, 147, 0xFF555555, false);
		}
	}

	private Component statusLine() {
		if (resultTicks > 0 && lastResult != null) {
			return resultMessage(lastResult);
		}
		ItemStack gem = this.menu.gemStack();
		Holder.Reference<Pattern> holder = this.menu.targetPattern();
		if (holder == null) {
			return Component.translatable("screen.socket-sorcery.need_scroll").withStyle(ChatFormatting.DARK_GRAY);
		}
		Pattern pattern = holder.value();
		Identifier patternId = holder.key().identifier();
		if (gem.isEmpty()) {
			return Component.translatable("screen.socket-sorcery.need_gem").withStyle(ChatFormatting.DARK_GRAY);
		}
		if (!Patterns.canEngrave(this.menu.registries(), gem.getItem(), patternId)) {
			return Component.translatable("screen.socket-sorcery.incompatible").withStyle(ChatFormatting.DARK_RED);
		}
		if (!this.menu.canEngrave()) {
			return Component.translatable("screen.socket-sorcery.need_chisel").withStyle(ChatFormatting.DARK_GRAY);
		}

		// Base symbol not fully carved yet: still forming the base.
		if (!GridBits.subset(pattern.maskBits(), carvedBits())) {
			return pattern.coloredName(patternId);
		}
		Set<Identifier> modifiers = validModifiers();
		if (modifiers == null) {
			return Component.translatable("screen.socket-sorcery.invalid_engraving").withStyle(ChatFormatting.DARK_RED);
		}
		MutableComponent line = pattern.coloredName(patternId);
		for (Identifier id : modifiers) {
			Holder.Reference<Modifier> modifier = Modifiers.get(this.menu.registries(), id);
			if (modifier != null) {
				line.append(Component.literal(" + ").withStyle(ChatFormatting.DARK_GRAY))
						.append(modifier.value().coloredName(id));
			}
		}
		return line;
	}

	private Component resultMessage(EngraveResult result) {
		return switch (result) {
			case OK -> Component.translatable("screen.socket-sorcery.engrave_success").withStyle(ChatFormatting.GREEN);
			case NO_CHISEL -> Component.translatable("screen.socket-sorcery.fail_no_chisel").withStyle(ChatFormatting.DARK_RED);
			case NOT_ENGRAVABLE -> Component.translatable("screen.socket-sorcery.fail_incompatible").withStyle(ChatFormatting.DARK_RED);
			case BAD_MODIFIERS -> Component.translatable("screen.socket-sorcery.fail_bad_modifiers").withStyle(ChatFormatting.DARK_RED);
			case BAD_SYMBOL -> Component.translatable("screen.socket-sorcery.fail_bad_symbol").withStyle(ChatFormatting.DARK_RED);
		};
	}

	/** A live preview of the gem as it would look once engraved with the current valid carve. */
	private ItemStack previewStack() {
		Set<Identifier> modifiers = validModifiers();
		Holder.Reference<Pattern> holder = this.menu.targetPattern();
		if (modifiers == null || holder == null) {
			return null;
		}
		ItemStack preview = this.menu.gemStack().copy();
		if (preview.isEmpty()) {
			return null;
		}
		preview.setCount(1);
		preview.set(ModComponents.ENGRAVING,
				new EngravingData(holder.key().identifier(), Modifiers.ordered(modifiers)));
		return preview;
	}

	/** Names the hovered cell: a formed modifier's cell, or a base-symbol cell. */
	@Override
	protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		Holder.Reference<Pattern> holder = this.menu.targetPattern();
		if (holder == null) {
			super.extractTooltip(g, mouseX, mouseY);
			return;
		}
		Pattern pattern = holder.value();
		int cell = cellAt(mouseX, mouseY);
		if (cell < 0) {
			super.extractTooltip(g, mouseX, mouseY);
			return;
		}
		Set<Identifier> valid = validModifiers();
		if (valid != null) {
			for (Identifier id : valid) {
				Holder.Reference<Modifier> modifier = Modifiers.get(this.menu.registries(), id);
				long[] cells = modifier == null ? null : modifier.value().cellMask(pattern);
				if (cells != null && GridBits.getIndex(cells, cell)) {
					g.setTooltipForNextFrame(modifier.value().coloredName(id), mouseX, mouseY);
					return;
				}
			}
		}
		if (depth[cell / GRID][cell % GRID] >= 1 && pattern.isCellCarved(cell / GRID, cell % GRID)) {
			g.setTooltipForNextFrame(pattern.coloredName(holder.key().identifier()), mouseX, mouseY);
			return;
		}
		super.extractTooltip(g, mouseX, mouseY);
	}
}
