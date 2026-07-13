package tinytaru.socketsorcery.client.api;

import net.minecraft.world.item.Item;
import tinytaru.socketsorcery.client.GemItemRenderer;
import tinytaru.socketsorcery.client.ModModelPredicates;

/**
 * Client hooks for other mods integrating with Socket Sorcery.
 *
 * <p>Declaring an item as a gem is pure data (list it in a pattern's {@code gems} array) and gives
 * full gameplay. The engraved-icon <em>look</em>, however, needs client registration that cannot be
 * data-driven: call {@link #registerEngravableGem(Item)} from your client initializer and give the
 * item a model {@code overrides} entry on the {@code socket-sorcery:engraving} predicate pointing at
 * a {@code builtin/entity} model (copy any {@code socket-sorcery:item/*_engraved.json}). Without
 * this, an engraved foreign gem still works — it just keeps its plain icon.
 */
public final class SocketSorceryClientApi {

	/**
	 * Registers the composited engraved-gem renderer and the {@code engraving} model predicate for
	 * the given item. Call from a client initializer.
	 */
	public static void registerEngravableGem(Item item) {
		GemItemRenderer.registerFor(item);
		ModModelPredicates.registerFor(item);
	}

	private SocketSorceryClientApi() {
	}
}
