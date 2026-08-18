package tinytaru.socketsorcery.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.registry.ModItems;
import tinytaru.socketsorcery.registry.ModRecipes;

/** Polishes a chisel-carved stone block, consuming two points of chisel durability. */
public final class EngravedStoneRecipe extends CustomRecipe {

	private static final TagKey<Item> CHISELS = TagKey.create(
			Registries.ITEM, SocketSorcery.id("chisels"));

	@Override
	public boolean matches(CraftingInput input, Level level) {
		if (input.ingredientCount() != 2) {
			return false;
		}

		boolean hasChisel = false;
		boolean hasStone = false;
		for (ItemStack stack : input.items()) {
			if (stack.isEmpty()) {
				continue;
			}
			if (stack.is(CHISELS)) {
				if (hasChisel) {
					return false;
				}
				hasChisel = true;
			} else if (polishedStone(stack)) {
				if (hasStone) {
					return false;
				}
				hasStone = true;
			} else {
				return false;
			}
		}
		return hasChisel && hasStone;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		for (ItemStack stack : input.items()) {
			if (stack.is(Blocks.POLISHED_DIORITE.asItem())) {
				return new ItemStack(ModItems.ENGRAVED_DIORITE);
			}
			if (stack.is(Blocks.POLISHED_DEEPSLATE.asItem())) {
				return new ItemStack(ModItems.ENGRAVED_DEEPSLATE);
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		NonNullList<ItemStack> remaining = CraftingRecipe.defaultCraftingReminder(input);
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (!stack.is(CHISELS)) {
				continue;
			}
			ItemStack damaged = stack.copy();
			damaged.setCount(1);
			damaged.setDamageValue(damaged.getDamageValue() + 2);
			remaining.set(i, damaged.isBroken() ? ItemStack.EMPTY : damaged);
			break;
		}
		return remaining;
	}

	@Override
	public RecipeSerializer<EngravedStoneRecipe> getSerializer() {
		return ModRecipes.ENGRAVED_STONE;
	}

	private static boolean polishedStone(ItemStack stack) {
		return stack.is(Blocks.POLISHED_DIORITE.asItem())
				|| stack.is(Blocks.POLISHED_DEEPSLATE.asItem());
	}
}
