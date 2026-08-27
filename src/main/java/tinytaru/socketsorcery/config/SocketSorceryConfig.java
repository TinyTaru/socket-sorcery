package tinytaru.socketsorcery.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.util.Mth;
import tinytaru.socketsorcery.Balance;

/**
 * User-tunable settings, persisted to {@code config/socket-sorcery.json} via Cloth Config's
 * AutoConfig. Defaults come from {@link Balance}, which remains the single source of truth for the
 * mod's intended values; this class is the runtime override layer. Register before anything that
 * reads it (first in mod init).
 */
@Config(name = "socket-sorcery")
public class SocketSorceryConfig implements ConfigData {

	// --- Treasure loot ---

	/** Whether pattern scrolls are injected into structure chest loot. */
	public boolean scrollLoot = true;
	/** Per-chest chance for the scroll pool to roll in an applicable structure table. */
	public float scrollDropChance = Balance.LOOT_SCROLL_CHANCE;

	/** Whether pre-socketed artifact accessories are injected into treasure loot. */
	public boolean accessoryLoot = true;
	/** Per-chest chance for an accessory artifact in an applicable treasure table. */
	public float accessoryChance = Balance.LOOT_ACCESSORY_CHANCE;

	/** Number of engraved gems a treasure artifact may carry. */
	public int artifactMinGems = Balance.ARTIFACT_MIN_GEMS;
	public int artifactMaxGems = Balance.ARTIFACT_MAX_GEMS;

	// --- Ability tuning ---

	/** Scales every bangle/ring activation cooldown (0.5 = half cooldowns, 2.0 = double). */
	public double cooldownMultiplier = 1.0;
	/** How far the bangle's targeting ray (and Blink teleport) reaches, in blocks. */
	public double bangleReach = Balance.BANGLE_REACH;

	// --- Crystal Lamp lighting ---

	/** Default values are mirrored by the /lamplight reset command. */
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_BRIGHTNESS = 0.62;
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_BLUR_BASE = 0.0;
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_BLUR_PER_BLOCK = 0.0;
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_WIDE_BLUR_WEIGHT = 0.0;
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_MIN_OPACITY = 0.003;
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_CULL_MARGIN_FACTOR = 0.001;
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_CULL_MARGIN_BASE = 0.03;
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_LINEAR_FALLOFF = 0.045;
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_QUADRATIC_FALLOFF = 0.015;
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_RANGE_FADE_START = 0.78;
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_SHADOW_SOFTNESS = 0.020;
	@ConfigEntry.Gui.Excluded
	public static final int DEFAULT_LAMP_LIGHT_SHADOW_SAMPLES = 1;
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_CENTER_FLOOR = 0.03;
	@ConfigEntry.Gui.Excluded
	public static final double DEFAULT_LAMP_LIGHT_SAMPLES_PER_PATTERN_CELL = 1.0;

	/** Reference softness used by /lamplight blur 1.0; defaults can still be fully crisp. */
	@ConfigEntry.Gui.Excluded
	public static final double LAMP_LIGHT_BLUR_REFERENCE_BASE = 0.0012;
	@ConfigEntry.Gui.Excluded
	public static final double LAMP_LIGHT_BLUR_REFERENCE_PER_BLOCK = 0.0004;

	/** Overall projected-light strength. */
	public double lampLightBrightness = DEFAULT_LAMP_LIGHT_BRIGHTNESS;
	/** Near-field blur radius in projector UV space. */
	public double lampLightBlurBase = DEFAULT_LAMP_LIGHT_BLUR_BASE;
	/** Extra blur added for every block of distance from the lamp. */
	public double lampLightBlurPerBlock = DEFAULT_LAMP_LIGHT_BLUR_PER_BLOCK;
	/** Strength of the faint, wider blur lobe around each projected pixel. */
	public double lampLightWideBlurWeight = DEFAULT_LAMP_LIGHT_WIDE_BLUR_WEIGHT;
	/** Lowest opacity retained in the projection mesh. Lower values preserve softer tails. */
	public double lampLightMinOpacity = DEFAULT_LAMP_LIGHT_MIN_OPACITY;
	/** Distance-scaled padding used while deciding which blocks can receive the soft edge. */
	public double lampLightCullMarginFactor = DEFAULT_LAMP_LIGHT_CULL_MARGIN_FACTOR;
	/** Fixed padding used while deciding which blocks can receive the soft edge. */
	public double lampLightCullMarginBase = DEFAULT_LAMP_LIGHT_CULL_MARGIN_BASE;
	/** Linear component of distance attenuation. */
	public double lampLightLinearFalloff = DEFAULT_LAMP_LIGHT_LINEAR_FALLOFF;
	/** Quadratic component of distance attenuation. */
	public double lampLightQuadraticFalloff = DEFAULT_LAMP_LIGHT_QUADRATIC_FALLOFF;
	/** Fraction of max range where the final range fade begins. */
	public double lampLightRangeFadeStart = DEFAULT_LAMP_LIGHT_RANGE_FADE_START;
	/** Radius of the tiny area light used to soften blocker shadows. */
	public double lampLightShadowSoftness = DEFAULT_LAMP_LIGHT_SHADOW_SOFTNESS;
	/** Number of blocker-visibility rays: 1 is fast/crisp, 3 gives the softest shadow edges. */
	public int lampLightShadowSamples = DEFAULT_LAMP_LIGHT_SHADOW_SAMPLES;
	/** How much a lit cell's center sample is allowed to fill its corners; lower = crisper edges. */
	public double lampLightCenterFloor = DEFAULT_LAMP_LIGHT_CENTER_FLOOR;
	/** Mesh density used to approximate soft pixel edges; higher = sharper/finer transitions. */
	public double lampLightSamplesPerPatternCell = DEFAULT_LAMP_LIGHT_SAMPLES_PER_PATTERN_CELL;

