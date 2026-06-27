package tinytaru.socketsorcery.registry;

import java.util.Set;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import tinytaru.socketsorcery.loot.SocketArtifactFunction;

/**
 * Injects mod items into vanilla chest loot tables.
 *
 * <p>Two injections, both gated by a chance roll:
 * <ul>
 *   <li>Pattern scrolls into a handful of common structures, so the engraving loop starts with a found scroll.
 *   <li>The necklace / bangle accessories into high-tier "treasure" tables, so the mod's core tools can also be
 *       found as a rare prize (they are craftable too). Sources are themed: jewelry-style treasure for the
 *       necklace, combat-flavoured treasure for the bangle.
 * </ul>
 */
public final class ModLoot {

	private static final Set<ResourceKey<LootTable>> SCROLL_TARGETS = Set.of(
			BuiltInLootTables.SIMPLE_DUNGEON,
			BuiltInLootTables.ABANDONED_MINESHAFT,
			BuiltInLootTables.DESERT_PYRAMID,
			BuiltInLootTables.JUNGLE_TEMPLE,
			BuiltInLootTables.STRONGHOLD_CORRIDOR,
			BuiltInLootTables.STRONGHOLD_LIBRARY,
			BuiltInLootTables.WOODLAND_MANSION,
			BuiltInLootTables.BURIED_TREASURE);

	/** Treasure tables that may yield an heirloom necklace. */
	private static final Set<ResourceKey<LootTable>> NECKLACE_TARGETS = Set.of(
			BuiltInLootTables.END_CITY_TREASURE,
			BuiltInLootTables.WOODLAND_MANSION,
			BuiltInLootTables.ANCIENT_CITY);

	/** Treasure tables that may yield a champion's bangle. */
	private static final Set<ResourceKey<LootTable>> BANGLE_TARGETS = Set.of(
			BuiltInLootTables.BASTION_TREASURE,
			BuiltInLootTables.NETHER_BRIDGE,
			BuiltInLootTables.END_CITY_TREASURE);

	/** Per-chest chance for an accessory to appear in an applicable treasure table. Tune to taste. */
	private static final float ACCESSORY_CHANCE = 0.10F;

	public static void init() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source) -> {
			if (!source.isBuiltin()) {
				return;
			}
			if (SCROLL_TARGETS.contains(key)) {
				tableBuilder.withPool(LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1.0F))
						.when(LootItemRandomChanceCondition.randomChance(0.5F))
						.add(LootItem.lootTableItem(ModItems.SCROLL_FIRE).setWeight(2))
						.add(LootItem.lootTableItem(ModItems.SCROLL_FROST).setWeight(2))
						.add(LootItem.lootTableItem(ModItems.SCROLL_HEALING).setWeight(2))
						.add(LootItem.lootTableItem(ModItems.SCROLL_LIGHTNING).setWeight(2))
						.add(LootItem.lootTableItem(ModItems.SCROLL_LEAPING).setWeight(2))
						.add(EmptyLootItem.emptyItem().setWeight(10)));
			}
			if (NECKLACE_TARGETS.contains(key)) {
				tableBuilder.withPool(accessoryPool(ModItems.NECKLACE));
			}
			if (BANGLE_TARGETS.contains(key)) {
				tableBuilder.withPool(accessoryPool(ModItems.BANGLE));
			}
		});
	}

	/**
	 * A single-roll pool that drops one accessory with {@link #ACCESSORY_CHANCE} probability. Every dropped
	 * accessory is a pre-socketed "artifact" (crafting is how you get blank ones), so a treasure find is
	 * always a ready-to-wear prize.
	 */
	private static LootPool.Builder accessoryPool(Item accessory) {
		return LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1.0F))
				.when(LootItemRandomChanceCondition.randomChance(ACCESSORY_CHANCE))
				.add(LootItem.lootTableItem(accessory).apply(SocketArtifactFunction.artifact()));
	}

	private ModLoot() {
	}
}
