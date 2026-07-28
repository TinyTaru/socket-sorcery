package tinytaru.socketsorcery.item;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.Balance;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.config.SocketSorceryConfig;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;

/**
 * Computes an accessory's activation cooldown from what's socketed: one cooldown for the whole
 * activation, summed across the socketed gems. Each gem costs its pattern's base (from the pattern's
 * data definition), surcharged per modifier — so more gems, stronger modifiers and heavier patterns
 * all lengthen the cooldown.
 */
public final class Cooldowns {

	/** Total activation cooldown (ticks) — the sum of each socketed gem's cost. */
	public static int forBangle(ItemStack bangle, HolderLookup.Provider registries) {
		int total = 0;
		for (ItemStack gem : AccessoryItem.getSockets(bangle).gems()) {
			EngravingData data = gem.get(ModComponents.ENGRAVING);
			if (data == null) {
				continue;
			}
			Holder.Reference<Pattern> pattern = Patterns.get(registries, data.pattern());
			int base = pattern != null ? pattern.value().cooldown() : Balance.COOLDOWN_DEFAULT_BASE;
			double multiplier = 1.0 + Balance.COOLDOWN_MOD_SURCHARGE * data.modifiers().size();
			total += (int) Math.round(base * multiplier);
		}
		double tierReduction = bangle.getItem() instanceof BangleItem item ? item.cooldownReduction() : 0.0;
		return (int) Math.round(total * (1.0 - tierReduction) * SocketSorceryConfig.get().cooldownMultiplier);
	}

	private Cooldowns() {
	}
}
