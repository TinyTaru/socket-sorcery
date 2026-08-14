package tinytaru.socketsorcery.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tinytaru.socketsorcery.component.CrystalLampData;
import tinytaru.socketsorcery.registry.ModComponents;

/** A bright decorative lamp whose five etched glass panels are rendered by its block entity. */
public class CrystalLampBlock extends BaseEntityBlock {

	public static final MapCodec<CrystalLampBlock> CODEC = simpleCodec(CrystalLampBlock::new);

	public CrystalLampBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<CrystalLampBlock> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CrystalLampBlockEntity(pos, state);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.getBlockEntity(pos) instanceof CrystalLampBlockEntity lamp) {
			lamp.setLampData(stack.getOrDefault(ModComponents.CRYSTAL_LAMP, CrystalLampData.EMPTY));
		}
	}
}
