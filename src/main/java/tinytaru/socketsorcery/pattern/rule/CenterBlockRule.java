package tinytaru.socketsorcery.pattern.rule;

import com.mojang.serialization.MapCodec;

import tinytaru.socketsorcery.pattern.ModifierCellRule;
import tinytaru.socketsorcery.pattern.Pattern;

/** The four cells straddling the grid centre (a 16x16 grid has no single centre cell). */
public record CenterBlockRule() implements ModifierCellRule {

	public static final CenterBlockRule INSTANCE = new CenterBlockRule();
	public static final MapCodec<CenterBlockRule> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<CenterBlockRule> codec() {
		return CODEC;
	}

	@Override
	public int[] cells(Pattern pattern) {
		int lo = RuleGeometry.GRID / 2 - 1;
		int hi = RuleGeometry.GRID / 2;
		return new int[] {
				RuleGeometry.idx(lo, lo), RuleGeometry.idx(lo, hi),
				RuleGeometry.idx(hi, lo), RuleGeometry.idx(hi, hi)
		};
	}
}
