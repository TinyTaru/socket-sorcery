package tinytaru.socketsorcery.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.component.SocketData;
import tinytaru.socketsorcery.item.AccessoryItem;
import tinytaru.socketsorcery.item.BangleItem;
import tinytaru.socketsorcery.item.NecklaceItem;
import tinytaru.socketsorcery.item.RingItem;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * JEI integration: two custom categories modelling the mod's non-vanilla processes.
 *
 * <ul>
 *   <li><b>Engraving</b> — one recipe per (gem, pattern) pair, straight from the pattern registry's
 *       {@code gems}/{@code scroll} declarations.</li>
 *   <li><b>Socketing</b> — one recipe per accessory, showing accessory + engraved gems → a
 *       fully-socketed accessory.</li>
 * </ul>
 *
 * <p>Everything is generated from the same synced dynamic registry the game logic reads, so
 * datapack-added patterns and gems appear here automatically. Those registries only exist in-world;
 * JEI reloads its plugins on world join, so the recipes appear then. If registration somehow runs
 * without a level, the engraving recipes are skipped for that pass rather than half-built.
 */
@JeiPlugin
public class SocketSorceryJeiPlugin implements IModPlugin {

	private static final Identifier UID = SocketSorcery.id("jei");

	@Override
	public Identifier getPluginUid() {
		return UID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(
				new EngravingRecipeCategory(guiHelper),
				new SocketingRecipeCategory(guiHelper));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addCraftingStation(EngravingRecipeCategory.TYPE, ModItems.ENGRAVING_TABLE);
		registration.addCraftingStation(SocketingRecipeCategory.TYPE, ModItems.SOCKETING_BENCH);
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		HolderLookup.Provider registries = Minecraft.getInstance().level == null ? null
				: Minecraft.getInstance().level.registryAccess();
		if (registries == null) {
			return; // no world yet — JEI re-registers on join and the recipes appear then
		}
		registration.addRecipes(EngravingRecipeCategory.TYPE, engravingRecipes(registries));
		registration.addRecipes(SocketingRecipeCategory.TYPE, socketingRecipes(registries));
	}

	private static List<EngravingRecipe> engravingRecipes(HolderLookup.Provider registries) {
		List<EngravingRecipe> recipes = new ArrayList<>();
		Patterns.all(registries).forEach(holder -> {
			Item scroll = holder.value().scroll().flatMap(BuiltInRegistries.ITEM::getOptional).orElse(null);
			if (scroll == null) {
				return;
			}
			Identifier patternId = holder.key().identifier();
			for (Identifier gemId : holder.value().gems()) {
				BuiltInRegistries.ITEM.getOptional(gemId).ifPresent(gem -> {
					ItemStack engraved = gem.getDefaultInstance();
					engraved.set(ModComponents.ENGRAVING, new EngravingData(patternId));
					recipes.add(new EngravingRecipe(gem.getDefaultInstance(), scroll.getDefaultInstance(), engraved));
				});
			}
		});
		return recipes;
	}

	private static List<SocketingRecipe> socketingRecipes(HolderLookup.Provider registries) {
		// One representative engraved stack per gem (its first supported pattern).
		List<ItemStack> examples = new ArrayList<>();
		for (Item gem : ModItems.GEMS) {
			for (Identifier patternId : Patterns.patternsFor(registries, gem)) {
				ItemStack engraved = gem.getDefaultInstance();
				engraved.set(ModComponents.ENGRAVING, new EngravingData(patternId));
				examples.add(engraved);
				break;
			}
		}
		return List.of(
				socketing(ModItems.NECKLACE, NecklaceItem.CAPACITY, examples),
				socketing(ModItems.COPPER_BANGLE, BangleItem.CAPACITY, examples),
				socketing(ModItems.BANGLE, BangleItem.CAPACITY, examples),
				socketing(ModItems.NETHERITE_BANGLE, BangleItem.CAPACITY, examples),
				socketing(ModItems.RING, RingItem.CAPACITY, examples));
	}

	private static SocketingRecipe socketing(Item accessory, int capacity, List<ItemStack> examples) {
		ItemStack filled = accessory.getDefaultInstance();
		AccessoryItem.setSockets(filled, new SocketData(examples.subList(0, Math.min(capacity, examples.size()))));
		return new SocketingRecipe(accessory.getDefaultInstance(), examples, capacity, filled);
	}
}
