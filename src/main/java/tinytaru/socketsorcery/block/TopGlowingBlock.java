package tinytaru.socketsorcery.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** A horizontally orientable glowing block used by the top variants. */
public class TopGlowingBlock extends HorizontalDirectionalBlock {

	public static final MapCodec<TopGlowingBlock> CODEC = simpleCodec(TopGlowingBlock::new);

	public TopGlowingBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<TopGlowingBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		builder.add(FACING);
	}
}
