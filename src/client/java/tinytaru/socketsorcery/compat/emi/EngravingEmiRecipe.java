package tinytaru.socketsorcery.compat.emi;

import java.util.List;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.registry.ModComponents;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * A synthetic EMI recipe for one (gem, pattern) engraving: gem + teaching scroll, with a chisel as
 * catalyst, produce the gem engraved with the pattern (no modifiers — the base engraving).
 */
public class EngravingEmiRecipe implements EmiRecipe {

	private final ResourceLocation id;
	private final EmiIngredient gem;
	private final EmiIngredient scroll;
	private final EmiIngredient chisels;
	private final EmiStack output;

	public EngravingEmiRecipe(Item gem, Item scroll, Pattern pattern) {
		ResourceLocation gemId = BuiltInRegistries.ITEM.getKey(gem);
		this.id = SocketSorcery.id("engraving/" + gemId.getPath() + "/" + pattern.id().getPath());
		this.gem = EmiStack.of(gem);
		this.scroll = EmiStack.of(scroll);
		this.chisels = EmiIngredient.of(List.of(
				EmiStack.of(ModItems.CHISEL), EmiStack.of(ModItems.DIAMOND_CHISEL), EmiStack.of(ModItems.NETHERITE_CHISEL)));
		ItemStack engraved = new ItemStack(gem);
		engraved.set(ModComponents.ENGRAVING, new EngravingData(pattern.id()));
		this.output = EmiStack.of(engraved);
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return SocketSorceryEmiPlugin.ENGRAVING;
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return List.of(gem, scroll);
	}

	@Override
	public List<EmiIngredient> getCatalysts() {
		return List.of(chisels);
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of(output);
	}

	@Override
	public int getDisplayWidth() {
		return 108;
	}

	@Override
	public int getDisplayHeight() {
		return 18;
	}

	@Override
	public boolean supportsRecipeTree() {
		return false; // synthetic — the real process is the chiselling minigame
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.addSlot(gem, 0, 0);
		widgets.addSlot(scroll, 18, 0);
		widgets.addSlot(chisels, 36, 0).catalyst(true);
		widgets.addTexture(EmiTexture.EMPTY_ARROW, 58, 0);
		widgets.addSlot(output, 86, 0).recipeContext(this);
	}
}
