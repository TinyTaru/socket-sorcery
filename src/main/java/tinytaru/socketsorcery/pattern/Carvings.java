package tinytaru.socketsorcery.pattern;

import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.component.CarvingData;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.registry.ModComponents;

/**
 * Reads the carve physically cut into a gem. Two components can describe one: an in-progress
 * {@link CarvingData} while the cells don't yet form a finished engraving, or an
 * {@link EngravingData} once they do (the two layers are re-derivable from the pattern's symbol plus
 * its modifiers' cells, so a finished gem carries no carving component). Both sides read a gem's
 * grid through here, so the Engraving Table screen draws exactly what the item holds.
 */
public final class Carvings {

	/** The pattern a gem is carved for — finished or not — or null if it carries no carve. */
	public static Identifier patternId(ItemStack stack) {
		EngravingData engraving = stack.get(ModComponents.ENGRAVING);
		if (engraving != null) {
			return engraving.pattern();
		}
		CarvingData carving = stack.get(ModComponents.CARVING);
		return carving == null ? null : carving.pattern();
	}

	/** The carve on a gem, or null if it is uncut (or carries a pattern this world doesn't know). */
	public static CarvingData on(HolderLookup.Provider registries, ItemStack stack) {
		CarvingData carving = stack.get(ModComponents.CARVING);
		if (carving != null) {
			return carving;
		}
		EngravingData engraving = stack.get(ModComponents.ENGRAVING);
		Holder.Reference<Pattern> holder = engraving == null ? null : Patterns.get(registries, engraving.pattern());
		if (holder == null) {
			return null;
		}
		long[] deep = Modifiers.cellsFor(registries, holder.value(), engraving.modifiers());
		return new CarvingData(engraving.pattern(), GridBits.or(holder.value().maskBits(), deep), deep);
	}

	/** How deep a single cell is cut: 0 bare, 1 carved, 2 deep. */
	public static int depth(long[] carved, long[] deep, int cell) {
		if (GridBits.getIndex(deep, cell)) {
			return 2;
		}
		return GridBits.getIndex(carved, cell) ? 1 : 0;
	}

	/**
	 * The modifier set these two layers form as pure geometry, or null if the deep cells are stray or
	 * incomplete, or the carved layer isn't exactly the symbol plus those deep cells. An empty set
	 * means a well-formed base engraving with no modifiers — very different from null, so callers
	 * must test for null rather than emptiness.
	 */
	public static Set<Identifier> formedModifiers(HolderLookup.Provider registries, Pattern pattern,
			long[] carved, long[] deep) {
		if (pattern.ringTrigger().isPresent()) {
			return pattern.matchesCarved(carved) && GridBits.isEmpty(deep) ? Set.of() : null;
		}
		Set<Identifier> modifiers = Modifiers.decode(registries, pattern, deep);
		if (modifiers == null || !GridBits.equal(carved, GridBits.or(pattern.maskBits(), deep))) {
			return null;
		}
		return modifiers;
	}

	/**
	 * As {@link #formedModifiers}, but also null when the pattern refuses one of the modifiers formed
	 * — i.e. non-null exactly when this carve is a finished engraving the table will accept.
	 */
	public static Set<Identifier> acceptedModifiers(HolderLookup.Provider registries, Pattern pattern,
			long[] carved, long[] deep) {
		Set<Identifier> modifiers = formedModifiers(registries, pattern, carved, deep);
		if (modifiers == null || !Modifiers.incompatible(pattern, modifiers).isEmpty()) {
			return null;
		}
		return modifiers;
	}

	private Carvings() {
	}
}
