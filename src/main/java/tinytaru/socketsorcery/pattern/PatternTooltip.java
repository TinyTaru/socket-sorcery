package tinytaru.socketsorcery.pattern;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;

/**
 * Tooltip image payload for a scroll: carries the scroll item itself; the client-side renderer
 * resolves it to a pattern (via the synced registry) and previews the symbol as a miniature grid.
 */
public record PatternTooltip(Item scroll) implements TooltipComponent {
}
