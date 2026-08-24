package tinytaru.socketsorcery.pattern.rule;

import java.util.LinkedHashSet;
import java.util.Set;

import com.mojang.serialization.MapCodec;

import tinytaru.socketsorcery.pattern.ModifierCellRule;
import tinytaru.socketsorcery.pattern.Pattern;

/** The symbol's top-centre and bottom-centre lit cells, including tied pixels around an even midpoint. */
public record TopBottomRule() implements ModifierCellRule {

	public static final TopBottomRule INSTANCE = new TopBottomRule();
	public static final MapCodec<TopBottomRule> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<TopBottomRule> codec() {
		return CODEC;
	}

	@Override
	public int[] cells(Pattern pattern) {
		boolean[][] mask = pattern.mask();
		int[] box = RuleGeometry.bbox(mask);
		Set<Integer> cells = new LinkedHashSet<>();
		int centerNumerator = box[2] + box[3];
		int[] rows = box[0] == box[1] ? new int[] { box[0] } : new int[] { box[0], box[1] };
		for (int row : rows) {
			for (int col : RuleGeometry.nearestLitCols(mask, row, centerNumerator)) {
				cells.add(RuleGeometry.idx(row, col));
			}
		}
		return cells.stream().mapToInt(Integer::intValue).toArray();
	}
}
