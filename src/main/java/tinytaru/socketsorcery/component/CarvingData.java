package tinytaru.socketsorcery.component;

import java.util.Arrays;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import tinytaru.socketsorcery.pattern.Pattern;

/**
 * Data component value stored on a gem that is part-way through being chiselled: the pattern being
 * carved and the two cell layers of the carve itself — {@code carved} (depth ≥ 1) and {@code deep}
 * (depth 2, a subset of {@code carved}), packed one bit per cell into {@link Pattern#WORDS} longs.
 *
 * <p>Every chisel stroke at the Engraving Table rewrites this component, so a half-finished carve
 * survives being pulled out of the table. It is present only while the carve is <em>not</em> a
 * finished engraving: the moment the cells form a complete, accepted pattern the table swaps it for
 * an {@link EngravingData}, from which the same two layers can be derived again.
 */
public record CarvingData(Identifier pattern, long[] carved, long[] deep) {

	public CarvingData {
		carved = normalize(carved);
		deep = normalize(deep);
	}

	private static final Codec<long[]> MASK_CODEC =
			Codec.LONG.listOf().xmap(CarvingData::toArray, CarvingData::toList);

	public static final Codec<CarvingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("pattern").forGetter(CarvingData::pattern),
			MASK_CODEC.fieldOf("carved").forGetter(CarvingData::carved),
			MASK_CODEC.fieldOf("deep").forGetter(CarvingData::deep)
	).apply(instance, CarvingData::new));

	public static final StreamCodec<ByteBuf, CarvingData> STREAM_CODEC = StreamCodec.of(
			(buf, data) -> {
				Identifier.STREAM_CODEC.encode(buf, data.pattern());
				writeMask(buf, data.carved());
				writeMask(buf, data.deep());
			},
			buf -> new CarvingData(Identifier.STREAM_CODEC.decode(buf), readMask(buf), readMask(buf)));

	/** Total chisel strokes this carve represents — one per carved cell, two per deep cell. */
	public int strokes() {
		int n = 0;
		for (long word : carved) {
			n += Long.bitCount(word);
		}
		for (long word : deep) {
			n += Long.bitCount(word);
		}
		return n;
	}

	// Records compare arrays by identity, which would make two equal carves look different to the
	// component map — and make every slot sync think the gem changed. Compare the cells instead.

	@Override
	public boolean equals(Object other) {
		return this == other || other instanceof CarvingData data
				&& pattern.equals(data.pattern)
				&& Arrays.equals(carved, data.carved)
				&& Arrays.equals(deep, data.deep);
	}

	@Override
	public int hashCode() {
		return (pattern.hashCode() * 31 + Arrays.hashCode(carved)) * 31 + Arrays.hashCode(deep);
	}

	private static long[] normalize(long[] mask) {
		long[] out = new long[Pattern.WORDS];
		if (mask != null) {
			System.arraycopy(mask, 0, out, 0, Math.min(mask.length, Pattern.WORDS));
		}
		return out;
	}

	private static long[] toArray(List<Long> words) {
		long[] out = new long[Pattern.WORDS];
		for (int i = 0; i < Pattern.WORDS && i < words.size(); i++) {
			out[i] = words.get(i);
		}
		return out;
	}

	private static List<Long> toList(long[] mask) {
		return Arrays.stream(mask).boxed().toList();
	}

	private static void writeMask(ByteBuf buf, long[] mask) {
		for (int i = 0; i < Pattern.WORDS; i++) {
			buf.writeLong(mask[i]);
		}
	}

	private static long[] readMask(ByteBuf buf) {
		long[] mask = new long[Pattern.WORDS];
		for (int i = 0; i < Pattern.WORDS; i++) {
			mask[i] = buf.readLong();
		}
		return mask;
	}
}
