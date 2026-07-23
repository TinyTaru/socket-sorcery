package tinytaru.socketsorcery.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.menu.SocketContainer;
import tinytaru.socketsorcery.registry.ModItems;

/** Accessory + engraved gems → fully-socketed accessory. */
public class SocketingRecipeCategory extends AbstractRecipeCategory<SocketingRecipe> {

	public static final IRecipeType<SocketingRecipe> TYPE =
			IRecipeType.create(SocketSorcery.id("socketing"), SocketingRecipe.class);

	/** Widest case: the accessory, every socket, the arrow gap and the result. */
	private static final int WIDTH = 18 * (SocketContainer.MAX_SLOTS + 1) + 26 + 18;

	public SocketingRecipeCategory(IGuiHelper guiHelper) {
		super(TYPE, Component.translatable("category.socket-sorcery.socketing"),
				guiHelper.createDrawableItemLike(ModItems.SOCKETING_BENCH), WIDTH, 18);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SocketingRecipe recipe, IFocusGroup focuses) {
		builder.addInputSlot(0, 0).addItemStack(recipe.accessory());
		// Any engraved gem fits any socket, so every slot offers the whole set.
		for (int i = 0; i < recipe.capacity(); i++) {
			builder.addInputSlot(18 * (i + 1), 0).addItemStacks(recipe.gems());
		}
		builder.addOutputSlot(18 * (recipe.capacity() + 1) + 30, 0).addItemStack(recipe.filled());
	}
}
