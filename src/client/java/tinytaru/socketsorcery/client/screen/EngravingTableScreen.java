package tinytaru.socketsorcery.client.screen;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

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
import tinytaru.socketsorcery.component.CarvingData;
import tinytaru.socketsorcery.menu.EngravingTableMenu;
import tinytaru.socketsorcery.net.ChiselC2SPayload;
import tinytaru.socketsorcery.pattern.Carvings;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.pattern.Modifier;
import tinytaru.socketsorcery.pattern.Modifiers;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.item.RingItem;
import tinytaru.socketsorcery.registry.ModComponents;

/**
 * The Engraving Table screen. The gem is shown in the background of a 16x16 grid, one cell per gem
 * pixel; the scroll's symbol is hinted faintly. Left-click chisels an opaque cell deeper (depth 1
 * then 2, capped). Depth 1 carves the base symbol; depth-2 cells form modifiers. Right-click eases a
 * cut back, at the cost of a gem dust matching the placed gem. There are no modifier hints — the
 * status line just names the modifiers you've correctly formed (learn-by-doing).
 *
 * <p>The grid is not this screen's state: every click goes to the server, which cuts the gem and
 * saves the carve onto the item, and the grid is drawn from what the gem carries. There is no
 * confirmation step — the engraving takes hold the moment the cells form a whole pattern, and the
 * cut cells pulse to say so (only the new modifier's cells when it is a modifier that closed).
 * Strokes are shown immediately and reconciled with the item a beat later, so carving stays
 * responsive on a laggy connection without ever drifting from what the server actually cut.
 *
 * <p>Pattern/modifier definitions come from the synced dynamic registries, resolved through the
 * menu's captured registry view.
 */
public class EngravingTableScreen extends AbstractContainerScreen<EngravingTableMenu> {

	private static final int GRID = Pattern.GRID; // 16
	private static final int CELL = 8;
	private static final int GRID_X = 64;
	private static final int GRID_Y = 18;

	/** How long a completion pulse runs, and how long a status banner lingers, in ticks. */
	private static final int PULSE_TICKS = 24;
	private static final int BANNER_TICKS = 40;

	/** How long after a stroke the drawn grid is allowed to run ahead of the item it was sent to. */
	private static final int STROKE_GRACE_TICKS = 8;

	private final int[][] depth = new int[GRID][GRID]; // 0 = bare, 1 = carved, 2 = deep

	// Cached gem pixel data so we don't read the texture every frame.
	private Item cachedGemItem;
	private int[][] gemPixels;   // [row][col] ARGB, null if no gem loaded
	private boolean[][] gemOpaque;

	private Button resetButton;

	// Ticks left in which the drawn grid may still be ahead of the gem, because a stroke we drew is
	// on its way to the server. Once it runs out the item is simply believed — which is also what
	// silently undoes a stroke the server refused.
	private int strokeGraceTicks;

	// Click-and-drag state: which button is being dragged and the last cell it acted on, so a
	// continuous drag chisels each cell it crosses exactly once rather than flickering back and forth.
	private int dragButton = -1;
	private int lastDragRow = -1;
	private int lastDragCol = -1;

	// Feedback state: a short cell flash after a chisel stroke, the completion pulse, and the banner
	// that names what was just finished.
	private int carveFlashRow = -1;
	private int carveFlashCol = -1;
	private int carveFlashTicks;
	private long[] pulseCells;
	private int pulseColor;
	private int pulseTicks;
	private Component banner;
	private int bannerTicks;

