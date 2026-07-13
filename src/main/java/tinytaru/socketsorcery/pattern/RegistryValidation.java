package tinytaru.socketsorcery.pattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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

		Map<ResourceLocation, ResourceLocation> scrollClaims = new HashMap<>();
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

			// Modifier cell sets must stay pairwise disjoint per pattern, or decode() loses its
			// unique-union guarantee and some combinations become uncarvable.
			List<Pair<ResourceLocation, long[]>> masks = new ArrayList<>();
			Modifiers.all(registries).forEach(modifier -> {
				long[] cells = modifier.value().cellMask(pattern);
				if (cells != null) {
					masks.add(Pair.of(modifier.key().location(), cells));
				}
			});
			for (int a = 0; a < masks.size(); a++) {
				for (int b = a + 1; b < masks.size(); b++) {
					if (intersects(masks.get(a).getSecond(), masks.get(b).getSecond())) {
						SocketSorcery.LOGGER.warn("On pattern {}, modifiers {} and {} overlap cells — "
										+ "combinations of them cannot be engraved",
								id, masks.get(a).getFirst(), masks.get(b).getFirst());
					}
				}
			}
		});
	}

	private static boolean intersects(long[] a, long[] b) {
		for (int i = 0; i < Pattern.WORDS; i++) {
			if ((a[i] & b[i]) != 0) {
				return true;
			}
		}
		return false;
	}

	private RegistryValidation() {
	}
}
