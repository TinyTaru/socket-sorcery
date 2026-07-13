package tinytaru.socketsorcery.pattern.rule;

import com.mojang.serialization.MapCodec;

import tinytaru.socketsorcery.pattern.ModifierCellRule;
import tinytaru.socketsorcery.pattern.Pattern;

/** The symbol's top-centre and bottom-centre lit cells. */
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
		int midCol = (box[2] + box[3]) / 2;
		return new int[] {
				RuleGeometry.cell(box[0], RuleGeometry.nearestLitCol(mask, box[0], midCol)),
				RuleGeometry.cell(box[1], RuleGeometry.nearestLitCol(mask, box[1], midCol))
		};
	}
}
