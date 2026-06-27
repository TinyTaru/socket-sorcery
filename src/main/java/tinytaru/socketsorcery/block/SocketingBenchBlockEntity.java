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
import tinytaru.socketsorcery.menu.SocketingBenchMenu;
import tinytaru.socketsorcery.registry.ModBlockEntities;
import tinytaru.socketsorcery.util.ImplementedInventory;

/**
 * Holds the single accessory being edited at a Socketing Bench. The gems themselves are stored in
 * the accessory's {@code sockets} component (the menu edits that component directly), so this block
 * entity only needs the one accessory slot.
 */
public class SocketingBenchBlockEntity extends BlockEntity
		implements ImplementedInventory, ExtendedScreenHandlerFactory<BlockPos> {

	public static final int SLOT_ACCESSORY = 0;
	public static final int SIZE = 1;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

	public SocketingBenchBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SOCKETING_BENCH, pos, state);
	}

	@Override
	public NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	public int getMaxStackSize() {
		return 1; // only ever holds a single accessory
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.socket-sorcery.socketing_bench");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
		return new SocketingBenchMenu(syncId, inventory, this);
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
