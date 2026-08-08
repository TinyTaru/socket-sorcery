package tinytaru.socketsorcery.registry;

import java.util.Map;
import java.util.function.Function;

import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import tinytaru.socketsorcery.Balance;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.item.BangleItem;
import tinytaru.socketsorcery.item.ChiselItem;
import tinytaru.socketsorcery.item.GemDustItem;
import tinytaru.socketsorcery.item.GemItem;
import tinytaru.socketsorcery.item.NecklaceItem;
import tinytaru.socketsorcery.item.RingItem;
import tinytaru.socketsorcery.item.ScrollItem;
import tinytaru.socketsorcery.item.BlankScrollItem;
import tinytaru.socketsorcery.item.ScrollInkColor;
import tinytaru.socketsorcery.item.ScrollInkItem;

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

	// Gem dust: ground from its matching gem (one gem -> nine dust, see data/.../recipe/*_dust.json).
	// Consumed at the Engraving Table to un-engrave that same gem type.
	public static final Item RUBY_DUST = registerDust("ruby_dust");
	public static final Item SAPPHIRE_DUST = registerDust("sapphire_dust");
	public static final Item PERIDOT_DUST = registerDust("peridot_dust");
	public static final Item AMETHYST_DUST = registerDust("amethyst_dust");
	public static final Item TOPAZ_DUST = registerDust("topaz_dust");
	public static final Item DIAMOND_DUST = registerDust("diamond_dust");
	public static final Item REDSTONE_DUST = registerDust("redstone_dust");
	public static final Item LAPIS_DUST = registerDust("lapis_dust");
	public static final Item EMERALD_DUST = registerDust("emerald_dust");
	public static final Item QUARTZ_DUST = registerDust("quartz_dust");
	public static final Item PRISMARINE_DUST = registerDust("prismarine_dust");
	public static final Item GLOWSTONE_DUST = registerDust("glowstone_dust");
	public static final Item COPPER_DUST = registerDust("copper_dust");
	public static final Item ENDER_DUST = registerDust("ender_dust");

	/** Every gem dust, in the same order as {@link #GEMS} — index {@code i} matches {@code GEMS[i]}. */
	public static final Item[] GEM_DUSTS = {
			RUBY_DUST, SAPPHIRE_DUST, PERIDOT_DUST, AMETHYST_DUST, TOPAZ_DUST,
			DIAMOND_DUST, REDSTONE_DUST, LAPIS_DUST, EMERALD_DUST, QUARTZ_DUST,
			PRISMARINE_DUST, GLOWSTONE_DUST, COPPER_DUST, ENDER_DUST
	};

	private static final Map<Item, Item> DUST_BY_GEM = buildDustByGem();

	private static Map<Item, Item> buildDustByGem() {
		Map<Item, Item> map = new java.util.HashMap<>();
		for (int i = 0; i < GEMS.length; i++) {
			map.put(GEMS[i], GEM_DUSTS[i]);
		}
		return map;
	}

	/** The gem dust that un-engraves {@code gem}, or null if {@code gem} isn't one of {@link #GEMS}. */
	public static Item dustFor(Item gem) {
		return DUST_BY_GEM.get(gem);
	}

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
	public static final Item SCROLL_RING_ON_HIT = registerScroll("scroll_ring_on_hit");
	public static final Item SCROLL_RING_ON_ATTACK = registerScroll("scroll_ring_on_attack");
	public static final Item SCROLL_RING_ON_FALLING = registerScroll("scroll_ring_on_falling");
	public static final Item SCROLL_RING_ON_KILL = registerScroll("scroll_ring_on_kill");
	public static final Item SCROLL_RING_HP_THRESHOLD = registerScroll("scroll_ring_hp_threshold");
	public static final Item SCROLL_RING_IN_RADIUS = registerScroll("scroll_ring_in_radius");

	public static final Item BLANK_SCROLL = register("blank_scroll", BlankScrollItem::new, new Item.Properties().stacksTo(16));
	/** Kept registered so old worlds do not lose their pre-color scroll ink. It is no longer consumed. */
	public static final Item SCROLL_INK = register("scroll_ink", Item::new, new Item.Properties());
	public static final ScrollInkItem SCROLL_INK_RED = registerScrollInk(ScrollInkColor.RED);
	public static final ScrollInkItem SCROLL_INK_BLUE = registerScrollInk(ScrollInkColor.BLUE);
	public static final ScrollInkItem SCROLL_INK_GREEN = registerScrollInk(ScrollInkColor.GREEN);
	public static final ScrollInkItem SCROLL_INK_YELLOW = registerScrollInk(ScrollInkColor.YELLOW);
	public static final ScrollInkItem SCROLL_INK_PURPLE = registerScrollInk(ScrollInkColor.PURPLE);

	public static final ScrollInkItem[] SCROLL_INKS = {
			SCROLL_INK_RED, SCROLL_INK_BLUE, SCROLL_INK_GREEN, SCROLL_INK_YELLOW, SCROLL_INK_PURPLE
	};

	// Accessories (Trinkets). Declared as their concrete types: each item is its own TrinketCallback.
	public static final NecklaceItem NECKLACE = register("necklace", NecklaceItem::new,
			new Item.Properties().stacksTo(1));
	public static final RingItem RING = register("ring", RingItem::new,
			new Item.Properties().stacksTo(1));

	// Bangle tiers. Baseline (copper) up through gold (the original bangle, kept at id "bangle" for
	// save/recipe compatibility) to netherite; see Balance.COOLDOWN_REDUCTION_* for their bonuses.
	public static final BangleItem COPPER_BANGLE = register("copper_bangle",
			p -> new BangleItem(p, Balance.COOLDOWN_REDUCTION_COPPER_BANGLE), new Item.Properties().stacksTo(1));
	public static final BangleItem BANGLE = register("bangle",
			p -> new BangleItem(p, Balance.COOLDOWN_REDUCTION_GOLD_BANGLE), new Item.Properties().stacksTo(1));
	public static final BangleItem NETHERITE_BANGLE = register("netherite_bangle",
			p -> new BangleItem(p, Balance.COOLDOWN_REDUCTION_NETHERITE_BANGLE),
			new Item.Properties().stacksTo(1).fireResistant());

	// Block items. BlockItem no longer inherits its block's translation key automatically, so these
	// opt into "block.<ns>.<path>" (matching the block's own lang entry) instead of the default
	// "item.<ns>.<path>", which would otherwise show up as a raw untranslated key in tooltips.
	public static final Item ENGRAVING_TABLE = register("engraving_table",
			p -> new BlockItem(ModBlocks.ENGRAVING_TABLE, p), new Item.Properties().useBlockDescriptionPrefix());
	public static final Item SOCKETING_BENCH = register("socketing_bench",
			p -> new BlockItem(ModBlocks.SOCKETING_BENCH, p), new Item.Properties().useBlockDescriptionPrefix());

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

	private static Item registerDust(String name) {
		return register(name, GemDustItem::new, new Item.Properties());
	}

	private static Item registerScroll(String name) {
		return register(name, ScrollItem::new, new Item.Properties().stacksTo(16));
	}

	private static ScrollInkItem registerScrollInk(ScrollInkColor color) {
		return register("scroll_ink_" + color.id(), p -> new ScrollInkItem(p, color), new Item.Properties());
	}

	public static Item scrollInk(ScrollInkColor color) {
		return switch (color) {
			case RED -> SCROLL_INK_RED;
			case BLUE -> SCROLL_INK_BLUE;
			case GREEN -> SCROLL_INK_GREEN;
			case YELLOW -> SCROLL_INK_YELLOW;
			case PURPLE -> SCROLL_INK_PURPLE;
		};
	}

	public static void init() {
		// Bind the accessories' Trinkets behaviour so they can be equipped in the necklace / bangle /
		// ring slots. Which slots accept them is data, via data/trinkets/tags/item/socket_sorcery/.
		TrinketCallback.setCallback(NECKLACE, NECKLACE);
		TrinketCallback.setCallback(COPPER_BANGLE, COPPER_BANGLE);
		TrinketCallback.setCallback(BANGLE, BANGLE);
		TrinketCallback.setCallback(NETHERITE_BANGLE, NETHERITE_BANGLE);
		TrinketCallback.setCallback(RING, RING);
	}

	private ModItems() {
	}
}
