package tinytaru.socketsorcery.pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.util.ColorCodecs;

/**
 * A data-driven engraving modifier, loaded from datapack JSON into the synced
 * {@code socket-sorcery:modifier} dynamic registry. Its {@link ModifierCellRule} derives the exact
 * depth-2 cells from a pattern's symbol (so the same gesture works on every pattern), and its knob
 * deltas fold into {@link EngraveMods} when an engraved gem's effects run.
 *
 * <p>Modifiers don't know their own id — the registry provides it. Display names derive from the
 * id: {@code modifier.<namespace>.<path>}.
 */
public record Modifier(int color, ModifierCellRule cellRule,
		int powerBonus, int durationMultiplier, double rangeBonus, Vec3 aim) {

	public static final Codec<Modifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ColorCodecs.RGB.fieldOf("color").forGetter(Modifier::color),
			ModifierCellRule.CODEC.fieldOf("cell_rule").forGetter(Modifier::cellRule),
			Codec.INT.optionalFieldOf("power_bonus", 0).forGetter(Modifier::powerBonus),
			Codec.INT.optionalFieldOf("duration_multiplier", 1).forGetter(Modifier::durationMultiplier),
			Codec.DOUBLE.optionalFieldOf("range_bonus", 0.0).forGetter(Modifier::rangeBonus),
			Vec3.CODEC.optionalFieldOf("aim", Vec3.ZERO).forGetter(Modifier::aim)
	).apply(instance, Modifier::new));

	/** This modifier's cell mask for the pattern, or null if it can't be applied (an off-grid cell). */
	public long[] cellMask(Pattern pattern) {
		int[] cells = cellRule.cells(pattern);
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

	/** The translation key for a modifier id: {@code modifier.<namespace>.<path>}. */
	public static String translationKey(Identifier id) {
		return Util.makeDescriptionId("modifier", id);
	}

	public static Component displayName(Identifier id) {
		return Component.translatable(translationKey(id));
	}

	/** Display name for the given modifier id, tinted with this modifier's colour. */
	public MutableComponent coloredName(Identifier id) {
		return Component.translatable(translationKey(id)).withColor(color);
	}
}
