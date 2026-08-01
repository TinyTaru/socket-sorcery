package tinytaru.socketsorcery.pattern;

import java.util.Arrays;

/**
 * Helpers for the {@code GRID*GRID}-bit masks (packed into {@link Pattern#WORDS} longs, bit
 * {@code row*GRID + col}) used throughout the engraving system: the carved layer, the deep layer,
 * pattern symbols and modifier cell sets.
 */
public final class GridBits {

	public static long[] empty() {
		return new long[Pattern.WORDS];
	}

	public static long[] copy(long[] mask) {
		return Arrays.copyOf(mask, Pattern.WORDS);
	}

	public static int index(int row, int col) {
		return row * Pattern.GRID + col;
	}

	public static void setIndex(long[] mask, int index) {
		mask[index >> 6] |= 1L << (index & 63);
	}

	public static void set(long[] mask, int row, int col) {
		setIndex(mask, index(row, col));
	}

	public static void clearIndex(long[] mask, int index) {
		mask[index >> 6] &= ~(1L << (index & 63));
	}

	public static boolean getIndex(long[] mask, int index) {
		return (mask[index >> 6] >>> (index & 63) & 1L) != 0;
	}

	public static boolean get(long[] mask, int row, int col) {
		return getIndex(mask, index(row, col));
	}

	/** True if every set bit of {@code sub} is also set in {@code sup} (sub ⊆ sup). */
	public static boolean subset(long[] sub, long[] sup) {
		for (int i = 0; i < Pattern.WORDS; i++) {
			if ((sub[i] & ~sup[i]) != 0) {
				return false;
			}
		}
		return true;
	}

	public static void orInto(long[] target, long[] other) {
		for (int i = 0; i < Pattern.WORDS; i++) {
			target[i] |= other[i];
		}
	}

	public static long[] or(long[] a, long[] b) {
		long[] out = copy(a);
		orInto(out, b);
		return out;
	}

	public static boolean equal(long[] a, long[] b) {
		return Arrays.equals(normalize(a), normalize(b));
	}

	public static int count(long[] mask) {
		int n = 0;
		for (long word : mask) {
			n += Long.bitCount(word);
		}
		return n;
	}

	public static boolean isEmpty(long[] mask) {
		for (long word : mask) {
			if (word != 0) {
				return false;
			}
		}
		return true;
	}

	private static long[] normalize(long[] mask) {
		return mask != null && mask.length == Pattern.WORDS ? mask : Arrays.copyOf(mask == null ? new long[0] : mask, Pattern.WORDS);
	}

	private GridBits() {
	}
}
