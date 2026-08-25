package tinytaru.socketsorcery.component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import tinytaru.socketsorcery.item.ScrollInkColor;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.pattern.Pattern;

/** The 16x16 ink layers on a Blank Scroll, with one independent layer per pigment. */
public final class ScrollDrawingData {
	private static final int LAYER_COUNT = ScrollInkColor.values().length;
	private static final long[][] EMPTY_LAYERS = new long[LAYER_COUNT][Pattern.WORDS];
	private static final Codec<long[]> MASK_CODEC = Codec.LONG.listOf()
			.xmap(ScrollDrawingData::toArray, ScrollDrawingData::toList);
	private static final Codec<List<long[]>> LAYERS_CODEC = MASK_CODEC.listOf();

	private final long[] painted;
	private final long[][] inkLayers;

	public ScrollDrawingData(long[] painted) {
		this(painted, EMPTY_LAYERS);
	}

	private ScrollDrawingData(long[] painted, long[][] inkLayers) {
		this.painted = GridBits.copy(painted);
		this.inkLayers = copyLayers(inkLayers);
	}

	public static final ScrollDrawingData EMPTY = new ScrollDrawingData(GridBits.empty());

	/**
	 * The legacy {@code ink} field is read for old worlds and expanded into one complete layer. New
	 * drawings write the {@code inks} layers, preserving every pigment used on the page.
	 */
	public static final Codec<ScrollDrawingData> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
			MASK_CODEC.fieldOf("painted").forGetter(ScrollDrawingData::painted),
			LAYERS_CODEC.optionalFieldOf("inks", List.of()).forGetter(ScrollDrawingData::layersForCodec),
			ScrollInkColor.CODEC.optionalFieldOf("ink").forGetter(data -> Optional.<ScrollInkColor>empty())
	).apply(instance, (painted, layers, legacyInk) -> new ScrollDrawingData(painted,
			layers.isEmpty() ? legacyLayers(painted, legacyInk) : layersFromCodec(layers))));

	public static final StreamCodec<ByteBuf, ScrollDrawingData> STREAM_CODEC = StreamCodec.of(
			(buf, data) -> {
				writeMask(buf, data.painted);
				for (long[] layer : data.inkLayers) writeMask(buf, layer);
			},
			buf -> {
				long[] painted = readMask(buf);
				long[][] layers = new long[LAYER_COUNT][Pattern.WORDS];
				for (int i = 0; i < layers.length; i++) layers[i] = readMask(buf);
				return new ScrollDrawingData(painted, layers);
			});

	public boolean isPainted(int cell) {
		return cell >= 0 && cell < Pattern.GRID * Pattern.GRID && GridBits.getIndex(painted, cell);
	}

	/** The pigment used for this cell, or null for legacy/malformed uncoloured marks. */
	public ScrollInkColor inkAt(int cell) {
		if (!isPainted(cell)) return null;
		for (ScrollInkColor color : ScrollInkColor.values()) {
			if (GridBits.getIndex(inkLayers[color.ordinal()], cell)) return color;
		}
		return null;
	}

	public ScrollDrawingData paint(int cell, ScrollInkColor color) {
		long[] nextPainted = GridBits.copy(painted);
		long[][] nextLayers = copyLayers(inkLayers);
		GridBits.setIndex(nextPainted, cell);
		GridBits.setIndex(nextLayers[color.ordinal()], cell);
		return new ScrollDrawingData(nextPainted, nextLayers);
	}

	/** Stable cache key for the dynamically rendered scroll icon. */
	public String cacheKey() {
		return Arrays.toString(painted) + "|" + Arrays.deepToString(inkLayers);
	}

	/** The painted mask, copied so callers cannot mutate the component's stored state. */
	public long[] painted() {
		return GridBits.copy(painted);
	}

	private List<long[]> layersForCodec() {
		return Arrays.stream(inkLayers).map(GridBits::copy).toList();
	}

	@Override
	public boolean equals(Object other) {
		return this == other || other instanceof ScrollDrawingData data
				&& Arrays.equals(painted, data.painted) && Arrays.deepEquals(inkLayers, data.inkLayers);
	}

	@Override
	public int hashCode() {
		return 31 * Arrays.hashCode(painted) + Arrays.deepHashCode(inkLayers);
	}

	private static long[][] legacyLayers(long[] painted, Optional<ScrollInkColor> legacyInk) {
		long[][] layers = copyLayers(EMPTY_LAYERS);
		legacyInk.ifPresent(color -> layers[color.ordinal()] = GridBits.copy(painted));
		return layers;
	}

	private static long[][] layersFromCodec(List<long[]> source) {
		long[][] layers = copyLayers(EMPTY_LAYERS);
		for (int i = 0; i < layers.length && i < source.size(); i++) {
			layers[i] = GridBits.copy(source.get(i));
		}
		return layers;
	}

	private static long[][] copyLayers(long[][] source) {
		long[][] copy = new long[LAYER_COUNT][Pattern.WORDS];
		for (int i = 0; i < copy.length && i < source.length; i++) {
			copy[i] = GridBits.copy(source[i]);
		}
		return copy;
	}

	private static void writeMask(ByteBuf buf, long[] mask) {
		for (long word : mask) buf.writeLong(word);
	}

	private static long[] readMask(ByteBuf buf) {
		long[] mask = new long[Pattern.WORDS];
		for (int i = 0; i < mask.length; i++) mask[i] = buf.readLong();
		return mask;
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
