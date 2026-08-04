package tinytaru.socketsorcery.client;

import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.Holder;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.PatternTooltip;
import tinytaru.socketsorcery.pattern.Patterns;

/**
 * Renders a {@link PatternTooltip} as a miniature version of the engraving grid, tinted with the
 * pattern's colour. The scroll item is resolved to its pattern here, against the client level's
 * synced registry (no level or no claiming pattern → renders nothing).
 */
public class PatternTooltipRenderer implements ClientTooltipComponent {

	private static final int CELL = 6;
	private static final int SIZE = Pattern.GRID * CELL;
	private static final int RING_WIDTH = 6;
	private static final int RING_HEIGHT = 4;
	private static final int MARGIN = 4;

	private final Pattern pattern; // null when unresolvable — renders as empty
	private final boolean ringPattern;

	public PatternTooltipRenderer(PatternTooltip tooltip) {
		Holder.Reference<Pattern> holder = Minecraft.getInstance().level == null ? null
				: Patterns.forScroll(Minecraft.getInstance().level.registryAccess(), tooltip.scroll());
		this.pattern = holder == null ? null : holder.value();
		this.ringPattern = pattern != null && pattern.ringTrigger().isPresent();
	}

	public static void register() {
		ClientTooltipComponentCallback.EVENT.register(data ->
				data instanceof PatternTooltip tooltip ? new PatternTooltipRenderer(tooltip) : null);
	}

	@Override
	public int getHeight(Font font) {
		return pattern == null ? 0 : (ringPattern ? RING_HEIGHT * CELL : Pattern.GRID * CELL) + MARGIN * 2;
	}

	@Override
	public int getWidth(Font font) {
		return pattern == null ? 0 : (ringPattern ? RING_WIDTH * CELL : Pattern.GRID * CELL) + MARGIN * 2;
	}

	/** {@code renderImage} became {@code extractImage}, with width/height passed in and g moved last. */
	@Override
	public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor g) {
		if (pattern == null) {
			return;
		}
		int ox = x + MARGIN;
		int oy = y + MARGIN;
		int boxWidth = ringPattern ? RING_WIDTH : Pattern.GRID;
		int boxHeight = ringPattern ? RING_HEIGHT : Pattern.GRID;
		int colOffset = ringPattern ? 5 : 0;
		int rowOffset = ringPattern ? 9 : 0;
		g.fill(ox - 2, oy - 2, ox + boxWidth * CELL + 2, oy + boxHeight * CELL + 2, 0xFF101010);

		int fill = 0xFF000000 | (pattern.color() & 0xFFFFFF);
		for (int row = rowOffset; row < rowOffset + boxHeight; row++) {
			for (int col = colOffset; col < colOffset + boxWidth; col++) {
				if (pattern.isCellCarved(row, col)) {
					int x1 = ox + (col - colOffset) * CELL;
					int y1 = oy + (row - rowOffset) * CELL;
					g.fill(x1, y1, x1 + CELL, y1 + CELL, fill);
				}
			}
		}
	}
}