	// Whether the OS cursor is currently hidden in favour of drawing the equipped chisel over it —
	// active only while hovering an engravable grid, so it never masks buttons or slots.
	private boolean chiselCursorActive;

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
		this.resetButton = this.addRenderableWidget(Button
				.builder(Component.translatable("screen.socket-sorcery.reset"), b -> sendClear())
				.bounds(this.leftPos + 8, this.topPos + 100, 48, 18)
				.build());
		updateButtons();
	}

	/** Restores the real OS cursor if this screen closes (or the game quits to it) while it's hidden. */
	@Override
	public void removed() {
		setChiselCursorActive(false);
		super.removed();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (strokeGraceTicks > 0) {
			strokeGraceTicks--;
		}
		ensureGemData();
		reconcileWithGem(false);
		if (carveFlashTicks > 0) {
			carveFlashTicks--;
		}
		if (pulseTicks > 0) {
			pulseTicks--;
		}
		if (bannerTicks > 0) {
			bannerTicks--;
		}
		updateButtons();
	}

	/** Called from the client networking handler when a stroke finished the whole pattern. */
	public void onPatternEngraved() {
		Holder.Reference<Pattern> holder = this.menu.targetPattern();
		pulse(carvedBits(), holder == null ? 0xFFFFFF : holder.value().color());
		showBanner(Component.translatable("screen.socket-sorcery.engrave_success").withStyle(ChatFormatting.GREEN));
	}

	/** Called from the client networking handler when a stroke closed one or more modifiers. */
	public void onModifiersFormed(List<Identifier> ids) {
		Holder.Reference<Pattern> holder = this.menu.targetPattern();
		if (holder == null) {
			return;
		}
		long[] cells = GridBits.empty();
		MutableComponent line = null;
		int color = holder.value().color();
		for (Identifier id : ids) {
			Holder.Reference<Modifier> modifier = Modifiers.get(this.menu.registries(), id);
			if (modifier == null) {
				continue;
			}
			long[] modifierCells = modifier.value().cellMask(holder.value());
			if (modifierCells != null) {
				GridBits.orInto(cells, modifierCells);
			}
			color = modifier.value().color();
			Component name = modifier.value().coloredName(id);
			line = line == null ? Component.empty().append(name)
					: line.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY)).append(name);
		}
		if (line == null) {
			return;
		}
		pulse(cells, color);
		showBanner(Component.translatable("screen.socket-sorcery.modifier_formed", line));
	}

	private void pulse(long[] cells, int color) {
		pulseCells = cells;
		pulseColor = color;
		pulseTicks = PULSE_TICKS;
	}

	private void showBanner(Component message) {
		banner = message;
		bannerTicks = BANNER_TICKS;
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
		reconcileWithGem(true); // a different gem is a different carve; adopt it outright
		pulseTicks = 0;
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

	/**
	 * Pulls the grid back in line with the carve saved on the gem. A stroke is drawn the instant it's
	 * clicked, so for the round trip it takes the server to cut the gem and send it back the grid is
	 * legitimately ahead of the item; outside that window the item is simply believed, which is what
	 * quietly undoes a refused stroke and what picks up a gem swapped for another of the same kind.
	 * {@code force} skips the window entirely, for when the gem in the slot changes outright.
	 */
	private void reconcileWithGem(boolean force) {
		CarvingData carve = Carvings.on(this.menu.registries(), this.menu.gemStack());
		int[][] saved = new int[GRID][GRID];
		if (carve != null) {
			for (int cell = 0; cell < GRID * GRID; cell++) {
				saved[cell / GRID][cell % GRID] = Carvings.depth(carve.carved(), carve.deep(), cell);
			}
		}
		if (Arrays.deepEquals(saved, depth) || (!force && strokeGraceTicks > 0)) {
			return;
		}
		for (int row = 0; row < GRID; row++) {
			System.arraycopy(saved[row], 0, depth[row], 0, GRID);
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

	/**
	 * The modifier set the current carve forms as pure geometry, ignoring whether the pattern accepts
	 * them; null if the deep cells are stray or the symbol doesn't match. Kept separate from
	 * {@link #acceptedModifiers} so the status line can distinguish a botched carve from a well-formed
	 * gesture this pattern refuses.
	 */
	private Set<Identifier> formedModifiers() {
		Holder.Reference<Pattern> holder = this.menu.targetPattern();
		return holder == null ? null
				: Carvings.formedModifiers(this.menu.registries(), holder.value(), carvedBits(), deepBits());
	}

	/** The applied modifier set if the current carve is a finished, acceptable engraving, else null. */
	private Set<Identifier> acceptedModifiers() {
		Holder.Reference<Pattern> holder = this.menu.targetPattern();
		return holder == null ? null
				: Carvings.acceptedModifiers(this.menu.registries(), holder.value(), carvedBits(), deepBits());
	}

	/** Total chisel strokes standing on the gem — also what easing them all back would cost in dust. */
	private int strokeCount() {
		int strokes = 0;
		for (int[] row : depth) {
			for (int d : row) {
				strokes += d;
			}
		}
		return strokes;
	}

	private void updateButtons() {
		if (resetButton == null) {
			return;
		}
		int strokes = strokeCount();
		resetButton.active = strokes > 0 && this.menu.canAffordEase(strokes);
	}

	private void sendStroke(int cell, ChiselC2SPayload.Action action) {
		ClientPlayNetworking.send(new ChiselC2SPayload(cell, action));
		strokeGraceTicks = STROKE_GRACE_TICKS;
	}

	/** Eases every cut back at once — the same dust bill as doing it a cell at a time by hand. */
	private void sendClear() {
		int strokes = strokeCount();
		if (strokes == 0) {
			return;
		}
		if (!this.menu.canAffordEase(strokes)) {
			playDeniedSound();
			return;
		}
		for (int[] row : depth) {
			Arrays.fill(row, 0);
		}
		sendStroke(0, ChiselC2SPayload.Action.CLEAR);
		pulseTicks = 0;
		carveFlashTicks = 0;
		playCarveSound(0);
		updateButtons();
	}

	/** Mouse input arrives as a {@link MouseButtonEvent} now, carrying position and button together. */
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int button = event.button();
		// Left-click chisels a cell deeper (0→1→2); right-click eases it back (2→1→0), at a dust cost.
		if ((button == 0 || button == 1) && this.menu.canCarve() && gemOpaque != null) {
			int cell = cellAt(event.x(), event.y());
			if (cell >= 0 && ringCellAllowed(cell)) {
				int row = cell / GRID;
				int col = cell % GRID;
				applyChisel(button, row, col);
				dragButton = button;
				lastDragRow = row;
				lastDragCol = col;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	/**
	 * Click-and-drag: as the mouse is dragged with a button held, chisel every new cell it crosses the
	 * same way the initial click did, so a stroke can carve or ease back many pixels in one drag.
	 */
	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		int button = event.button();
		if (button == dragButton && this.menu.canCarve() && gemOpaque != null) {
			int cell = cellAt(event.x(), event.y());
			if (cell >= 0 && ringCellAllowed(cell)) {
				int row = cell / GRID;
				int col = cell % GRID;
				if (row != lastDragRow || col != lastDragCol) {
					lastDragRow = row;
					lastDragCol = col;
					applyChisel(button, row, col);
				}
				return true;
			}
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == dragButton) {
			dragButton = -1;
			lastDragRow = -1;
			lastDragCol = -1;
		}
		return super.mouseReleased(event);
	}

	/**
	 * Chisels a single cell one step deeper (left) or eases it back one step (right), at a dust cost.
	 * The cut is drawn straight away and sent to the server, which is what actually cuts the gem.
	 */
	private void applyChisel(int button, int row, int col) {
		if (!ringCellAllowed(row * GRID + col)) {
			return;
		}
		if (!gemOpaque[row][col]) { // can't chisel transparent pixels
			return;
		}
		int before = depth[row][col];
		ChiselC2SPayload.Action action;
		if (button == 1) {
			if (before == 0) {
				return;
			}
			if (!this.menu.canAffordEase(1)) {
				playDeniedSound();
				return;
			}
			action = ChiselC2SPayload.Action.EASE;
			depth[row][col] = before - 1;
		} else {
			if (before >= 2) {
				return;
			}
			action = ChiselC2SPayload.Action.DEEPEN;
			depth[row][col] = before + 1;
		}
		sendStroke(GridBits.index(row, col), action);
		carveFlashRow = row;
		carveFlashCol = col;
		carveFlashTicks = 4;
		playCarveSound(depth[row][col]);
		updateButtons();
	}

	private void playCarveSound(int depthNow) {
		float pitch = depthNow >= 2 ? 1.5F : (depthNow == 1 ? 1.1F : 0.8F);
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_HIT, pitch));
	}

	private void playDeniedSound() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0F));
	}

	/** Hides (or restores) the real OS cursor, swapping it for the equipped chisel drawn each frame. */
	private void setChiselCursorActive(boolean active) {
		if (active == chiselCursorActive) {
			return;
		}
		chiselCursorActive = active;
		long window = Minecraft.getInstance().getWindow().handle();
		GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR,
				active ? GLFW.GLFW_CURSOR_HIDDEN : GLFW.GLFW_CURSOR_NORMAL);
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

	private boolean ringCellAllowed(int cell) {
		ItemStack stack = this.menu.gemStack();
		if (!(stack.getItem() instanceof RingItem)) {
			return true;
		}
		int row = cell / GRID;
		int col = cell % GRID;
		return col >= 5 && col <= 10 && row >= 9 && row <= 12;
	}

	/**
	 * The panel, the carving grid and the engraved-gem preview. {@code renderBg} is gone: the
	 * background goes at the head of {@code extractContents} and anything that used to be drawn on
	 * top in {@code render} goes after the super call, since drawing is submission-ordered now.
	 */
	@Override
	public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		extractPanel(g, mouseX, mouseY, partialTick);
		super.extractContents(g, mouseX, mouseY, partialTick);
		ItemStack gem = this.menu.gemStack();
		if (gem.has(ModComponents.ENGRAVING)) {
			g.item(gem, this.leftPos + 42, this.topPos + 70);
		}
		extractResetCost(g);
		extractChiselCursor(g, mouseX, mouseY);
	}

	/**
	 * The current gem's dust icon and the stroke count Reset would spend, shown beside the button
	 * itself rather than behind a hover — the cost of clicking Reset should be visible at a glance.
	 */
	private void extractResetCost(GuiGraphicsExtractor g) {
		int strokes = strokeCount();
		Item dust = this.menu.currentGemDust();
		if (strokes <= 0 || dust == null) {
			return;
		}
		int x = this.leftPos + 8;
		int y = this.topPos + 122;
		g.item(dust.getDefaultInstance(), x, y);
		g.text(this.font, Component.literal("x" + strokes), x + 18, y + 4, 0xFFFFFFFF, true);
	}

	// The chisel item icons are drawn on a 16x16 canvas but the tool itself doesn't reach the edges —
	// its actual tip sits at this pixel within the texture, not the texture's corner.
	private static final int CHISEL_TIP_X = 12;
	private static final int CHISEL_TIP_Y = 3;

	/**
	 * While hovering an engravable grid, swap the OS cursor for the equipped chisel (so its tier is
	 * visible at a glance) by hiding the real cursor and drawing the chisel's own item icon in its place.
	 * The icon is offset so its tip pixel — not the texture's corner — lands on the actual mouse
	 * position (its hotspot).
	 */
	private void extractChiselCursor(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		ItemStack chisel = this.menu.chiselStack();
		boolean overGrid = this.menu.canCarve() && cellAt(mouseX, mouseY) >= 0 && !chisel.isEmpty();
		setChiselCursorActive(overGrid);
		if (overGrid) {
			g.item(chisel, mouseX - CHISEL_TIP_X, mouseY - CHISEL_TIP_Y);
		}
	}

	private void extractPanel(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
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

		renderGrid(g, mouseX, mouseY, partialTick);
	}

	private static void drawSlot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
		g.fill(x - 1, y - 1, x + 17, y, 0xFF8B8B8B);
		g.fill(x - 1, y - 1, x, y + 17, 0xFF8B8B8B);
	}

	private void renderGrid(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
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
		Set<Identifier> accepted = pattern == null ? null : acceptedModifiers();
		if (accepted != null) {
			for (Identifier id : accepted) {
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

		renderPulse(g, ox, oy, partialTick);

		// Brief white flash on the cell just chiselled.
		if (carveFlashTicks > 0 && carveFlashRow >= 0) {
			int alpha = Math.min(0xFF, 0x30 + 0x30 * carveFlashTicks);
			int x1 = ox + carveFlashCol * CELL;
			int y1 = oy + carveFlashRow * CELL;
			g.fill(x1, y1, x1 + CELL, y1 + CELL, (alpha << 24) | 0xFFFFFF);
		}

		// Hover highlight: white over a carveable pixel, faint red over a transparent one.
		int hover = cellAt(mouseX, mouseY);
		if (hover >= 0 && ringCellAllowed(hover) && this.menu.canCarve()) {
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

	/**
	 * The completion pulse: the cells that just closed something brighten and fade a few times over
	 * {@link #PULSE_TICKS}, in the colour of whatever closed — the whole carve for a finished pattern,
	 * only that modifier's cells for a modifier. Driven off {@code partialTick} so it breathes
	 * smoothly rather than stepping at 20Hz.
	 */
	private void renderPulse(GuiGraphicsExtractor g, int ox, int oy, float partialTick) {
		if (pulseTicks <= 0 || pulseCells == null) {
			return;
		}
		float remaining = Math.max(0.0F, pulseTicks - partialTick);
		float wave = (float) Math.abs(Math.sin(remaining * Math.PI / 6.0));
		int alpha = (int) (0xC0 * wave * (remaining / PULSE_TICKS));
		if (alpha <= 0) {
			return;
		}
		int color = (alpha << 24) | (brighten(pulseColor) & 0xFFFFFF);
		for (int idx = 0; idx < GRID * GRID; idx++) {
			if (GridBits.getIndex(pulseCells, idx)) {
				int x1 = ox + (idx % GRID) * CELL;
				int y1 = oy + (idx / GRID) * CELL;
				g.fill(x1, y1, x1 + CELL, y1 + CELL, color);
			}
		}
	}

	/** Half-way to white, so a pulse reads as the same colour lit up rather than a different one. */
	private static int brighten(int rgb) {
		int r = (((rgb >> 16) & 0xFF) + 0xFF) / 2;
		int gr = (((rgb >> 8) & 0xFF) + 0xFF) / 2;
		int b = ((rgb & 0xFF) + 0xFF) / 2;
		return (r << 16) | (gr << 8) | b;
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
		if (bannerTicks > 0 && banner != null) {
			return banner;
		}
		ItemStack gem = this.menu.gemStack();
		Holder.Reference<Pattern> holder = this.menu.targetPattern();
		if (gem.isEmpty()) {
			return Component.translatable("screen.socket-sorcery.need_gem").withStyle(ChatFormatting.DARK_GRAY);
		}
		if (holder == null) {
			return Component.translatable("screen.socket-sorcery.need_scroll").withStyle(ChatFormatting.DARK_GRAY);
		}
		Pattern pattern = holder.value();
		Identifier patternId = holder.key().identifier();
		if (!Patterns.canEngrave(this.menu.registries(), gem.getItem(), patternId)) {
			return Component.translatable("screen.socket-sorcery.incompatible").withStyle(ChatFormatting.DARK_RED);
		}
		if (!this.menu.canCarve()) {
			return Component.translatable("screen.socket-sorcery.need_chisel").withStyle(ChatFormatting.DARK_GRAY);
		}
		if (pattern.ringTrigger().isPresent()) {
			return pattern.coloredName(patternId);
		}

		// Base symbol not fully carved yet: still forming the base.
		if (!GridBits.subset(pattern.maskBits(), carvedBits())) {
			return pattern.coloredName(patternId);
		}
		Set<Identifier> modifiers = acceptedModifiers();
		if (modifiers == null) {
			Set<Identifier> formed = formedModifiers();
			List<Identifier> rejected = formed == null ? List.of() : Modifiers.incompatible(pattern, formed);
			if (!rejected.isEmpty()) {
				Identifier id = rejected.getFirst();
				Holder.Reference<Modifier> modifier = Modifiers.get(this.menu.registries(), id);
				Component name = modifier != null ? modifier.value().coloredName(id) : Modifier.displayName(id);
				return Component.translatable("screen.socket-sorcery.modifier_rejected",
						name, pattern.coloredName(patternId)).withStyle(ChatFormatting.DARK_RED);
			}
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
		Set<Identifier> accepted = acceptedModifiers();
		if (accepted != null) {
			for (Identifier id : accepted) {
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
