package tinytaru.socketsorcery;

/**
 * Single source of truth for the mod's cross-cutting balance constants.
 *
 * <p>These were previously scattered as {@code private static final} fields across {@code Cooldowns},
 * {@code Patterns}, {@code Modifiers}, {@code EngraveMods}, {@code NecklaceItem},
 * {@code SocketArtifactFunction} and {@code ModLoot}. Gathering them here gives one place to tune
 * gameplay and a single hook point for the configuration screen added later.
 *
 * <p>Per-effect tuning literals that live inside individual pattern lambdas (e.g. a specific burn
 * duration or heal amount) are intentionally <em>not</em> here — they are local to one effect and are
 * tuned in place.
 */
public final class Balance {

	// --- Bangle activation cooldowns (ticks; 20 ticks = 1 second) ---

	/** Cooldown contributed by a socketed gem whose pattern has no explicit base. */
	public static final int COOLDOWN_DEFAULT_BASE = 40;
	/** Extra fraction of a gem's base cooldown charged per applied modifier. */
	public static final double COOLDOWN_MOD_SURCHARGE = 0.4;

	public static final int COOLDOWN_BASE_LEAPING = 20;
	public static final int COOLDOWN_BASE_WIND = 30;
	public static final int COOLDOWN_BASE_FIRE = 40;
	public static final int COOLDOWN_BASE_HASTE = 50;
	public static final int COOLDOWN_BASE_FROST = 50;
	public static final int COOLDOWN_BASE_SPIKES = 55;
	public static final int COOLDOWN_BASE_LIFESTEAL = 60;
	public static final int COOLDOWN_BASE_HEALING = 60;
	public static final int COOLDOWN_BASE_EARTH = 70;
	public static final int COOLDOWN_BASE_BLINK = 80;
	public static final int COOLDOWN_BASE_LIGHTNING = 100;

	// --- Bangle targeting ---

	/** How far the bangle's targeting ray reaches, in blocks. */
	public static final double BANGLE_REACH = 6.0;

	// --- Necklace passive ticking ---

	/** Run each necklace gem's passive behaviour every this many ticks. */
	public static final int NECKLACE_TICK_INTERVAL = 20;
	/** Duration (ticks) of the short, continuously-refreshed self-buffs the necklace passives apply. */
	public static final int NECKLACE_BUFF_DURATION = 60;

	// --- Engraving modifier strengths ---

	/** Amplifier bump applied by the Power modifier. */
	public static final int MOD_POWER_AMP = 1;
	/** Duration multiplier applied by the Duration modifier. */
	public static final int MOD_DURATION_MULT = 2;
	/** Radius bonus (blocks) applied by the Range modifier. */
	public static final double MOD_RANGE_BONUS = 3.0;
	/** Magnitude multiplier applied when an engraving carries the Power modifier. */
	public static final double MOD_MAGNITUDE_POWER_MULT = 1.5;

	// --- Treasure loot ---

	/** Number of engraved gems a pre-socketed treasure artifact may carry. */
	public static final int ARTIFACT_MIN_GEMS = 1;
	public static final int ARTIFACT_MAX_GEMS = 3;

	/** Per-chest chance for an accessory to appear in an applicable treasure table. */
	public static final float LOOT_ACCESSORY_CHANCE = 0.10F;
	/** Per-chest chance for the scroll pool to roll in an applicable structure table. */
	public static final float LOOT_SCROLL_CHANCE = 0.5F;

	private Balance() {
	}
}
