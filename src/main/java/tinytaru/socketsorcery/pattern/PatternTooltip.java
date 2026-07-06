package tinytaru.socketsorcery.pattern;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Tooltip image payload carrying a pattern to preview as a miniature grid; rendered client-side. */
public record PatternTooltip(Pattern pattern) implements TooltipComponent {
}
