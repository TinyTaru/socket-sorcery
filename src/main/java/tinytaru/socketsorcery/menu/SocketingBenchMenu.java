package tinytaru.socketsorcery.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import tinytaru.socketsorcery.block.SocketingBenchBlockEntity;
import tinytaru.socketsorcery.item.AccessoryItem;
import tinytaru.socketsorcery.item.GemItem;
import tinytaru.socketsorcery.registry.ModBlocks;
import tinytaru.socketsorcery.registry.ModMenus;

/**
 * Menu for the Socketing Bench. Slot 0 holds the accessory; the remaining slots are a live view of
 * the gems socketed into it (see {@link SocketContainer}). Inactive slots beyond the accessory's
 * capacity simply do not render.
 */
public class SocketingBenchMenu extends AbstractContainerMenu {

	private static final int ACCESSORY_SLOT = 0;
	private static final int SOCKET_START = 1;
	private static final int SOCKET_END = SOCKET_START + SocketContainer.MAX_SLOTS;

	private final Container bench;
	private final SocketContainer sockets;
	private final ContainerLevelAccess access;

	public SocketingBenchMenu(int syncId, Inventory playerInv, SocketingBenchBlockEntity be) {
		this(syncId, playerInv, be, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()));
	}

	public SocketingBenchMenu(int syncId, Inventory playerInv, BlockPos pos) {
		this(syncId, playerInv, clientContainer(playerInv, pos),
				ContainerLevelAccess.create(playerInv.player.level(), pos));
	}

	private SocketingBenchMenu(int syncId, Inventory playerInv, Container bench, ContainerLevelAccess access) {
		super(ModMenus.SOCKETING_BENCH, syncId);
		checkContainerSize(bench, SocketingBenchBlockEntity.SIZE);
		this.bench = bench;
		this.sockets = new SocketContainer(bench);
		this.access = access;

		this.addSlot(new Slot(bench, SocketingBenchBlockEntity.SLOT_ACCESSORY, 80, 18) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.getItem() instanceof AccessoryItem;
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});

		for (int i = 0; i < SocketContainer.MAX_SLOTS; i++) {
			final int socketIndex = i;
			this.addSlot(new Slot(sockets, i, 35 + i * 22, 58) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return GemItem.isEngravedGem(stack)
							&& socketIndex < sockets.capacity()
							&& socketIndex <= sockets.currentCount();
				}

				@Override
				public boolean isActive() {
					return socketIndex < sockets.capacity();
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}
			});
		}

		addPlayerInventory(playerInv, 8, 102);
	}

	private static Container clientContainer(Inventory inv, BlockPos pos) {
		BlockEntity be = inv.player.level().getBlockEntity(pos);
		return be instanceof SocketingBenchBlockEntity bench ? bench : new SimpleContainer(SocketingBenchBlockEntity.SIZE);
	}

	private void addPlayerInventory(Inventory inv, int x, int y) {
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(inv, 9 + row * 9 + col, x + col * 18, y + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(inv, col, x + col * 18, y + 58));
		}
	}

	/** Capacity of the currently placed accessory (0 if none) — used by the screen for slot overlays. */
	public int capacity() {
		return sockets.capacity();
	}

	public int socketCount() {
		return sockets.currentCount();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack moved = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot == null || !slot.hasItem()) {
			return moved;
		}
		ItemStack inSlot = slot.getItem();
		moved = inSlot.copy();
		int invEnd = this.slots.size();

		if (index < SOCKET_END) {
			// accessory or socket -> player inventory
			if (!this.moveItemStackTo(inSlot, SOCKET_END, invEnd, true)) {
				return ItemStack.EMPTY;
			}
		} else if (inSlot.getItem() instanceof AccessoryItem) {
			if (!this.moveItemStackTo(inSlot, ACCESSORY_SLOT, ACCESSORY_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (GemItem.isEngravedGem(inSlot)) {
			if (!this.moveItemStackTo(inSlot, SOCKET_START, SOCKET_END, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}

		if (inSlot.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (inSlot.getCount() == moved.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, inSlot);
		return moved;
	}

	@Override
	public boolean stillValid(Player player) {
		return AbstractContainerMenu.stillValid(access, player, ModBlocks.SOCKETING_BENCH);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		// Gems live in the accessory component, so nothing transient to drop. The accessory simply
		// stays in the bench (slot 0) until the player removes it.
	}
}
