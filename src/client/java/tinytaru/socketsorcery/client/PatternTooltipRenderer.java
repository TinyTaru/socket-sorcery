package tinytaru.socketsorcery.client;

import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.PatternTooltip;

/** Renders a {@link PatternTooltip} as a miniature version of the engraving grid, tinted with the pattern's colour. */
public class PatternTooltipRenderer implements ClientTooltipComponent {

	private static final int CELL = 6;
	private static final int SIZE = Pattern.GRID * CELL;
	private static final int MARGIN = 4;

	private final Pattern pattern;

	public PatternTooltipRenderer(Pattern pattern) {
		this.pattern = pattern;
	}

	public static void register() {
		TooltipComponentCallback.EVENT.register(data ->
				data instanceof PatternTooltip tooltip ? new PatternTooltipRenderer(tooltip.pattern()) : null);
	}

	@Override
	public int getHeight() {
		return SIZE + MARGIN * 2;
	}

	@Override
	public int getWidth(Font font) {
		return SIZE + MARGIN * 2;
	}

	@Override
	public void renderImage(Font font, int x, int y, GuiGraphics g) {
		int ox = x + MARGIN;
		int oy = y + MARGIN;
		g.fill(ox - 2, oy - 2, ox + SIZE + 2, oy + SIZE + 2, 0xFF101010);

		int fill = 0xFF000000 | (pattern.color() & 0xFFFFFF);
		for (int row = 0; row < Pattern.GRID; row++) {
			for (int col = 0; col < Pattern.GRID; col++) {
				if (pattern.isCellCarved(row, col)) {
					int x1 = ox + col * CELL;
					int y1 = oy + row * CELL;
					g.fill(x1, y1, x1 + CELL, y1 + CELL, fill);
				}
			}
		}
	}
}
