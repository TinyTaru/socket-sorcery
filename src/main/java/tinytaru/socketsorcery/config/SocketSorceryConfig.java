package tinytaru.socketsorcery.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
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

	@Override
	public void validatePostLoad() {
		scrollDropChance = Mth.clamp(scrollDropChance, 0.0F, 1.0F);
		accessoryChance = Mth.clamp(accessoryChance, 0.0F, 1.0F);
		artifactMinGems = Mth.clamp(artifactMinGems, 0, 5);
		artifactMaxGems = Mth.clamp(artifactMaxGems, artifactMinGems, 5);
		cooldownMultiplier = Mth.clamp(cooldownMultiplier, 0.0, 10.0);
		bangleReach = Mth.clamp(bangleReach, 1.0, 64.0);
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
