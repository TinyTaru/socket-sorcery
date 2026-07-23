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
	private static final int MARGIN = 4;

	private final Pattern pattern; // null when unresolvable — renders as empty

	public PatternTooltipRenderer(PatternTooltip tooltip) {
		Holder.Reference<Pattern> holder = Minecraft.getInstance().level == null ? null
				: Patterns.forScroll(Minecraft.getInstance().level.registryAccess(), tooltip.scroll());
		this.pattern = holder == null ? null : holder.value();
	}

	public static void register() {
		ClientTooltipComponentCallback.EVENT.register(data ->
				data instanceof PatternTooltip tooltip ? new PatternTooltipRenderer(tooltip) : null);
	}

	@Override
	public int getHeight(Font font) {
		return pattern == null ? 0 : SIZE + MARGIN * 2;
	}

	@Override
	public int getWidth(Font font) {
		return pattern == null ? 0 : SIZE + MARGIN * 2;
	}

	/** {@code renderImage} became {@code extractImage}, with width/height passed in and g moved last. */
	@Override
	public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor g) {
		if (pattern == null) {
			return;
		}
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
