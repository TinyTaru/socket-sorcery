package tinytaru.socketsorcery.item;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.Balance;
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

	private static final Map<ResourceLocation, Integer> BASE = Map.ofEntries(
			Map.entry(Patterns.LEAPING.id(), Balance.COOLDOWN_BASE_LEAPING),
			Map.entry(Patterns.WIND.id(), Balance.COOLDOWN_BASE_WIND),
			Map.entry(Patterns.FIRE.id(), Balance.COOLDOWN_BASE_FIRE),
			Map.entry(Patterns.HASTE.id(), Balance.COOLDOWN_BASE_HASTE),
			Map.entry(Patterns.FROST.id(), Balance.COOLDOWN_BASE_FROST),
			Map.entry(Patterns.SPIKES.id(), Balance.COOLDOWN_BASE_SPIKES),
			Map.entry(Patterns.LIFESTEAL.id(), Balance.COOLDOWN_BASE_LIFESTEAL),
			Map.entry(Patterns.HEALING.id(), Balance.COOLDOWN_BASE_HEALING),
			Map.entry(Patterns.EARTH.id(), Balance.COOLDOWN_BASE_EARTH),
			Map.entry(Patterns.BLINK.id(), Balance.COOLDOWN_BASE_BLINK),
			Map.entry(Patterns.LIGHTNING.id(), Balance.COOLDOWN_BASE_LIGHTNING));

	/** Total activation cooldown (ticks) for a bangle — the sum of each socketed gem's cost. */
	public static int forBangle(ItemStack bangle) {
		int total = 0;
		for (ItemStack gem : AccessoryItem.getSockets(bangle).gems()) {
			EngravingData data = gem.get(ModComponents.ENGRAVING);
			if (data == null) {
				continue;
			}
			int base = BASE.getOrDefault(data.pattern(), Balance.COOLDOWN_DEFAULT_BASE);
			double multiplier = 1.0 + Balance.COOLDOWN_MOD_SURCHARGE * data.modifiers().size();
			total += (int) Math.round(base * multiplier);
		}
		return total;
	}

	private Cooldowns() {
	}
}
