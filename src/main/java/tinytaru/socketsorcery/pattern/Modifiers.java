package tinytaru.socketsorcery.pattern;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.Balance;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * Registry of engraving modifiers and the routines that decode a deep-layer mask into a modifier set
 * and turn a modifier set into {@link EngraveMods}. Each modifier's cells are derived from the
 * pattern's symbol by a standardized rule, so the same gesture works on every pattern.
 */
public final class Modifiers {

	private static final int GRID = Pattern.GRID;

	private static final Map<ResourceLocation, Modifier> BY_ID = new LinkedHashMap<>();

	// Deepening modifiers chisel interior cells to depth 2, centered so the gesture is symmetric.
	public static final Modifier POWER = register(new Modifier(
			SocketSorcery.id("power"), "modifier.socket-sorcery.power", 0xFF5252, Modifiers::centerBlock));
	public static final Modifier DURATION = register(new Modifier(
			SocketSorcery.id("duration"), "modifier.socket-sorcery.duration", 0x42A5F5, Modifiers::topBottom));
	public static final Modifier RANGE = register(new Modifier(
			SocketSorcery.id("range"), "modifier.socket-sorcery.range", 0x66BB6A, Modifiers::leftRight));

	// Direction modifiers chisel the +1 extension cell beyond an extreme to depth 2.
	public static final Modifier UP = register(new Modifier(
			SocketSorcery.id("direction_up"), "modifier.socket-sorcery.direction_up", 0xFFD54F,
			p -> new int[] { extension(p, -1, 0) }));
	public static final Modifier DOWN = register(new Modifier(
			SocketSorcery.id("direction_down"), "modifier.socket-sorcery.direction_down", 0xFFD54F,
			p -> new int[] { extension(p, 1, 0) }));
	public static final Modifier LEFT = register(new Modifier(
			SocketSorcery.id("direction_left"), "modifier.socket-sorcery.direction_left", 0xFFD54F,
			p -> new int[] { extension(p, 0, -1) }));
	public static final Modifier RIGHT = register(new Modifier(
			SocketSorcery.id("direction_right"), "modifier.socket-sorcery.direction_right", 0xFFD54F,
			p -> new int[] { extension(p, 0, 1) }));

	public static Modifier get(ResourceLocation id) {
		return id == null ? null : BY_ID.get(id);
	}

	public static Collection<Modifier> all() {
		return BY_ID.values();
	}

	/**
	 * Decodes the deep-layer mask for {@code pattern} into the set of applied modifiers, or null if
	 * the mask isn't an exact disjoint union of complete modifier cell sets. Empty decodes to empty
	 * (a plain base engraving).
	 */
	public static Set<ResourceLocation> decode(Pattern pattern, long[] deep) {
		Set<ResourceLocation> applied = new LinkedHashSet<>();
		long[] union = GridBits.empty();
		for (Modifier modifier : BY_ID.values()) {
			long[] cells = modifier.cellMask(pattern);
			if (cells != null && GridBits.subset(cells, deep)) {
				applied.add(modifier.id());
				GridBits.orInto(union, cells);
			}
		}
		return GridBits.equal(union, deep) ? applied : null;
	}

	/** The full deep mask an applied modifier set occupies for a pattern. */
	public static long[] cellsFor(Pattern pattern, Collection<ResourceLocation> ids) {
		long[] mask = GridBits.empty();
		for (ResourceLocation id : ids) {
			Modifier modifier = get(id);
			if (modifier != null) {
				long[] cells = modifier.cellMask(pattern);
				if (cells != null) {
					GridBits.orInto(mask, cells);
				}
			}
		}
		return mask;
	}

