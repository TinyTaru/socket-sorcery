package tinytaru.socketsorcery.registry;

import java.util.function.Function;

import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.item.BangleItem;
import tinytaru.socketsorcery.item.ChiselItem;
import tinytaru.socketsorcery.item.GemItem;
import tinytaru.socketsorcery.item.NecklaceItem;
import tinytaru.socketsorcery.item.RingItem;
import tinytaru.socketsorcery.item.ScrollItem;

public final class ModItems {

	public static final Item CHISEL = register("chisel", ChiselItem::new,
			new Item.Properties().durability(64).stacksTo(1));
	public static final Item DIAMOND_CHISEL = register("diamond_chisel", ChiselItem::new,
			new Item.Properties().durability(256).stacksTo(1));
	/** Every point of carve cost above 1 is discounted by 1 with this chisel (see {@link ChiselItem}). */
	public static final Item NETHERITE_CHISEL = register("netherite_chisel", p -> new ChiselItem(p, 1),
			new Item.Properties().durability(1024).stacksTo(1).fireResistant());

	// Custom gems. Each supports a fixed pair of patterns (see Patterns).
	public static final Item RUBY = registerGem("ruby");
	public static final Item SAPPHIRE = registerGem("sapphire");
	public static final Item PERIDOT = registerGem("peridot");
	public static final Item AMETHYST = registerGem("amethyst");
	public static final Item TOPAZ = registerGem("topaz");

	// Engravable gems crafted from vanilla materials (a "+" of that material).
	public static final Item ENGRAVABLE_DIAMOND = registerGem("engravable_diamond");
	public static final Item ENGRAVABLE_REDSTONE = registerGem("engravable_redstone");
	public static final Item ENGRAVABLE_LAPIS = registerGem("engravable_lapis");
	public static final Item ENGRAVABLE_EMERALD = registerGem("engravable_emerald");
	public static final Item ENGRAVABLE_QUARTZ = registerGem("engravable_quartz");
	public static final Item ENGRAVABLE_PRISMARINE = registerGem("engravable_prismarine");
	public static final Item ENGRAVABLE_GLOWSTONE = registerGem("engravable_glowstone");
	public static final Item ENGRAVABLE_COPPER = registerGem("engravable_copper");
	public static final Item ENGRAVABLE_ENDER = registerGem("engravable_ender");

	/** Every engravable gem (custom + vanilla-derived), for client renderers and the creative tab. */
	public static final Item[] GEMS = {
			RUBY, SAPPHIRE, PERIDOT, AMETHYST, TOPAZ,
			ENGRAVABLE_DIAMOND, ENGRAVABLE_REDSTONE, ENGRAVABLE_LAPIS, ENGRAVABLE_EMERALD, ENGRAVABLE_QUARTZ,
			ENGRAVABLE_PRISMARINE, ENGRAVABLE_GLOWSTONE, ENGRAVABLE_COPPER, ENGRAVABLE_ENDER
	};

	// Pattern scrolls (found in loot).
	public static final Item SCROLL_FIRE = registerScroll("scroll_fire");
	public static final Item SCROLL_FROST = registerScroll("scroll_frost");
	public static final Item SCROLL_HEALING = registerScroll("scroll_healing");
	public static final Item SCROLL_LIGHTNING = registerScroll("scroll_lightning");
	public static final Item SCROLL_LEAPING = registerScroll("scroll_leaping");
	public static final Item SCROLL_WIND = registerScroll("scroll_wind");
	public static final Item SCROLL_EARTH = registerScroll("scroll_earth");
	public static final Item SCROLL_LIFESTEAL = registerScroll("scroll_lifesteal");
	public static final Item SCROLL_BLINK = registerScroll("scroll_blink");
	public static final Item SCROLL_HASTE = registerScroll("scroll_haste");
	public static final Item SCROLL_SPIKES = registerScroll("scroll_spikes");

	// Accessories (Trinkets). Declared as their concrete types: each item is its own TrinketCallback.
	public static final NecklaceItem NECKLACE = register("necklace", NecklaceItem::new,
			new Item.Properties().stacksTo(1));
	public static final BangleItem BANGLE = register("bangle", BangleItem::new,
			new Item.Properties().stacksTo(1));
	public static final RingItem RING = register("ring", RingItem::new,
			new Item.Properties().stacksTo(1));

	// Block items.
	public static final Item ENGRAVING_TABLE = register("engraving_table",
			p -> new BlockItem(ModBlocks.ENGRAVING_TABLE, p), new Item.Properties());
	public static final Item SOCKETING_BENCH = register("socketing_bench",
			p -> new BlockItem(ModBlocks.SOCKETING_BENCH, p), new Item.Properties());

	/**
	 * Registers an item under {@code socket-sorcery:<name>}. The registry key has to be stamped onto
	 * the {@link Item.Properties} before the item is constructed, so the item comes from a factory
	 * rather than being passed in ready-built.
	 */
	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory,
			Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SocketSorcery.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	private static Item registerGem(String name) {
		return register(name, GemItem::new, new Item.Properties());
	}

	private static Item registerScroll(String name) {
		return register(name, ScrollItem::new, new Item.Properties().stacksTo(16));
	}

	public static void init() {
		// Bind the accessories' Trinkets behaviour so they can be equipped in the necklace / bangle /
		// ring slots. Which slots accept them is data, via data/trinkets/tags/item/socket_sorcery/.
		TrinketCallback.setCallback(NECKLACE, NECKLACE);
		TrinketCallback.setCallback(BANGLE, BANGLE);
		TrinketCallback.setCallback(RING, RING);
	}

	private ModItems() {
	}
}
