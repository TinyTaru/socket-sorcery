package tinytaru.socketsorcery.pattern.rule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;
import tinytaru.socketsorcery.pattern.ModifierCellRule;
import tinytaru.socketsorcery.pattern.Pattern;

/** One cell just beyond the symbol's extreme in a direction, centred on the bounding box. */
public record ExtensionRule(Direction direction) implements ModifierCellRule {

	public enum Direction implements StringRepresentable {
		UP("up"),
		DOWN("down"),
		LEFT("left"),
		RIGHT("right");

		public static final Codec<Direction> CODEC = StringRepresentable.fromEnum(Direction::values);

		private final String name;

		Direction(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final MapCodec<ExtensionRule> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Direction.CODEC.fieldOf("direction").forGetter(ExtensionRule::direction)
	).apply(instance, ExtensionRule::new));

	@Override
	public MapCodec<ExtensionRule> codec() {
		return CODEC;
	}

	@Override
	public int[] cells(Pattern pattern) {
		int[] box = RuleGeometry.bbox(pattern.mask());
		int midRow = (box[0] + box[1]) / 2;
		int midCol = (box[2] + box[3]) / 2;
		return new int[] {
				switch (direction) {
					case UP -> RuleGeometry.cell(box[0] - 1, midCol);
					case DOWN -> RuleGeometry.cell(box[1] + 1, midCol);
					case LEFT -> RuleGeometry.cell(midRow, box[2] - 1);
					case RIGHT -> RuleGeometry.cell(midRow, box[3] + 1);
				}
		};
	}
}
