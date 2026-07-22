package tinytaru.socketsorcery.pattern;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * Sanity checks over the loaded pattern/modifier registries, run once at server start. Nothing here
 * is fatal — codec-level violations already failed the individual file at load — these catch the
 * mistakes that parse fine but play wrong, and log them with enough context to fix the datapack.
 */
public final class RegistryValidation {

	public static void init() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> validate(server.registryAccess()));
	}

	private static void validate(HolderLookup.Provider registries) {
		long patternCount = Patterns.all(registries).count();
		long modifierCount = Modifiers.all(registries).count();
		SocketSorcery.LOGGER.info("Loaded {} pattern(s) and {} modifier(s)", patternCount, modifierCount);

		List<Holder.Reference<Modifier>> modifiers = Modifiers.all(registries).toList();
		Map<ResourceLocation, ResourceLocation> scrollClaims = new HashMap<>();
		int[] checkedCombos = { 0 };
		int[] brokenPatterns = { 0 };

		Patterns.all(registries).forEach(holder -> {
			ResourceLocation id = holder.key().location();
			Pattern pattern = holder.value();

			for (ResourceLocation gemId : pattern.gems()) {
				if (!BuiltInRegistries.ITEM.containsKey(gemId)) {
					SocketSorcery.LOGGER.warn("Pattern {} lists unknown gem item {} (mod not installed?)", id, gemId);
				}
			}
			pattern.scroll().ifPresent(scrollId -> {
				if (!BuiltInRegistries.ITEM.containsKey(scrollId)) {
					SocketSorcery.LOGGER.warn("Pattern {} declares unknown scroll item {}", id, scrollId);
				} else {
					ResourceLocation previous = scrollClaims.put(scrollId, id);
					if (previous != null) {
						SocketSorcery.LOGGER.warn(
								"Patterns {} and {} both claim scroll item {} — the first found wins at the table",
								previous, id, scrollId);
					}
				}
			});

			if (checkRoundTrips(registries, id, pattern, modifiers, checkedCombos)) {
				brokenPatterns[0]++;
			}
		});

		SocketSorcery.LOGGER.info(
				"Engraving self-check: {} modifier combination(s) across all patterns round-trip correctly{}",
				checkedCombos[0], brokenPatterns[0] == 0 ? "" : " (" + brokenPatterns[0] + " pattern(s) had problems)");
	}

	/**
	 * The real correctness condition for the engraving minigame: every combination of a pattern's
	 * applicable modifiers must encode to a deep-cell mask that decodes back to <em>exactly</em> that
	 * combination. This catches the genuine break — one combination's cells accidentally containing
	 * another modifier's cells, so carving the first silently also applies the second — where a naive
	 * "do any two modifiers share a cell" test would false-positive on harmless overlaps. Returns true
	 * if this pattern had any broken combination.
	 */
	private static boolean checkRoundTrips(HolderLookup.Provider registries, ResourceLocation id, Pattern pattern,
			List<Holder.Reference<Modifier>> modifiers, int[] checkedCombos) {
		List<Holder.Reference<Modifier>> applicable = modifiers.stream()
				.filter(m -> m.value().cellMask(pattern) != null)
				.toList();
		int n = applicable.size();
		if (n > 16) { // 2^n guard; the built-ins are 7 — only a pathological datapack would exceed this
			SocketSorcery.LOGGER.info("Pattern {} has {} applicable modifiers; skipping exhaustive round-trip check", id, n);
			return false;
		}
		int problems = 0;
		for (int subset = 0; subset < (1 << n); subset++) {
			Set<ResourceLocation> want = new LinkedHashSet<>();
			for (int i = 0; i < n; i++) {
				if ((subset & (1 << i)) != 0) {
					want.add(applicable.get(i).key().location());
				}
			}
			long[] deep = Modifiers.cellsFor(registries, pattern, want);
			Set<ResourceLocation> got = Modifiers.decode(registries, pattern, deep);
			checkedCombos[0]++;
			if (got == null || !got.equals(want)) {
				problems++;
				if (problems <= 3) { // don't flood the log if a datapack is badly broken
					SocketSorcery.LOGGER.warn("On pattern {}, the modifier combination {} does not round-trip "
							+ "(engraving it decodes to {}) — those modifiers cannot be applied as intended", id, want, got);
				}
			}
		}
		return problems > 0;
	}

	private RegistryValidation() {
	}
}
