package tinytaru.socketsorcery.registry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.recipe.CrystalLampRecipe;
import tinytaru.socketsorcery.recipe.EngravedStoneRecipe;

/** Registers recipes whose results preserve data from their ingredients. */
public final class ModRecipes {

	private static final CrystalLampRecipe CRYSTAL_LAMP_INSTANCE = new CrystalLampRecipe();
	private static final EngravedStoneRecipe ENGRAVED_STONE_INSTANCE = new EngravedStoneRecipe();

	public static final RecipeSerializer<CrystalLampRecipe> CRYSTAL_LAMP = Registry.register(
			BuiltInRegistries.RECIPE_SERIALIZER, SocketSorcery.id("crystal_lamp"), new RecipeSerializer<>(
					MapCodec.unit(CRYSTAL_LAMP_INSTANCE),
					StreamCodec.unit(CRYSTAL_LAMP_INSTANCE)));

	public static final RecipeSerializer<EngravedStoneRecipe> ENGRAVED_STONE = Registry.register(
			BuiltInRegistries.RECIPE_SERIALIZER, SocketSorcery.id("engraved_stone"), new RecipeSerializer<>(
					MapCodec.unit(ENGRAVED_STONE_INSTANCE),
					StreamCodec.unit(ENGRAVED_STONE_INSTANCE)));

	public static void init() {
	}

	private ModRecipes() {
	}
}
