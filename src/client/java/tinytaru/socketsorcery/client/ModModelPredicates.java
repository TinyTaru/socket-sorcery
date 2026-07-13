package tinytaru.socketsorcery.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.registry.ModComponents;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * Registers a {@code socket-sorcery:engraving} model property on every gem: 1 when the gem is
 * engraved, else 0. The gem item models use it in a single {@code overrides} entry to switch to the
 * {@code builtin/entity} engraved model, which routes rendering through {@link GemItemRenderer}.
 */
public final class ModModelPredicates {

	private static final ResourceLocation ENGRAVING = SocketSorcery.id("engraving");

	public static void register() {
		for (Item gem : ModItems.GEMS) {
			registerFor(gem);
		}
	}

	/** Adds the {@code engraving} predicate to the given item (for other mods' gems). */
	public static void registerFor(Item item) {
		ItemProperties.register(item, ENGRAVING, (stack, level, entity, seed) ->
				stack.has(ModComponents.ENGRAVING) ? 1.0F : 0.0F);
	}

	private ModModelPredicates() {
	}
}
