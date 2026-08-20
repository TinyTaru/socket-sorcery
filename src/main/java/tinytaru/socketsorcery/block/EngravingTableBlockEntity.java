package tinytaru.socketsorcery.block;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import tinytaru.socketsorcery.menu.EngravingTableMenu;
import tinytaru.socketsorcery.registry.ModBlockEntities;
import tinytaru.socketsorcery.util.ImplementedInventory;

/**
 * Holds the gem, scroll and chisel placed in an Engraving Table. The chiselling itself happens in
 * the screen; on completion the gem in {@link #SLOT_GEM} is engraved in place.
 */
public class EngravingTableBlockEntity extends BlockEntity
		implements ImplementedInventory, ExtendedMenuProvider<BlockPos> {

	public static final int SLOT_GEM = 0;
	public static final int SLOT_SCROLL = 1;
	public static final int SLOT_CHISEL = 2;
	public static final int SIZE = 3;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

	public EngravingTableBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ENGRAVING_TABLE, pos, state);
	}

	@Override
	public NonNullList<ItemStack> getItems() {
		return items;
	}

	/**
	 * The tabletop renderer reads the workpiece directly from this inventory. Keep that client copy
	 * current as slots change or a chisel stroke rewrites the gem's engraving component.
	 */
	@Override
	public void setChanged() {
		super.setChanged();
		Level level = getLevel();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.socket-sorcery.engraving_table");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
		return new EngravingTableMenu(syncId, inventory, this);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return this.worldPosition;
	}

	/** Drops the placed gem / scroll / chisel when the table is broken (see {@link StationBlock}). */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (this.level != null) {
			Containers.dropContents(this.level, pos, this);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		ContainerHelper.loadAllItems(input, this.items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, this.items);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}
