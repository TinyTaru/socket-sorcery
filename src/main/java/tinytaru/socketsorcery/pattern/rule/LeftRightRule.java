package tinytaru.socketsorcery.pattern.rule;

import java.util.LinkedHashSet;
import java.util.Set;

import com.mojang.serialization.MapCodec;

import tinytaru.socketsorcery.pattern.ModifierCellRule;
import tinytaru.socketsorcery.pattern.Pattern;

/** The symbol's left and right edges at its vertical-centre row(s) — centred and symmetric. */
public record LeftRightRule() implements ModifierCellRule {

	public static final LeftRightRule INSTANCE = new LeftRightRule();
	public static final MapCodec<LeftRightRule> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<LeftRightRule> codec() {
		return CODEC;
	}

	@Override
	public int[] cells(Pattern pattern) {
		boolean[][] mask = pattern.mask();
		int[] box = RuleGeometry.bbox(mask);
		int loRow = (box[0] + box[1]) / 2;
		int hiRow = (box[0] + box[1] + 1) / 2;
		Set<Integer> cells = new LinkedHashSet<>();
		for (int row : loRow == hiRow ? new int[] { loRow } : new int[] { loRow, hiRow }) {
			int left = RuleGeometry.leftmostLit(mask, row);
			int right = RuleGeometry.rightmostLit(mask, row);
			if (left >= 0) {
				cells.add(RuleGeometry.idx(row, left));
			}
			if (right >= 0) {
				cells.add(RuleGeometry.idx(row, right));
			}
		}
		return cells.stream().mapToInt(Integer::intValue).toArray();
	}
}
