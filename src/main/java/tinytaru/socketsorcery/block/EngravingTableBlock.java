package tinytaru.socketsorcery.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EngravingTableBlock extends StationBlock {

	public static final MapCodec<EngravingTableBlock> CODEC = simpleCodec(EngravingTableBlock::new);

	public EngravingTableBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<EngravingTableBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EngravingTableBlockEntity(pos, state);
	}
}
