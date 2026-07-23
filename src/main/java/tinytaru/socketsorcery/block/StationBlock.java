package tinytaru.socketsorcery.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Shared behaviour for the mod's workstation blocks: open the block entity's menu on right-click,
 * render as a normal model, and drop their contents when broken.
 */
public abstract class StationBlock extends BaseEntityBlock {

	protected StationBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
			player.openMenu(provider);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * {@code onRemove} split in two: neighbour updates land here (after the block is gone), while
	 * dropping the contents moved onto the block entity's {@code preRemoveSideEffects}, which still
	 * runs while the block entity exists. This mirrors what vanilla's containers now do.
	 */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}
}
