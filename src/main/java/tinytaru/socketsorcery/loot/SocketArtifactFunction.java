package tinytaru.socketsorcery.loot;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.config.SocketSorceryConfig;
import tinytaru.socketsorcery.component.SocketData;
import tinytaru.socketsorcery.item.AccessoryItem;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * Loot function that turns a freshly-rolled accessory into a pre-socketed "artifact": it fills the
 * accessory with a small handful of randomly engraved gems, mirroring exactly what the engraving table +
 * socketing bench would have produced. Used by {@code ModLoot} to seed treasure chests with the occasional
 * ready-to-wear necklace / bangle.
 *
 * <p>Only the craftable engravable gems are used (never the creative-only custom gems): socketed gems can be
 * removed at the socketing bench, so seeding a custom gem would be a back-door to obtaining one.
 */
public final class SocketArtifactFunction extends LootItemConditionalFunction {

	/** Pool of gems an artifact may carry — the five craftable engravable gems only. */
	private static final Item[] ARTIFACT_GEMS = {
			ModItems.ENGRAVABLE_DIAMOND, ModItems.ENGRAVABLE_REDSTONE, ModItems.ENGRAVABLE_LAPIS,
			ModItems.ENGRAVABLE_EMERALD, ModItems.ENGRAVABLE_QUARTZ,
			ModItems.ENGRAVABLE_PRISMARINE, ModItems.ENGRAVABLE_GLOWSTONE, ModItems.ENGRAVABLE_ECHO, ModItems.ENGRAVABLE_ENDER
	};

	/**
	 * The {@code LootItemFunctionType} wrapper is gone: the registry now holds the {@link MapCodec}
	 * directly, and the per-instance hook is {@code codec()} rather than {@code getType()}.
	 */
	public static final MapCodec<SocketArtifactFunction> CODEC = RecordCodecBuilder.mapCodec(
			instance -> commonFields(instance).apply(instance, SocketArtifactFunction::new));

	public static final MapCodec<SocketArtifactFunction> TYPE = Registry.register(
			BuiltInRegistries.LOOT_FUNCTION_TYPE, SocketSorcery.id("socket_artifact"), CODEC);

	private SocketArtifactFunction(List<LootItemCondition> predicates) {
		super(predicates);
	}

	@Override
	public MapCodec<? extends LootItemConditionalFunction> codec() {
		return TYPE;
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		if (!(stack.getItem() instanceof AccessoryItem accessory)) {
			return stack;
		}
		RandomSource random = context.getRandom();
		SocketSorceryConfig config = SocketSorceryConfig.get();
		int min = Math.min(config.artifactMinGems, accessory.capacity());
		int max = Math.max(min, Math.min(config.artifactMaxGems, accessory.capacity()));
		int count = min + random.nextInt(max - min + 1);

		List<ItemStack> gems = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			Item gemItem = ARTIFACT_GEMS[random.nextInt(ARTIFACT_GEMS.length)];
			List<Identifier> patterns = List.copyOf(
					Patterns.patternsFor(context.getLevel().registryAccess(), gemItem));
			if (patterns.isEmpty()) {
				continue;
			}
			Identifier pattern = patterns.get(random.nextInt(patterns.size()));
			ItemStack gem = new ItemStack(gemItem);
			gem.set(ModComponents.ENGRAVING, new EngravingData(pattern));
			gems.add(gem);
		}

		AccessoryItem.setSockets(stack, new SocketData(gems));
		SocketSorcery.LOGGER.info("Generated pre-socketed artifact with {} gem(s)", gems.size());
		return stack;
	}

	/** Builder for use in loot pools, e.g. {@code .apply(SocketArtifactFunction.artifact())}. */
	public static LootItemConditionalFunction.Builder<?> artifact() {
		return simpleBuilder(SocketArtifactFunction::new);
	}

	/** Forces class load so the static {@link #TYPE} registers. */
	public static void init() {
	}
}
