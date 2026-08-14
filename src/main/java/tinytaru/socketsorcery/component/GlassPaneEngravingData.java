package tinytaru.socketsorcery.component;

import java.util.Arrays;
import java.util.List;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.pattern.Pattern;

/** A freeform 16×16 mask chiselled into a glass pane for use in a Crystal Lamp. */
public record GlassPaneEngravingData(long[] etched) {

	public static final GlassPaneEngravingData EMPTY = new GlassPaneEngravingData(GridBits.empty());

	public GlassPaneEngravingData {
		etched = normalize(etched);
	}

	private static final Codec<long[]> MASK_CODEC = Codec.LONG.listOf()
			.xmap(GlassPaneEngravingData::toArray, GlassPaneEngravingData::toList);

	public static final Codec<GlassPaneEngravingData> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder
			.create(instance -> instance.group(
					MASK_CODEC.fieldOf("etched").forGetter(GlassPaneEngravingData::etched)
			).apply(instance, GlassPaneEngravingData::new));

	public static final StreamCodec<ByteBuf, GlassPaneEngravingData> STREAM_CODEC = StreamCodec.of(
				(buf, data) -> writeMask(buf, data.etched),
				buf -> new GlassPaneEngravingData(readMask(buf)));

	public boolean isEtched(int cell) {
		return cell >= 0 && cell < Pattern.GRID * Pattern.GRID && GridBits.getIndex(etched, cell);
	}

	public boolean isEmpty() {
		return GridBits.isEmpty(etched);
	}

	public GlassPaneEngravingData etch(int cell) {
		long[] next = GridBits.copy(etched);
		GridBits.setIndex(next, cell);
		return new GlassPaneEngravingData(next);
	}

	public GlassPaneEngravingData erase(int cell) {
		long[] next = GridBits.copy(etched);
		GridBits.clearIndex(next, cell);
		return new GlassPaneEngravingData(next);
	}

	@Override
	public boolean equals(Object other) {
		return this == other || other instanceof GlassPaneEngravingData data
				&& Arrays.equals(etched, data.etched);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(etched);
	}

	private static long[] normalize(long[] mask) {
		long[] out = new long[Pattern.WORDS];
		if (mask != null) {
			System.arraycopy(mask, 0, out, 0, Math.min(mask.length, Pattern.WORDS));
		}
		return out;
	}

	private static List<Long> toList(long[] mask) {
		return Arrays.stream(mask).boxed().toList();
	}

	private static long[] toArray(List<Long> words) {
		long[] out = new long[Pattern.WORDS];
		for (int i = 0; i < out.length && i < words.size(); i++) {
			out[i] = words.get(i);
		}
		return out;
	}

	private static void writeMask(ByteBuf buf, long[] mask) {
		for (long word : mask) {
			buf.writeLong(word);
		}
	}

	private static long[] readMask(ByteBuf buf) {
		long[] mask = new long[Pattern.WORDS];
		for (int i = 0; i < mask.length; i++) {
			mask[i] = buf.readLong();
		}
		return mask;
	}
}
