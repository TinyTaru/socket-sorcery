package tinytaru.socketsorcery.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.SocketSorcery;

public final class ModItemGroup {

	public static final ResourceKey<CreativeModeTab> KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, SocketSorcery.id("general"));

	public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KEY,
			FabricItemGroup.builder()
					.icon(() -> new ItemStack(ModItems.RUBY))
					.title(Component.translatable("itemGroup.socket-sorcery.general"))
					.displayItems((params, output) -> {
						output.accept(ModItems.ENGRAVING_TABLE);
						output.accept(ModItems.SOCKETING_BENCH);
						output.accept(ModItems.CHISEL);
						for (var gem : ModItems.GEMS) {
							output.accept(gem);
						}
						output.accept(ModItems.SCROLL_FIRE);
						output.accept(ModItems.SCROLL_FROST);
						output.accept(ModItems.SCROLL_HEALING);
						output.accept(ModItems.SCROLL_LIGHTNING);
						output.accept(ModItems.SCROLL_LEAPING);
						output.accept(ModItems.NECKLACE);
						output.accept(ModItems.BANGLE);
					})
					.build());

	public static void init() {
	}

	private ModItemGroup() {
	}
}
