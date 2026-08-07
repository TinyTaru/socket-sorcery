package tinytaru.socketsorcery.component;

import java.util.Arrays;
import java.util.List;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.pattern.Pattern;

/** The 16x16 ink layer on a Blank Scroll, stored as one bit per painted cell. */
public record ScrollDrawingData(long[] painted) {

	public ScrollDrawingData {
		painted = GridBits.copy(painted);
	}

	public static final ScrollDrawingData EMPTY = new ScrollDrawingData(GridBits.empty());

	private static final Codec<long[]> MASK_CODEC = Codec.LONG.listOf()
			.xmap(ScrollDrawingData::toArray, ScrollDrawingData::toList);

	public static final Codec<ScrollDrawingData> CODEC = MASK_CODEC.xmap(ScrollDrawingData::new, ScrollDrawingData::painted);
	public static final StreamCodec<ByteBuf, ScrollDrawingData> STREAM_CODEC = StreamCodec.of(
			(buf, data) -> {
				for (long word : data.painted) buf.writeLong(word);
			},
			buf -> {
				long[] painted = new long[Pattern.WORDS];
				for (int i = 0; i < painted.length; i++) painted[i] = buf.readLong();
				return new ScrollDrawingData(painted);
			});

	public boolean isPainted(int cell) {
		return cell >= 0 && cell < Pattern.GRID * Pattern.GRID && GridBits.getIndex(painted, cell);
	}

	public ScrollDrawingData paint(int cell) {
		long[] next = GridBits.copy(painted);
		GridBits.setIndex(next, cell);
		return new ScrollDrawingData(next);
	}

	@Override
	public boolean equals(Object other) {
		return this == other || other instanceof ScrollDrawingData data && Arrays.equals(painted, data.painted);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(painted);
	}

	private static long[] toArray(List<Long> words) {
		long[] result = new long[Pattern.WORDS];
		for (int i = 0; i < result.length && i < words.size(); i++) result[i] = words.get(i);
		return result;
	}

	private static List<Long> toList(long[] words) {
		return Arrays.stream(words).boxed().toList();
	}
}
