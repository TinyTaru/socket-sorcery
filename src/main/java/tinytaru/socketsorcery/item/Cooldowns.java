package tinytaru.socketsorcery.item;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;

/**
 * Computes a bangle's activation cooldown from what's socketed: one cooldown for the whole activation
 * (the keybind fires every gem at once), summed across the socketed gems. Each gem costs its
 * pattern's base, surcharged per modifier — so more gems, stronger modifiers and heavier patterns all
 * lengthen the cooldown.
 */
public final class Cooldowns {

	private static final int DEFAULT_BASE = 40;
	private static final double MOD_SURCHARGE = 0.4;

	private static final Map<ResourceLocation, Integer> BASE = Map.of(
			Patterns.LEAPING.id(), 20,
			Patterns.FIRE.id(), 40,
			Patterns.FROST.id(), 50,
			Patterns.HEALING.id(), 60,
			Patterns.LIGHTNING.id(), 100);

	/** Total activation cooldown (ticks) for a bangle — the sum of each socketed gem's cost. */
	public static int forBangle(ItemStack bangle) {
		int total = 0;
		for (ItemStack gem : AccessoryItem.getSockets(bangle).gems()) {
			EngravingData data = gem.get(ModComponents.ENGRAVING);
			if (data == null) {
				continue;
			}
			int base = BASE.getOrDefault(data.pattern(), DEFAULT_BASE);
			double multiplier = 1.0 + MOD_SURCHARGE * data.modifiers().size();
			total += (int) Math.round(base * multiplier);
		}
		return total;
	}

	private Cooldowns() {
	}
}
