package tinytaru.socketsorcery.block;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tinytaru.socketsorcery.menu.EngravingTableMenu;
import tinytaru.socketsorcery.registry.ModBlockEntities;
import tinytaru.socketsorcery.util.ImplementedInventory;

/**
 * Holds the gem, scroll and chisel placed in an Engraving Table. The chiselling itself happens in
 * the screen; on completion the gem in {@link #SLOT_GEM} is engraved in place.
 */
public class EngravingTableBlockEntity extends BlockEntity
		implements ImplementedInventory, ExtendedScreenHandlerFactory<BlockPos> {

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

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		ContainerHelper.loadAllItems(tag, this.items, registries);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, this.items, registries);
	}
}