	/** Combines a modifier set into the effect knobs. */
	public static EngraveMods toMods(Collection<ResourceLocation> ids) {
		int power = 0;
		int durationMult = 1;
		double rangeBonus = 0.0;
		Vec3 aim = Vec3.ZERO;
		for (ResourceLocation id : ids) {
			if (POWER.id().equals(id)) {
				power = Balance.MOD_POWER_AMP;
			} else if (DURATION.id().equals(id)) {
				durationMult = Balance.MOD_DURATION_MULT;
			} else if (RANGE.id().equals(id)) {
				rangeBonus = Balance.MOD_RANGE_BONUS;
			} else if (UP.id().equals(id)) {
				aim = aim.add(0.0, 1.0, 0.0);
			} else if (DOWN.id().equals(id)) {
				aim = aim.add(0.0, -1.0, 0.0);
			} else if (LEFT.id().equals(id)) {
				aim = aim.add(-1.0, 0.0, 0.0);
			} else if (RIGHT.id().equals(id)) {
				aim = aim.add(1.0, 0.0, 0.0);
			}
		}
		return new EngraveMods(power, durationMult, rangeBonus, aim);
	}

	/** Stable display order for a modifier set (registration order). */
	public static List<ResourceLocation> ordered(Collection<ResourceLocation> ids) {
		return BY_ID.keySet().stream().filter(ids::contains).toList();
	}

	// --- cell rules (derived from the symbol so the same gesture works on every pattern) ---

	/** Power: the four pixels straddling the grid centre (a 16x16 grid has no single centre pixel). */
	private static int[] centerBlock(Pattern pattern) {
		int lo = GRID / 2 - 1;
		int hi = GRID / 2;
		return new int[] { idx(lo, lo), idx(lo, hi), idx(hi, lo), idx(hi, hi) };
	}

	/** Duration: the symbol's top-centre and bottom-centre cells. */
	private static int[] topBottom(Pattern pattern) {
		boolean[][] mask = pattern.mask();
		int[] box = bbox(mask);
		int midCol = (box[2] + box[3]) / 2;
		return new int[] {
				cell(box[0], nearestLitCol(mask, box[0], midCol)),
				cell(box[1], nearestLitCol(mask, box[1], midCol))
		};
	}

	/** Range: the symbol's left and right edges at its vertical centre row(s) — centred and symmetric. */
	private static int[] leftRight(Pattern pattern) {
		boolean[][] mask = pattern.mask();
		int[] box = bbox(mask);
		int loRow = (box[0] + box[1]) / 2;
		int hiRow = (box[0] + box[1] + 1) / 2;
		Set<Integer> cells = new LinkedHashSet<>();
		for (int row : loRow == hiRow ? new int[] { loRow } : new int[] { loRow, hiRow }) {
			int left = leftmostLit(mask, row);
			int right = rightmostLit(mask, row);
			if (left >= 0) {
				cells.add(idx(row, left));
			}
			if (right >= 0) {
				cells.add(idx(row, right));
			}
		}
		return cells.stream().mapToInt(Integer::intValue).toArray();
	}

	/** A Direction's extension cell: one pixel beyond the symbol's extreme, centred on the bounding box. */
	private static int extension(Pattern pattern, int dRow, int dCol) {
		int[] box = bbox(pattern.mask());
		int midRow = (box[0] + box[1]) / 2;
		int midCol = (box[2] + box[3]) / 2;
		if (dRow < 0) {
			return cell(box[0] - 1, midCol);
		}
		if (dRow > 0) {
			return cell(box[1] + 1, midCol);
		}
		if (dCol < 0) {
			return cell(midRow, box[2] - 1);
		}
		return cell(midRow, box[3] + 1);
	}

	private static int[] bbox(boolean[][] mask) {
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

	private static int leftmostLit(boolean[][] mask, int row) {
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

	private static int rightmostLit(boolean[][] mask, int row) {
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

	private static int nearestLitCol(boolean[][] mask, int row, int targetCol) {
		if (row < 0 || row >= GRID) {
			return -1;
		}
		int best = -1;
		int bestDist = Integer.MAX_VALUE;
		for (int col = 0; col < GRID; col++) {
			if (mask[row][col]) {
				int dist = Math.abs(col - targetCol);
				if (dist < bestDist) {
					bestDist = dist;
					best = col;
				}
			}
		}
		return best;
	}

	private static int idx(int row, int col) {
		return GridBits.index(row, col);
	}

	private static int cell(int row, int col) {
		if (row < 0 || row >= GRID || col < 0 || col >= GRID) {
			return -1;
		}
		return GridBits.index(row, col);
	}

	private static Modifier register(Modifier modifier) {
		BY_ID.put(modifier.id(), modifier);
		return modifier;
	}

	private Modifiers() {
	}
}
