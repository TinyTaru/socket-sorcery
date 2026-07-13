package tinytaru.socketsorcery.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.item.BangleItem;
import tinytaru.socketsorcery.item.NecklaceItem;
import tinytaru.socketsorcery.item.RingItem;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * EMI integration: two custom categories modelling the mod's non-vanilla processes.
 *
 * <ul>
 *   <li><b>Engraving</b> — one recipe per (gem, pattern) pair, straight from the pattern registry's
 *       {@code gems}/{@code scroll} declarations: gem + teaching scroll (+ chisel catalyst) →
 *       engraved gem.</li>
 *   <li><b>Socketing</b> — one recipe per accessory, showing accessory + engraved gems → a
 *       fully-socketed accessory.</li>
 * </ul>
 *
 * <p>Everything is generated from the same synced dynamic registry the game logic reads, so
 * datapack-added patterns and gems appear here automatically. EMI re-runs registration when recipes
 * reload (i.e. in-world), so the client level's registries are available; if not, the engraving
 * recipes are skipped for that pass.
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

		registry.addRecipe(new SocketingEmiRecipe(ModItems.NECKLACE, NecklaceItem.CAPACITY));
		registry.addRecipe(new SocketingEmiRecipe(ModItems.BANGLE, BangleItem.CAPACITY));
		registry.addRecipe(new SocketingEmiRecipe(ModItems.RING, RingItem.CAPACITY));

		HolderLookup.Provider registries = Minecraft.getInstance().level == null ? null
				: Minecraft.getInstance().level.registryAccess();
		if (registries == null) {
			return; // no world yet — EMI re-registers on join and the recipes appear then
		}
		Patterns.all(registries).forEach(holder -> {
			Item scroll = holder.value().scroll()
					.flatMap(BuiltInRegistries.ITEM::getOptional).orElse(null);
			if (scroll == null) {
				return;
			}
			for (var gemId : holder.value().gems()) {
				BuiltInRegistries.ITEM.getOptional(gemId).ifPresent(gem ->
						registry.addRecipe(new EngravingEmiRecipe(gem, scroll, holder)));
			}
		});
	}
}
