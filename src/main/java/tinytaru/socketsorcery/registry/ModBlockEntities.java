package tinytaru.socketsorcery.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.block.EngravingTableBlockEntity;
import tinytaru.socketsorcery.block.SocketingBenchBlockEntity;

public final class ModBlockEntities {

	public static final BlockEntityType<EngravingTableBlockEntity> ENGRAVING_TABLE = register("engraving_table",
			BlockEntityType.Builder.of(EngravingTableBlockEntity::new, ModBlocks.ENGRAVING_TABLE));

	public static final BlockEntityType<SocketingBenchBlockEntity> SOCKETING_BENCH = register("socketing_bench",
			BlockEntityType.Builder.of(SocketingBenchBlockEntity::new, ModBlocks.SOCKETING_BENCH));

	private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.Builder<T> builder) {
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SocketSorcery.id(name), builder.build(null));
	}

	public static void init() {
	}

	private ModBlockEntities() {
	}
}
