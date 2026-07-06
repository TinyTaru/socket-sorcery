package tinytaru.socketsorcery.compat.emi;

import java.util.ArrayList;
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
import tinytaru.socketsorcery.component.SocketData;
import tinytaru.socketsorcery.item.AccessoryItem;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * A synthetic EMI recipe for one accessory: the accessory plus its capacity in engraved gems (any
 * engraved gem fits any socket) → the accessory fully socketed. The output stack really carries a
 * {@code sockets} component, so EMI shows the composited icon exactly as it renders in-game.
 */
public class SocketingEmiRecipe implements EmiRecipe {

	private final ResourceLocation id;
	private final EmiStack accessory;
	private final EmiIngredient engravedGems;
	private final int capacity;
	private final EmiStack output;

	public SocketingEmiRecipe(Item accessoryItem, int capacity) {
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(accessoryItem);
		this.id = SocketSorcery.id("socketing/" + itemId.getPath());
		this.accessory = EmiStack.of(accessoryItem);
		this.capacity = capacity;

		// One representative engraved stack per gem (its first supported pattern).
		List<ItemStack> examples = new ArrayList<>();
		for (Item gem : ModItems.GEMS) {
			for (ResourceLocation patternId : Patterns.patternsFor(gem)) {
				ItemStack engraved = new ItemStack(gem);
				engraved.set(ModComponents.ENGRAVING, new EngravingData(patternId));
				examples.add(engraved);
				break;
			}
		}
		this.engravedGems = EmiIngredient.of(examples.stream().map(EmiStack::of).toList());

		ItemStack filled = new ItemStack(accessoryItem);
		AccessoryItem.setSockets(filled, new SocketData(examples.subList(0, Math.min(capacity, examples.size()))));
		this.output = EmiStack.of(filled);
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return SocketSorceryEmiPlugin.SOCKETING;
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return List.of(accessory, engravedGems);
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of(output);
	}

	@Override
	public int getDisplayWidth() {
		return 18 * (capacity + 1) + 26 + 18;
	}

	@Override
	public int getDisplayHeight() {
		return 18;
	}

	@Override
	public boolean supportsRecipeTree() {
		return false; // synthetic — socketing happens at the bench, gem by gem
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.addSlot(accessory, 0, 0);
		for (int i = 0; i < capacity; i++) {
			widgets.addSlot(engravedGems, 18 * (i + 1), 0);
		}
		int arrowX = 18 * (capacity + 1) + 2;
		widgets.addTexture(EmiTexture.EMPTY_ARROW, arrowX, 0);
		widgets.addSlot(output, arrowX + 28, 0).recipeContext(this);
	}
}
