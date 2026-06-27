package tinytaru.socketsorcery.pattern;

import java.util.Arrays;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * A single engravable pattern: an identity, a display name, a tint colour, the 16x16 grid of cells
 * that make up its symbol (what the player chisels — one cell per gem pixel), and the
 * {@link PatternEffect} it grants.
 */
public record Pattern(ResourceLocation id, String translationKey, int color, boolean[][] mask, PatternEffect effect) {

	/** Edge length of the chiselling grid — one cell per pixel of the 16x16 gem texture. */
	public static final int GRID = 16;

	/** Number of 64-bit words needed to hold the {@code GRID*GRID}-bit mask. */
	public static final int WORDS = (GRID * GRID + 63) / 64;

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

	public Component displayName() {
		return Component.translatable(translationKey);
	}

	/** Display name tinted with this pattern's colour (no alpha). */
	public MutableComponent coloredName() {
		return Component.translatable(translationKey).withColor(color);
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