	@Override
	public void validatePostLoad() {
		scrollDropChance = Mth.clamp(scrollDropChance, 0.0F, 1.0F);
		accessoryChance = Mth.clamp(accessoryChance, 0.0F, 1.0F);
		artifactMinGems = Mth.clamp(artifactMinGems, 0, 5);
		artifactMaxGems = Mth.clamp(artifactMaxGems, artifactMinGems, 5);
		cooldownMultiplier = Mth.clamp(cooldownMultiplier, 0.0, 10.0);
		bangleReach = Mth.clamp(bangleReach, 1.0, 64.0);

		lampLightBrightness = Mth.clamp(lampLightBrightness, 0.0, 2.0);
		lampLightBlurBase = Mth.clamp(lampLightBlurBase, 0.0, 0.05);
		lampLightBlurPerBlock = Mth.clamp(lampLightBlurPerBlock, 0.0, 0.02);
		lampLightWideBlurWeight = Mth.clamp(lampLightWideBlurWeight, 0.0, 1.0);
		lampLightMinOpacity = Mth.clamp(lampLightMinOpacity, 0.0, 0.1);
		lampLightCullMarginFactor = Mth.clamp(lampLightCullMarginFactor, 0.0, 0.5);
		lampLightCullMarginBase = Mth.clamp(lampLightCullMarginBase, 0.0, 1.0);
		lampLightLinearFalloff = Mth.clamp(lampLightLinearFalloff, 0.0, 1.0);
		lampLightQuadraticFalloff = Mth.clamp(lampLightQuadraticFalloff, 0.0, 1.0);
		lampLightRangeFadeStart = Mth.clamp(lampLightRangeFadeStart, 0.0, 0.99);
		lampLightShadowSoftness = Mth.clamp(lampLightShadowSoftness, 0.0, 0.25);
		lampLightShadowSamples = Mth.clamp(lampLightShadowSamples, 1, 3);
		lampLightCenterFloor = Mth.clamp(lampLightCenterFloor, 0.0, 1.0);
		lampLightSamplesPerPatternCell = Mth.clamp(lampLightSamplesPerPatternCell, 0.25, 8.0);
	}

	/** Restores only the Crystal Lamp rendering controls, leaving gameplay settings untouched. */
	public void resetLampLighting() {
		lampLightBrightness = DEFAULT_LAMP_LIGHT_BRIGHTNESS;
		lampLightBlurBase = DEFAULT_LAMP_LIGHT_BLUR_BASE;
		lampLightBlurPerBlock = DEFAULT_LAMP_LIGHT_BLUR_PER_BLOCK;
		lampLightWideBlurWeight = DEFAULT_LAMP_LIGHT_WIDE_BLUR_WEIGHT;
		lampLightMinOpacity = DEFAULT_LAMP_LIGHT_MIN_OPACITY;
		lampLightCullMarginFactor = DEFAULT_LAMP_LIGHT_CULL_MARGIN_FACTOR;
		lampLightCullMarginBase = DEFAULT_LAMP_LIGHT_CULL_MARGIN_BASE;
		lampLightLinearFalloff = DEFAULT_LAMP_LIGHT_LINEAR_FALLOFF;
		lampLightQuadraticFalloff = DEFAULT_LAMP_LIGHT_QUADRATIC_FALLOFF;
		lampLightRangeFadeStart = DEFAULT_LAMP_LIGHT_RANGE_FADE_START;
		lampLightShadowSoftness = DEFAULT_LAMP_LIGHT_SHADOW_SOFTNESS;
		lampLightShadowSamples = DEFAULT_LAMP_LIGHT_SHADOW_SAMPLES;
		lampLightCenterFloor = DEFAULT_LAMP_LIGHT_CENTER_FLOOR;
		lampLightSamplesPerPatternCell = DEFAULT_LAMP_LIGHT_SAMPLES_PER_PATTERN_CELL;
	}

	/** The live config instance. */
	public static SocketSorceryConfig get() {
		return AutoConfig.getConfigHolder(SocketSorceryConfig.class).getConfig();
	}

	/** Registers the config. Call once, before anything that reads it. */
	public static void init() {
		AutoConfig.register(SocketSorceryConfig.class, GsonConfigSerializer::new);
	}
}
