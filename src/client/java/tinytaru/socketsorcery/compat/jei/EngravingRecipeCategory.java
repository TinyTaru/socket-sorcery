package tinytaru.socketsorcery.compat.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.registry.ModItems;

/** Gem + teaching scroll (+ chisel catalyst) → engraved gem. */
public class EngravingRecipeCategory extends AbstractRecipeCategory<EngravingRecipe> {

	public static final IRecipeType<EngravingRecipe> TYPE =
			IRecipeType.create(SocketSorcery.id("engraving"), EngravingRecipe.class);

	public EngravingRecipeCategory(IGuiHelper guiHelper) {
		super(TYPE, Component.translatable("category.socket-sorcery.engraving"),
				guiHelper.createDrawableItemLike(ModItems.ENGRAVING_TABLE), 108, 18);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, EngravingRecipe recipe, IFocusGroup focuses) {
		builder.addInputSlot(0, 0).addItemStack(recipe.gem());
		builder.addInputSlot(18, 0).addItemStack(recipe.scroll());
		// Any chisel will do; shown as the station rather than a consumed input.
		builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 36, 0).addItemStacks(List.of(
				ModItems.CHISEL.getDefaultInstance(),
				ModItems.DIAMOND_CHISEL.getDefaultInstance(),
				ModItems.NETHERITE_CHISEL.getDefaultInstance()));
		builder.addOutputSlot(86, 0).addItemStack(recipe.engraved());
	}
}
