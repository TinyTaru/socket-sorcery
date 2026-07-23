package tinytaru.socketsorcery.client.api;

import net.minecraft.world.item.Item;
import tinytaru.socketsorcery.client.GemItemRenderer;

/**
 * Client hooks for other mods integrating with Socket Sorcery.
 *
 * <p>Declaring an item as a gem is pure data (list it in a pattern's {@code gems} array) and gives
 * full gameplay. The engraved-icon <em>look</em> is now data too: give your item a model definition
 * at {@code assets/<your-namespace>/items/<item>.json} that switches on the
 * {@code minecraft:has_component} condition for {@code socket-sorcery:engraving} and points the
 * true branch at the {@code socket-sorcery:engraved_gem} special model — copy any of this mod's
 * {@code assets/socket-sorcery/items/*.json} gem files. Without it, an engraved foreign gem still
 * works — it just keeps its plain icon.
 */
public final class SocketSorceryClientApi {

	/**
	 * Opts an item into the composited engraved-gem renderer's texture cache.
	 *
	 * @deprecated The model predicate this used to register no longer exists: the engraved look is
	 *     selected entirely by the item's model definition JSON (see the class docs). Kept for source
	 *     compatibility.
	 */
	@Deprecated(forRemoval = true)
	public static void registerEngravableGem(Item item) {
		GemItemRenderer.registerFor(item);
	}

	private SocketSorceryClientApi() {
	}
}
