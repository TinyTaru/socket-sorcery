package tinytaru.socketsorcery.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
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
			FabricCreativeModeTab.builder()
					.icon(() -> new ItemStack(ModItems.RUBY))
					.title(Component.translatable("itemGroup.socket-sorcery.general"))
					.displayItems((params, output) -> {
						output.accept(ModItems.ENGRAVING_TABLE);
						output.accept(ModItems.SOCKETING_BENCH);
						output.accept(ModItems.CHISEL);
						output.accept(ModItems.DIAMOND_CHISEL);
						output.accept(ModItems.NETHERITE_CHISEL);
						for (var gem : ModItems.GEMS) {
							output.accept(gem);
						}
						for (var dust : ModItems.GEM_DUSTS) {
							output.accept(dust);
						}
						output.accept(ModItems.SCROLL_FIRE);
						output.accept(ModItems.SCROLL_FROST);
						output.accept(ModItems.SCROLL_HEALING);
						output.accept(ModItems.SCROLL_LIGHTNING);
						output.accept(ModItems.SCROLL_LEAPING);
						output.accept(ModItems.SCROLL_WIND);
						output.accept(ModItems.SCROLL_EARTH);
						output.accept(ModItems.SCROLL_LIFESTEAL);
						output.accept(ModItems.SCROLL_BLINK);
						output.accept(ModItems.SCROLL_HASTE);
						output.accept(ModItems.SCROLL_SPIKES);
						output.accept(ModItems.SCROLL_RING_ON_HIT);
						output.accept(ModItems.SCROLL_RING_ON_ATTACK);
						output.accept(ModItems.SCROLL_RING_ON_FALLING);
						output.accept(ModItems.SCROLL_RING_ON_KILL);
						output.accept(ModItems.SCROLL_RING_HP_THRESHOLD);
						output.accept(ModItems.SCROLL_RING_IN_RADIUS);
						output.accept(ModItems.BLANK_SCROLL);
						for (var ink : ModItems.SCROLL_INKS) {
							output.accept(ink);
						}
						output.accept(ModItems.NECKLACE);
						output.accept(ModItems.COPPER_BANGLE);
						output.accept(ModItems.BANGLE);
						output.accept(ModItems.NETHERITE_BANGLE);
						output.accept(ModItems.RING);
					})
					.build());

	public static void init() {
	}

	private ModItemGroup() {
	}
}
