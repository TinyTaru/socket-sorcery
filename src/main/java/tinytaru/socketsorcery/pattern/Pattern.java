package tinytaru.socketsorcery.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import tinytaru.socketsorcery.item.ScrollInkColor;
import tinytaru.socketsorcery.util.ColorCodecs;

/**
 * A data-driven engravable pattern, loaded from datapack JSON into the synced
 * {@code socket-sorcery:pattern} dynamic registry (see {@code ModRegistries}). Bundles the 16x16
 * symbol the player must chisel, a display colour, the bangle cooldown base, optional activation
 * feedback, the items that can hold/teach it, and its effect components (passive list for the
 * necklace tick, active list for bangle/ring activation).
 *
 * <p>Patterns don't know their own id — the registry provides it (as with vanilla enchantments).
 * Display names derive from the id: {@code pattern.<namespace>.<path>}.
 */
public record Pattern(boolean[][] mask, int color, ScrollInkColor ink, int cooldown, Optional<CastFeedback> castFeedback,
		List<Identifier> gems, Optional<Identifier> scroll, List<Identifier> incompatibleModifiers,
		List<PatternEffectComponent> necklaceEffects, List<PatternEffectComponent> bangleEffects,
		Optional<RingTrigger> ringTrigger) {

	/** Edge length of the chiselling grid — one cell per pixel of the 16x16 gem texture. */
	public static final int GRID = 16;

	/** Number of 64-bit words needed to hold the {@code GRID*GRID}-bit mask. */
	public static final int WORDS = (GRID * GRID + 63) / 64;

	private static final Codec<boolean[][]> MASK_CODEC =
			Codec.STRING.listOf().comapFlatMap(Pattern::parseMask, Pattern::formatMask);

	public static final Codec<Pattern> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			MASK_CODEC.fieldOf("mask").forGetter(Pattern::mask),
			ColorCodecs.RGB.fieldOf("color").forGetter(Pattern::color),
			ScrollInkColor.CODEC.optionalFieldOf("ink", ScrollInkColor.RED).forGetter(Pattern::ink),
			ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cooldown").forGetter(Pattern::cooldown),
			CastFeedback.CODEC.optionalFieldOf("cast_feedback").forGetter(Pattern::castFeedback),
			Identifier.CODEC.listOf().optionalFieldOf("gems", List.of()).forGetter(Pattern::gems),
			Identifier.CODEC.optionalFieldOf("scroll").forGetter(Pattern::scroll),
			Identifier.CODEC.listOf().optionalFieldOf("incompatible_modifiers", List.of()).forGetter(Pattern::incompatibleModifiers),
			PatternEffectComponent.CODEC.listOf().optionalFieldOf("necklace_effects", List.of()).forGetter(Pattern::necklaceEffects),
			PatternEffectComponent.CODEC.listOf().optionalFieldOf("bangle_effects", List.of()).forGetter(Pattern::bangleEffects),
			RingTrigger.CODEC.optionalFieldOf("ring_trigger").forGetter(Pattern::ringTrigger)
	).apply(instance, Pattern::new));

	/** Builds a {@code GRID x GRID} mask from rows of text where '#' marks a carved cell. */
	public static boolean[][] mask(String... rows) {
		boolean[][] m = new boolean[GRID][GRID];
		for (int r = 0; r < GRID; r++) {
			String row = r < rows.length ? rows[r] : "";
			for (int c = 0; c < GRID; c++) {
				m[r][c] = c < row.length() && row.charAt(c) == '#';
			}
		}
		return m;
	}

	private static DataResult<boolean[][]> parseMask(List<String> rows) {
		if (rows.size() != GRID) {
			return DataResult.error(() -> "Pattern mask must have exactly " + GRID + " rows, got " + rows.size());
		}
		boolean any = false;
		for (int r = 0; r < GRID; r++) {
			String row = rows.get(r);
			if (row.length() != GRID) {
				int index = r;
				return DataResult.error(() -> "Pattern mask row " + index + " must be exactly " + GRID + " chars");
			}
			for (int c = 0; c < GRID; c++) {
				char ch = row.charAt(c);
				if (ch == '#') {
					any = true;
				} else if (ch != '.') {
					int index = r;
					return DataResult.error(() -> "Pattern mask row " + index + " may only contain '.' and '#'");
				}
			}
		}
		if (!any) {
			return DataResult.error(() -> "Pattern mask must carve at least one cell");
		}
		return DataResult.success(mask(rows.toArray(String[]::new)));
	}

	private static List<String> formatMask(boolean[][] mask) {
		List<String> rows = new ArrayList<>(GRID);
		for (int r = 0; r < GRID; r++) {
			StringBuilder row = new StringBuilder(GRID);
			for (int c = 0; c < GRID; c++) {
				row.append(mask[r][c] ? '#' : '.');
			}
			rows.add(row.toString());
		}
		return rows;
	}

	/** The translation key for a pattern id: {@code pattern.<namespace>.<path>}. */
	public static String translationKey(Identifier id) {
		return Util.makeDescriptionId("pattern", id);
	}

	public static Component displayName(Identifier id) {
		return Component.translatable(translationKey(id));
	}

	/** Display name for the given pattern id, tinted with this pattern's colour. */
	public MutableComponent coloredName(Identifier id) {
		return Component.translatable(translationKey(id)).withColor(color);
	}

	/**
	 * Whether this pattern accepts the given modifier. A pattern lists {@code incompatible_modifiers}
	 * for gestures its effects cannot use — Direction on a pure self-buff, say — so the table refuses
	 * the engraving outright rather than charging a cooldown surcharge for nothing.
	 */
	public boolean allows(Identifier modifierId) {
		return !incompatibleModifiers.contains(modifierId);
	}

	public boolean isCellCarved(int row, int col) {
		return row >= 0 && row < GRID && col >= 0 && col < GRID && mask[row][col];
	}

	/** The symbol packed into one bit per cell (bit {@code row*GRID + col}); used to validate chiselling. */
	public long[] maskBits() {
		long[] bits = new long[WORDS];
		for (int row = 0; row < GRID; row++) {
			for (int col = 0; col < GRID; col++) {
				if (mask[row][col]) {
					int idx = row * GRID + col;
					bits[idx >> 6] |= 1L << (idx & 63);
				}
			}
		}
		return bits;
	}

	/** True if the carved bit-set exactly matches this symbol. */
	public boolean matchesCarved(long[] carved) {
		return Arrays.equals(normalize(carved), maskBits());
	}

	private static long[] normalize(long[] carved) {
		if (carved != null && carved.length == WORDS) {
			return carved;
		}
		long[] out = new long[WORDS];
		if (carved != null) {
			System.arraycopy(carved, 0, out, 0, Math.min(carved.length, WORDS));
		}
		return out;
	}

	/** Number of carved cells — also the chisel durability cost of engraving this pattern. */
	public int cellCount() {
		int n = 0;
		for (boolean[] row : mask) {
			for (boolean cell : row) {
				if (cell) {
					n++;
				}
			}
		}
		return n;
	}
}
