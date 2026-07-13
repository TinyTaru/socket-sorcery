package tinytaru.socketsorcery.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.block.EngravingTableBlock;
import tinytaru.socketsorcery.block.SocketingBenchBlock;

public final class ModBlocks {

	public static final Block ENGRAVING_TABLE = register("engraving_table", new EngravingTableBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F)
					.sound(SoundType.STONE).requiresCorrectToolForDrops()));

	public static final Block SOCKETING_BENCH = register("socketing_bench", new SocketingBenchBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F)
					.sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

	private static Block register(String name, Block block) {
		return Registry.register(BuiltInRegistries.BLOCK, SocketSorcery.id(name), block);
	}

	public static void init() {
	}

	private ModBlocks() {
	}
}
