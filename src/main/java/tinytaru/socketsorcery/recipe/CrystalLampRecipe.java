package tinytaru.socketsorcery.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import tinytaru.socketsorcery.component.CrystalLampData;
import tinytaru.socketsorcery.component.GlassPaneEngravingData;
import tinytaru.socketsorcery.registry.ModComponents;
import tinytaru.socketsorcery.registry.ModItems;
import tinytaru.socketsorcery.registry.ModRecipes;

/**
 * Combines five individually engraved glass panes with a glowstone block. The pane positions are
 * meaningful: up/north sit in the top-left/top-middle, west/east flank the core, and south sits
 * below it. That gives builders direct control of which image shines from each lamp face.
 */
public class CrystalLampRecipe extends CustomRecipe {

	@Override
	public boolean matches(CraftingInput input, Level level) {
		if (input.width() != 3 || input.height() != 3) {
			return false;
		}
		return engravedPane(input.getItem(0, 0)) // up
				&& engravedPane(input.getItem(1, 0)) // north
				&& input.getItem(2, 0).isEmpty()
				&& engravedPane(input.getItem(0, 1)) // west
				&& input.getItem(1, 1).is(Items.GLOWSTONE)
				&& engravedPane(input.getItem(2, 1)) // east
				&& input.getItem(0, 2).isEmpty()
				&& engravedPane(input.getItem(1, 2)) // south
				&& input.getItem(2, 2).isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		if (input.width() != 3 || input.height() != 3) {
			return ItemStack.EMPTY;
		}
		ItemStack lamp = new ItemStack(ModItems.CRYSTAL_LAMP);
		lamp.set(ModComponents.CRYSTAL_LAMP, new CrystalLampData(
				data(input.getItem(1, 0)).etched(), // north
				data(input.getItem(2, 1)).etched(), // east
				data(input.getItem(1, 2)).etched(), // south
				data(input.getItem(0, 1)).etched(), // west
				data(input.getItem(0, 0)).etched()  // up
		));
		return lamp;
	}

	@Override
	public RecipeSerializer<CrystalLampRecipe> getSerializer() {
		return ModRecipes.CRYSTAL_LAMP;
	}

	private static boolean engravedPane(ItemStack stack) {
		return stack.is(Items.GLASS_PANE)
				&& !stack.getOrDefault(ModComponents.GLASS_PANE_ENGRAVING,
						GlassPaneEngravingData.EMPTY).isEmpty();
	}

	private static GlassPaneEngravingData data(ItemStack stack) {
		return stack.getOrDefault(ModComponents.GLASS_PANE_ENGRAVING, GlassPaneEngravingData.EMPTY);
	}
}
