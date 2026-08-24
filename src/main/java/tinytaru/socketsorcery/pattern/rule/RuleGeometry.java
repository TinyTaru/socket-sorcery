package tinytaru.socketsorcery.pattern.rule;

import java.util.Arrays;

import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.pattern.Pattern;

/**
 * Shared symbol-geometry helpers for the built-in cell rules — ported verbatim from the private
 * methods the rules used when they lived in {@code Modifiers}.
 */
final class RuleGeometry {

	static final int GRID = Pattern.GRID;

	/** {minRow, maxRow, minCol, maxCol} of the symbol's lit cells. */
	static int[] bbox(boolean[][] mask) {
		int minR = GRID, maxR = -1, minC = GRID, maxC = -1;
		for (int row = 0; row < GRID; row++) {
			for (int col = 0; col < GRID; col++) {
				if (mask[row][col]) {
					minR = Math.min(minR, row);
					maxR = Math.max(maxR, row);
					minC = Math.min(minC, col);
					maxC = Math.max(maxC, col);
				}
			}
		}
		return new int[] { minR, maxR, minC, maxC };
	}

	static int leftmostLit(boolean[][] mask, int row) {
		if (row < 0 || row >= GRID) {
			return -1;
		}
		for (int col = 0; col < GRID; col++) {
			if (mask[row][col]) {
				return col;
			}
		}
		return -1;
	}

	static int rightmostLit(boolean[][] mask, int row) {
		if (row < 0 || row >= GRID) {
			return -1;
		}
		for (int col = GRID - 1; col >= 0; col--) {
			if (mask[row][col]) {
				return col;
			}
		}
		return -1;
	}

	/**
	 * The lit columns nearest the symbol's horizontal centre. {@code centerNumerator} is twice the
	 * centre column, so an even-width bounding box can preserve both equally-near centre columns
	 * instead of rounding to one side.
	 */
	static int[] nearestLitCols(boolean[][] mask, int row, int centerNumerator) {
		if (row < 0 || row >= GRID) {
			return new int[0];
		}
		int[] best = new int[GRID];
		int count = 0;
		int bestDist = Integer.MAX_VALUE;
		for (int col = 0; col < GRID; col++) {
			if (mask[row][col]) {
				int dist = Math.abs(2 * col - centerNumerator);
				if (dist < bestDist) {
					bestDist = dist;
					count = 0;
					best[count++] = col;
				} else if (dist == bestDist) {
					best[count++] = col;
				}
			}
		}
		return Arrays.copyOf(best, count);
	}

	static int idx(int row, int col) {
		return GridBits.index(row, col);
	}

	/** A bounds-checked cell index, or -1 when off-grid. */
	static int cell(int row, int col) {
		if (row < 0 || row >= GRID || col < 0 || col >= GRID) {
			return -1;
		}
		return GridBits.index(row, col);
	}

	private RuleGeometry() {
	}
}
