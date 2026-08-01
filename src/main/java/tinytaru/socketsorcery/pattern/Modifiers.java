package tinytaru.socketsorcery.pattern;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.registry.ModRegistries;

/**
 * Lookup helpers over the synced {@code socket-sorcery:modifier} dynamic registry, plus the
 * routines that decode a deep-layer mask into a modifier set and fold a modifier set into
 * {@link EngraveMods}. Null-tolerant like {@link Patterns}.
 */
public final class Modifiers {

	/** The modifier holder for an id, or null if unavailable. */
	public static Holder.Reference<Modifier> get(HolderLookup.Provider registries, Identifier id) {
		if (registries == null || id == null) {
			return null;
		}
		return registries.lookup(ModRegistries.MODIFIER)
				.flatMap(lookup -> lookup.get(ResourceKey.create(ModRegistries.MODIFIER, id)))
				.orElse(null);
	}

	/** Every registered modifier (empty when registries are unavailable). */
	public static Stream<Holder.Reference<Modifier>> all(HolderLookup.Provider registries) {
		if (registries == null) {
			return Stream.empty();
		}
		return registries.lookup(ModRegistries.MODIFIER)
				.map(HolderLookup::listElements)
				.orElse(Stream.empty());
	}

	/**
	 * Decodes the deep-layer mask for {@code pattern} into the set of applied modifiers, or null if
	 * the mask isn't an exact disjoint union of complete modifier cell sets. Empty decodes to empty
	 * (a plain base engraving).
	 */
	public static Set<Identifier> decode(HolderLookup.Provider registries, Pattern pattern, long[] deep) {
		Set<Identifier> applied = formedSubset(registries, pattern, deep);
		return GridBits.equal(cellsFor(registries, pattern, applied), deep) ? applied : null;
	}

	/**
	 * The modifiers fully embedded in {@code deep}, tolerating stray or otherwise-incomplete cells
	 * alongside them — unlike {@link #decode}, this doesn't require {@code deep} to be exactly their
	 * union. Meant for asking "which modifiers were already standing" mid-carve, when an unrelated
	 * half-cut modifier elsewhere in {@code deep} would otherwise make {@link #decode} fail outright
	 * and hide a different one that's already whole.
	 */
	public static Set<Identifier> formedSubset(HolderLookup.Provider registries, Pattern pattern, long[] deep) {
		Set<Identifier> applied = new LinkedHashSet<>();
		all(registries).forEach(holder -> {
			long[] cells = holder.value().cellMask(pattern);
			if (cells != null && GridBits.subset(cells, deep)) {
				applied.add(holder.key().identifier());
			}
		});
		return applied;
	}

	/**
	 * The subset of {@code ids} the pattern refuses (see {@link Pattern#allows}), in display order.
	 * Empty for an acceptable set — the engraving path treats a non-empty result as a hard refusal
	 * rather than filtering the offenders out, so the player is told what went wrong.
	 */
	public static List<Identifier> incompatible(Pattern pattern, Collection<Identifier> ids) {
		return ordered(ids).stream().filter(id -> !pattern.allows(id)).toList();
	}

	/** The full deep mask an applied modifier set occupies for a pattern. */
	public static long[] cellsFor(HolderLookup.Provider registries, Pattern pattern, Collection<Identifier> ids) {
		long[] mask = GridBits.empty();
		for (Identifier id : ids) {
			Holder.Reference<Modifier> modifier = get(registries, id);
			if (modifier != null) {
				long[] cells = modifier.value().cellMask(pattern);
				if (cells != null) {
					GridBits.orInto(mask, cells);
				}
			}
		}
		return mask;
	}

	/** Folds a modifier set into the effect knobs. Unknown ids are skipped. */
	public static EngraveMods toMods(HolderLookup.Provider registries, Collection<Identifier> ids) {
		int power = 0;
		int durationMult = 1;
		double rangeBonus = 0.0;
		Vec3 aim = Vec3.ZERO;
		for (Identifier id : ids) {
			Holder.Reference<Modifier> holder = get(registries, id);
			if (holder == null) {
				continue;
			}
			Modifier modifier = holder.value();
			power += modifier.powerBonus();
			durationMult *= modifier.durationMultiplier();
			rangeBonus += modifier.rangeBonus();
			aim = aim.add(modifier.aim());
		}
		return new EngraveMods(power, durationMult, rangeBonus, aim);
	}

	/**
	 * Canonical display/storage order for a modifier set: sorted by id. Stable across any datapack
	 * combination (unlike the old Java-registration order).
	 */
	public static List<Identifier> ordered(Collection<Identifier> ids) {
		return ids.stream().distinct().sorted().toList();
	}

	private Modifiers() {
	}
}
