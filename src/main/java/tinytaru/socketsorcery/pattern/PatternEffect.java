package tinytaru.socketsorcery.pattern;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;

/**
 * The two behaviours a pattern exposes. The same engraved gem runs its passive behaviour when
 * socketed into a necklace and its active behaviour when socketed into a bangle. {@code mods} carries
 * the engraving's modifier adjustments ({@link EngraveMods#NONE} for a base engraving).
 */
public interface PatternEffect {

	/**
	 * Passive behaviour, run server-side while a necklace carrying this pattern is worn. Called from
	 * the necklace's trinket tick (throttled), once per socketed gem of this pattern, in socket order.
	 */
	void onNecklaceTick(ServerPlayer player, EngraveMods mods, int socketIndex);

	/**
	 * Active behaviour, run server-side when the player presses the bangle keybind. Called once per
	 * socketed gem of this pattern, in socket order.
	 *
	 * @param target what the player is looking at (entity, block, or {@link HitResult.Type#MISS}).
	 */
	void onBangleActivate(ServerPlayer player, HitResult target, EngraveMods mods, int socketIndex);
}
