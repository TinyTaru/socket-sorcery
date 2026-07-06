package tinytaru.socketsorcery.compat.emi;

import java.util.HashMap;
import java.util.Map;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.item.BangleItem;
import tinytaru.socketsorcery.item.NecklaceItem;
import tinytaru.socketsorcery.item.RingItem;
import tinytaru.socketsorcery.item.ScrollItem;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * EMI integration: two custom categories modelling the mod's non-vanilla processes.
 *
 * <ul>
 *   <li><b>Engraving</b> — one recipe per (gem, pattern) pair from {@link Patterns#patternsFor},
 *       showing gem + teaching scroll (+ chisel catalyst) → engraved gem.</li>
 *   <li><b>Socketing</b> — one recipe per accessory, showing accessory + engraved gems → a
 *       fully-socketed accessory.</li>
 * </ul>
 *
 * <p>Everything is generated from the same registries the game logic reads, so new patterns and
 * gems appear here automatically.
 */
public class SocketSorceryEmiPlugin implements EmiPlugin {

	public static final EmiRecipeCategory ENGRAVING = new EmiRecipeCategory(
			SocketSorcery.id("engraving"), EmiStack.of(ModItems.ENGRAVING_TABLE));
	public static final EmiRecipeCategory SOCKETING = new EmiRecipeCategory(
			SocketSorcery.id("socketing"), EmiStack.of(ModItems.SOCKETING_BENCH));

	@Override
	public void register(EmiRegistry registry) {
		registry.addCategory(ENGRAVING);
		registry.addCategory(SOCKETING);
		registry.addWorkstation(ENGRAVING, EmiStack.of(ModItems.ENGRAVING_TABLE));
		registry.addWorkstation(SOCKETING, EmiStack.of(ModItems.SOCKETING_BENCH));

		// pattern id -> the scroll that teaches it.
		Map<ResourceLocation, Item> scrolls = new HashMap<>();
		for (Item item : BuiltInRegistries.ITEM) {
			if (item instanceof ScrollItem scroll && scroll.patternId() != null) {
				scrolls.put(scroll.patternId(), item);
			}
		}

		for (Item gem : ModItems.GEMS) {
			for (ResourceLocation patternId : Patterns.patternsFor(gem)) {
				Item scroll = scrolls.get(patternId);
				if (scroll != null) {
					registry.addRecipe(new EngravingEmiRecipe(gem, scroll, Patterns.get(patternId)));
				}
			}
		}

		registry.addRecipe(new SocketingEmiRecipe(ModItems.NECKLACE, NecklaceItem.CAPACITY));
		registry.addRecipe(new SocketingEmiRecipe(ModItems.BANGLE, BangleItem.CAPACITY));
		registry.addRecipe(new SocketingEmiRecipe(ModItems.RING, RingItem.CAPACITY));
	}
}
