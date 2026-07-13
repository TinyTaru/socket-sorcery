package tinytaru.socketsorcery.pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import tinytaru.socketsorcery.registry.ModRegistries;

/**
 * A modifier's cell derivation: given a pattern, which grid cells this modifier deepens. Rules are
 * geometric (derived from the pattern's symbol) so the same gesture works on every pattern.
 * Dispatched by type id from the {@code socket-sorcery:modifier_cell_rule_type} registry, which
 * other mods may extend at init.
 */
public interface ModifierCellRule {

	Codec<ModifierCellRule> CODEC = ModRegistries.CELL_RULE_TYPE.byNameCodec()
			.dispatch(ModifierCellRule::codec, mapCodec -> mapCodec);

	/** This rule's serializer, as registered in {@link ModRegistries#CELL_RULE_TYPE}. */
	MapCodec<? extends ModifierCellRule> codec();

	/** Cell indices ({@code row*GRID+col}) this rule deepens; any -1 means it can't apply here. */
	int[] cells(Pattern pattern);
}
