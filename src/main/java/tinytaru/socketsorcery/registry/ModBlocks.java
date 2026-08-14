package tinytaru.socketsorcery.registry;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.block.EngravingTableBlock;
import tinytaru.socketsorcery.block.CrystalLampBlock;
import tinytaru.socketsorcery.block.SocketingBenchBlock;

public final class ModBlocks {

	public static final Block ENGRAVING_TABLE = register("engraving_table", EngravingTableBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F)
					.sound(SoundType.STONE).requiresCorrectToolForDrops());

	public static final Block SOCKETING_BENCH = register("socketing_bench", SocketingBenchBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F)
					.sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

	public static final Block CRYSTAL_LAMP = register("crystal_lamp", CrystalLampBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.3F)
					.sound(SoundType.GLASS).lightLevel(state -> 15).noOcclusion());

	/** As in {@link ModItems}: the registry key must be stamped onto the properties pre-construction. */
	private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory,
			BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SocketSorcery.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	public static void init() {
	}

	private ModBlocks() {
	}
}
