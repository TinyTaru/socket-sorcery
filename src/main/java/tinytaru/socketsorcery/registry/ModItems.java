package tinytaru.socketsorcery.registry;

import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.item.BangleItem;
import tinytaru.socketsorcery.item.ChiselItem;
import tinytaru.socketsorcery.item.GemItem;
import tinytaru.socketsorcery.item.NecklaceItem;
import tinytaru.socketsorcery.item.ScrollItem;

public final class ModItems {

	public static final Item CHISEL = register("chisel",
			new ChiselItem(new Item.Properties().durability(64).stacksTo(1)));

	// Custom gems. Each supports a fixed pair of patterns (see Patterns).
	public static final Item RUBY = register("ruby", new GemItem(new Item.Properties()));
	public static final Item SAPPHIRE = register("sapphire", new GemItem(new Item.Properties()));
	public static final Item PERIDOT = register("peridot", new GemItem(new Item.Properties()));
	public static final Item AMETHYST = register("amethyst", new GemItem(new Item.Properties()));
	public static final Item TOPAZ = register("topaz", new GemItem(new Item.Properties()));

	// Engravable gems crafted from vanilla materials (a "+" of that material).
	public static final Item ENGRAVABLE_DIAMOND = register("engravable_diamond", new GemItem(new Item.Properties()));
	public static final Item ENGRAVABLE_REDSTONE = register("engravable_redstone", new GemItem(new Item.Properties()));
	public static final Item ENGRAVABLE_LAPIS = register("engravable_lapis", new GemItem(new Item.Properties()));
	public static final Item ENGRAVABLE_EMERALD = register("engravable_emerald", new GemItem(new Item.Properties()));
	public static final Item ENGRAVABLE_QUARTZ = register("engravable_quartz", new GemItem(new Item.Properties()));

	/** Every engravable gem (custom + vanilla-derived), for client renderers and the creative tab. */
	public static final Item[] GEMS = {
			RUBY, SAPPHIRE, PERIDOT, AMETHYST, TOPAZ,
			ENGRAVABLE_DIAMOND, ENGRAVABLE_REDSTONE, ENGRAVABLE_LAPIS, ENGRAVABLE_EMERALD, ENGRAVABLE_QUARTZ
	};

	// Pattern scrolls (found in loot).
	public static final Item SCROLL_FIRE = register("scroll_fire", new ScrollItem(new Item.Properties().stacksTo(16)));
	public static final Item SCROLL_FROST = register("scroll_frost", new ScrollItem(new Item.Properties().stacksTo(16)));
	public static final Item SCROLL_HEALING = register("scroll_healing", new ScrollItem(new Item.Properties().stacksTo(16)));
	public static final Item SCROLL_LIGHTNING = register("scroll_lightning", new ScrollItem(new Item.Properties().stacksTo(16)));
	public static final Item SCROLL_LEAPING = register("scroll_leaping", new ScrollItem(new Item.Properties().stacksTo(16)));

	// Accessories (Trinkets).
	public static final Item NECKLACE = register("necklace", new NecklaceItem(new Item.Properties().stacksTo(1)));
	public static final Item BANGLE = register("bangle", new BangleItem(new Item.Properties().stacksTo(1)));

	// Block items.
	public static final Item ENGRAVING_TABLE = register("engraving_table",
			new BlockItem(ModBlocks.ENGRAVING_TABLE, new Item.Properties()));
	public static final Item SOCKETING_BENCH = register("socketing_bench",
			new BlockItem(ModBlocks.SOCKETING_BENCH, new Item.Properties()));

	private static Item register(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, SocketSorcery.id(name), item);
	}

	public static void init() {
		// Register the accessories with Trinkets so they can be equipped in the necklace / bangle slots.
		TrinketsApi.registerTrinket(NECKLACE, (Trinket) NECKLACE);
		TrinketsApi.registerTrinket(BANGLE, (Trinket) BANGLE);
	}

	private ModItems() {
	}
}
