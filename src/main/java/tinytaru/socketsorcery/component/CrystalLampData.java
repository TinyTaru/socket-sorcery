package tinytaru.socketsorcery.component;

import java.util.Arrays;
import java.util.List;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.pattern.Pattern;

/** The five freeform masks held by a placed or item-form Crystal Lamp. */
public record CrystalLampData(long[] north, long[] east, long[] south, long[] west, long[] up) {

	public static final CrystalLampData EMPTY = new CrystalLampData(
			GridBits.empty(), GridBits.empty(), GridBits.empty(), GridBits.empty(), GridBits.empty());

	public CrystalLampData {
		north = normalize(north);
		east = normalize(east);
		south = normalize(south);
		west = normalize(west);
		up = normalize(up);
	}

	private static final Codec<long[]> MASK_CODEC = Codec.LONG.listOf()
			.xmap(CrystalLampData::toArray, CrystalLampData::toList);

	public static final Codec<CrystalLampData> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder
			.create(instance -> instance.group(
					MASK_CODEC.fieldOf("north").forGetter(CrystalLampData::north),
					MASK_CODEC.fieldOf("east").forGetter(CrystalLampData::east),
					MASK_CODEC.fieldOf("south").forGetter(CrystalLampData::south),
					MASK_CODEC.fieldOf("west").forGetter(CrystalLampData::west),
					MASK_CODEC.fieldOf("up").forGetter(CrystalLampData::up)
			).apply(instance, CrystalLampData::new));

	public static final StreamCodec<ByteBuf, CrystalLampData> STREAM_CODEC = StreamCodec.of(
				(buf, data) -> {
					writeMask(buf, data.north);
					writeMask(buf, data.east);
					writeMask(buf, data.south);
					writeMask(buf, data.west);
					writeMask(buf, data.up);
				},
				buf -> new CrystalLampData(readMask(buf), readMask(buf), readMask(buf), readMask(buf), readMask(buf)));

	public long[] mask(Direction direction) {
		return switch (direction) {
			case NORTH -> north;
			case EAST -> east;
			case SOUTH -> south;
			case WEST -> west;
			case UP -> up;
			case DOWN -> GridBits.empty();
		};
	}

	@Override
	public boolean equals(Object other) {
		return this == other || other instanceof CrystalLampData data
				&& Arrays.equals(north, data.north)
				&& Arrays.equals(east, data.east)
				&& Arrays.equals(south, data.south)
				&& Arrays.equals(west, data.west)
				&& Arrays.equals(up, data.up);
	}

	@Override
	public int hashCode() {
		int hash = Arrays.hashCode(north);
		hash = 31 * hash + Arrays.hashCode(east);
		hash = 31 * hash + Arrays.hashCode(south);
		hash = 31 * hash + Arrays.hashCode(west);
		return 31 * hash + Arrays.hashCode(up);
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
