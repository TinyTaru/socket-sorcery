package tinytaru.socketsorcery.component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.item.ScrollInkColor;

/** The 16x16 ink layer on a Blank Scroll, stored as one bit per painted cell. */
public record ScrollDrawingData(long[] painted, Optional<ScrollInkColor> ink) {

	public ScrollDrawingData {
		painted = GridBits.copy(painted);
		ink = ink == null ? Optional.empty() : ink;
	}

	public ScrollDrawingData(long[] painted) {
		this(painted, Optional.empty());
	}

	public static final ScrollDrawingData EMPTY = new ScrollDrawingData(GridBits.empty());

	private static final Codec<long[]> MASK_CODEC = Codec.LONG.listOf()
			.xmap(ScrollDrawingData::toArray, ScrollDrawingData::toList);

	public static final Codec<ScrollDrawingData> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
			MASK_CODEC.fieldOf("painted").forGetter(ScrollDrawingData::painted),
			ScrollInkColor.CODEC.optionalFieldOf("ink").forGetter(ScrollDrawingData::ink)
	).apply(instance, ScrollDrawingData::new));
	public static final StreamCodec<ByteBuf, ScrollDrawingData> STREAM_CODEC = StreamCodec.of(
			(buf, data) -> {
				for (long word : data.painted) buf.writeLong(word);
				buf.writeBoolean(data.ink.isPresent());
				data.ink.ifPresent(color -> ScrollInkColor.STREAM_CODEC.encode(buf, color));
			},
			buf -> {
				long[] painted = new long[Pattern.WORDS];
				for (int i = 0; i < painted.length; i++) painted[i] = buf.readLong();
				Optional<ScrollInkColor> ink = buf.readBoolean()
						? Optional.of(ScrollInkColor.STREAM_CODEC.decode(buf)) : Optional.empty();
				return new ScrollDrawingData(painted, ink);
			});

	public boolean isPainted(int cell) {
		return cell >= 0 && cell < Pattern.GRID * Pattern.GRID && GridBits.getIndex(painted, cell);
	}

	public ScrollDrawingData paint(int cell, ScrollInkColor color) {
		long[] next = GridBits.copy(painted);
		GridBits.setIndex(next, cell);
		return new ScrollDrawingData(next, Optional.ofNullable(color));
	}

	@Override
	public boolean equals(Object other) {
		return this == other || other instanceof ScrollDrawingData data
				&& Arrays.equals(painted, data.painted) && ink.equals(data.ink);
	}

	@Override
	public int hashCode() {
		return 31 * Arrays.hashCode(painted) + ink.hashCode();
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
