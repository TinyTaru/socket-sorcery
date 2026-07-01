package tinytaru.socketsorcery.menu;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.block.SocketingBenchBlockEntity;
import tinytaru.socketsorcery.component.SocketData;
import tinytaru.socketsorcery.item.AccessoryItem;

/**
 * A virtual {@link Container} whose slots are a live view of the gems stored in the accessory's
 * {@code sockets} component. The accessory itself lives in slot 0 of the backing bench container.
 *
 * <p>Because the gems live in exactly one place — the accessory component — editing a socket slot
 * rewrites the component directly. There is no separate gem storage to keep in sync, so gems can
 * never be duplicated or lost when the screen closes or the block is broken.
 */
public class SocketContainer implements Container {

	/** Number of socket slots shown; the largest accessory capacity. Extra slots stay inactive. */
	public static final int MAX_SLOTS = 5;

	private final Container bench;

	public SocketContainer(Container bench) {
		this.bench = bench;
	}

	private ItemStack accessory() {
		return bench.getItem(0);
	}

	/** Capacity of the currently placed accessory, or 0 if none. */
	public int capacity() {
		return accessory().getItem() instanceof AccessoryItem accessory ? accessory.capacity() : 0;
	}

	/** Number of gems currently socketed. */
	public int currentCount() {
		return AccessoryItem.getSockets(accessory()).size();
	}

	private List<ItemStack> gems() {
		return AccessoryItem.getSockets(accessory()).toMutableList();
	}

	private void store(List<ItemStack> gems) {
		ItemStack accessory = accessory();
		if (!(accessory.getItem() instanceof AccessoryItem item)) {
			return;
		}
		while (gems.size() > item.capacity()) {
			gems.remove(gems.size() - 1);
		}
		AccessoryItem.setSockets(accessory, new SocketData(gems));
		bench.setChanged();
	}

	@Override
	public int getContainerSize() {
		return MAX_SLOTS;
	}

	@Override
	public boolean isEmpty() {
		return currentCount() == 0;
	}

	@Override
	public ItemStack getItem(int slot) {
		List<ItemStack> gems = AccessoryItem.getSockets(accessory()).gems();
		return slot >= 0 && slot < gems.size() ? gems.get(slot) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		List<ItemStack> gems = gems();
		if (slot < 0 || slot >= gems.size() || amount <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack taken = gems.remove(slot);
		store(gems);
		feedback(false);
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return removeItem(slot, 1);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		List<ItemStack> gems = gems();
		if (stack.isEmpty()) {
			if (slot >= 0 && slot < gems.size()) {
				gems.remove(slot);
				store(gems);
				feedback(false);
			}
			return;
		}
		ItemStack one = stack.copy();
		one.setCount(1);
		if (slot < gems.size()) {
			gems.set(slot, one);
		} else {
			gems.add(one); // append at the next open socket
		}
		store(gems);
		feedback(true);
	}

	/** A short chime + sparkle at the bench when a gem is socketed (or removed). Server-side only. */
	private void feedback(boolean inserted) {
		if (bench instanceof SocketingBenchBlockEntity be && be.getLevel() instanceof ServerLevel level) {
			BlockPos pos = be.getBlockPos();
			level.playSound(null, pos, inserted ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.ITEM_PICKUP,
					SoundSource.BLOCKS, 0.7F, inserted ? 1.0F : 1.3F);
			level.sendParticles(inserted ? ParticleTypes.WAX_ON : ParticleTypes.GLOW,
					pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.0);
		}
	}

	@Override
	public int getMaxStackSize() {
		return 1; // one gem per socket
	}

	@Override
	public void setChanged() {
		bench.setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return bench.stillValid(player);
	}

	@Override
	public void clearContent() {
		store(new java.util.ArrayList<>());
	}
}
