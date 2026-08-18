package tinytaru.socketsorcery.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import tinytaru.socketsorcery.component.CrystalLampData;
import tinytaru.socketsorcery.registry.ModBlockEntities;
import tinytaru.socketsorcery.registry.ModComponents;

/** Stores the five masks and exposes them as an implicit component for placement, drops, and sync. */
public class CrystalLampBlockEntity extends BlockEntity {

	private CrystalLampData lampData = CrystalLampData.EMPTY;

	public CrystalLampBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CRYSTAL_LAMP, pos, state);
	}

	public CrystalLampData lampData() {
		return lampData;
	}

	public void setLampData(CrystalLampData lampData) {
		this.lampData = lampData == null ? CrystalLampData.EMPTY : lampData;
		setChanged();
		Level level = getLevel();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		lampData = input.read("lamp", CrystalLampData.CODEC).orElse(CrystalLampData.EMPTY);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("lamp", CrystalLampData.CODEC, lampData);
	}

	/**
	 * The projection is client-rendered, so the masks must be sent both when the lamp changes and
	 * when its chunk is first loaded after joining a world.
	 */
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		lampData = components.getOrDefault(ModComponents.CRYSTAL_LAMP, CrystalLampData.EMPTY);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(ModComponents.CRYSTAL_LAMP, lampData);
	}
}
