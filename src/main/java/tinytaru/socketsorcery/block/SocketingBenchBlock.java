package tinytaru.socketsorcery.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SocketingBenchBlock extends StationBlock {

	public static final MapCodec<SocketingBenchBlock> CODEC = simpleCodec(SocketingBenchBlock::new);

	public SocketingBenchBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<SocketingBenchBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SocketingBenchBlockEntity(pos, state);
	}
}
