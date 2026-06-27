package tinytaru.socketsorcery.pattern;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * An engraving modifier: a fixed set of depth-2 cells derived from a pattern's symbol via a
 * standardized rule, plus a display identity. Applying the modifier means chiselling exactly those
 * cells to depth 2. Modifier cell sets are pairwise disjoint, so any combination decodes uniquely.
 */
public record Modifier(ResourceLocation id, String translationKey, int color, CellRule rule) {

	@FunctionalInterface
	public interface CellRule {
		/** Cell indices ({@code row*GRID+col}) this modifier deepens; a -1 means it can't apply here. */
		int[] cells(Pattern pattern);
	}

	/** This modifier's cell mask for the pattern, or null if it can't be applied (an off-grid cell). */
	public long[] cellMask(Pattern pattern) {
		int[] cells = rule.cells(pattern);
		if (cells.length == 0) {
			return null;
		}
		long[] mask = GridBits.empty();
		for (int index : cells) {
			if (index < 0) {
				return null;
			}
			GridBits.setIndex(mask, index);
		}
		return mask;
	}

	public Component displayName() {
		return Component.translatable(translationKey);
	}

	public MutableComponent coloredName() {
		return Component.translatable(translationKey).withColor(color);
	}
}
