package tinytaru.socketsorcery.menu;

import java.util.Set;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import tinytaru.socketsorcery.block.EngravingTableBlockEntity;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.item.ChiselItem;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.pattern.Modifiers;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.net.EngraveResult;
import tinytaru.socketsorcery.registry.ModBlocks;
import tinytaru.socketsorcery.registry.ModComponents;
import tinytaru.socketsorcery.registry.ModMenus;

/**
 * Menu for the Engraving Table. Holds the gem / scroll / chisel slots; the chiselling minigame is
 * driven by the screen, which sends the carved cells back here for validation via
 * {@link #tryEngrave(ServerPlayer, int)}.
 */
public class EngravingTableMenu extends AbstractContainerMenu {

	private static final int SLOT_GEM = EngravingTableBlockEntity.SLOT_GEM;
	private static final int SLOT_SCROLL = EngravingTableBlockEntity.SLOT_SCROLL;
	private static final int SLOT_CHISEL = EngravingTableBlockEntity.SLOT_CHISEL;
	private static final int TABLE_SLOTS = EngravingTableBlockEntity.SIZE;

	private final Container table;
	private final ContainerLevelAccess access;
	private final HolderLookup.Provider registries;

	public EngravingTableMenu(int syncId, Inventory playerInv, EngravingTableBlockEntity be) {
		this(syncId, playerInv, be, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()));
	}

	public EngravingTableMenu(int syncId, Inventory playerInv, BlockPos pos) {
		this(syncId, playerInv, clientContainer(playerInv, pos),
				ContainerLevelAccess.create(playerInv.player.level(), pos));
	}

	private EngravingTableMenu(int syncId, Inventory playerInv, Container table, ContainerLevelAccess access) {
		super(ModMenus.ENGRAVING_TABLE, syncId);
		checkContainerSize(table, TABLE_SLOTS);
		this.table = table;
		this.access = access;
		this.registries = playerInv.player.level().registryAccess();

		this.addSlot(new Slot(table, SLOT_GEM, 20, 20) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return Patterns.isEngravableGem(registries, stack.getItem());
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		this.addSlot(new Slot(table, SLOT_SCROLL, 20, 46) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return Patterns.forScroll(registries, stack.getItem()) != null;
			}
		});
		this.addSlot(new Slot(table, SLOT_CHISEL, 20, 72) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.getItem() instanceof ChiselItem;
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});

		addPlayerInventory(playerInv, 19, 168);
	}

	private static Container clientContainer(Inventory inv, BlockPos pos) {
		BlockEntity be = inv.player.level().getBlockEntity(pos);
		return be instanceof EngravingTableBlockEntity table ? table : new SimpleContainer(TABLE_SLOTS);
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

	public ItemStack gemStack() {
		return table.getItem(SLOT_GEM);
	}

	public ItemStack scrollStack() {
		return table.getItem(SLOT_SCROLL);
	}

	public ItemStack chiselStack() {
		return table.getItem(SLOT_CHISEL);
	}

	/** The registry view this menu resolves patterns/modifiers against (valid on both sides). */
	public HolderLookup.Provider registries() {
		return registries;
	}

	/** The pattern the loaded scroll teaches, or null if no scroll present. */
	public Holder.Reference<Pattern> targetPattern() {
		ItemStack scroll = scrollStack();
		return scroll.isEmpty() ? null : Patterns.forScroll(registries, scroll.getItem());
	}

	/** True when a gem, a matching scroll and a chisel are all present and the gem is engravable. */
	public boolean canEngrave() {
		ItemStack gem = gemStack();
		Holder.Reference<Pattern> pattern = targetPattern();
		return pattern != null
				&& !gem.isEmpty()
				&& !gem.has(ModComponents.ENGRAVING)
				&& Patterns.canEngrave(registries, gem.getItem(), pattern.key().location())
				&& chiselStack().getItem() instanceof ChiselItem;
	}

	/**
	 * Server-side completion of an engraving. {@code carved} is the depth≥1 layer (must equal the
	 * symbol plus any Direction extension cells) and {@code deep} is the depth-2 layer (must decode to
	 * a valid modifier set). Consumes a scroll and chisel durability. Returns the outcome so the
	 * caller can report it to the client.
	 */
	public EngraveResult tryEngrave(ServerPlayer player, long[] carved, long[] deep) {
		ItemStack gem = gemStack();
		Holder.Reference<Pattern> holder = targetPattern();
		if (holder == null
				|| gem.isEmpty()
				|| gem.has(ModComponents.ENGRAVING)
				|| !Patterns.canEngrave(registries, gem.getItem(), holder.key().location())) {
			return EngraveResult.NOT_ENGRAVABLE;
		}
		if (!(chiselStack().getItem() instanceof ChiselItem)) {
			return EngraveResult.NO_CHISEL;
		}
		Pattern pattern = holder.value();
		Set<ResourceLocation> modifiers = Modifiers.decode(registries, pattern, deep);
		if (modifiers == null) {
			return EngraveResult.BAD_MODIFIERS; // stray or incomplete deep cells
		}
		long[] required = GridBits.or(pattern.maskBits(), deep); // symbol + Direction extension cells
		if (!GridBits.equal(carved, required)) {
			return EngraveResult.BAD_SYMBOL; // missing symbol or stray carved cells
		}

		int baseCost = GridBits.count(carved) + GridBits.count(deep);
		int reduction = chiselStack().getItem() instanceof ChiselItem chisel ? chisel.carveCostReduction() : 0;
		int cost = Math.max(1, baseCost - reduction);
		int color = pattern.color();
		access.execute((level, pos) -> {
			ItemStack engraved = gemStack().copy();
			engraved.setCount(1);
			engraved.set(ModComponents.ENGRAVING,
					new EngravingData(holder.key().location(), Modifiers.ordered(modifiers)));
			table.setItem(SLOT_GEM, engraved);

			scrollStack().shrink(1);

			ItemStack chisel = chiselStack();
			if (chisel.isDamageableItem()) {
				int damage = chisel.getDamageValue() + cost;
				if (damage >= chisel.getMaxDamage()) {
					table.setItem(SLOT_CHISEL, ItemStack.EMPTY);
					level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.7F, 1.0F);
				} else {
					chisel.setDamageValue(damage);
				}
			}

			table.setChanged();
			level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 1.4F);
			if (level instanceof ServerLevel server) {
				spawnEngraveBurst(server, pos, color);
			}
		});
		broadcastChanges();
		return EngraveResult.OK;
	}

	/** A short celebratory burst in the pattern's colour when an engraving completes. */
	private static void spawnEngraveBurst(ServerLevel level, BlockPos pos, int color) {
		double cx = pos.getX() + 0.5;
		double cy = pos.getY() + 1.0;
		double cz = pos.getZ() + 0.5;
		float r = ((color >> 16) & 0xFF) / 255.0F;
		float g = ((color >> 8) & 0xFF) / 255.0F;
		float b = (color & 0xFF) / 255.0F;
		level.sendParticles(new DustParticleOptions(new Vector3f(r, g, b), 1.2F), cx, cy, cz, 16, 0.25, 0.2, 0.25, 0.0);
		level.sendParticles(ParticleTypes.ENCHANT, cx, cy, cz, 12, 0.3, 0.3, 0.3, 0.05);
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
		int invStart = TABLE_SLOTS;
		int invEnd = this.slots.size();

		if (index < TABLE_SLOTS) {
			if (!this.moveItemStackTo(inSlot, invStart, invEnd, true)) {
				return ItemStack.EMPTY;
			}
		} else if (Patterns.isEngravableGem(registries, inSlot.getItem())) {
			if (!this.moveItemStackTo(inSlot, SLOT_GEM, SLOT_GEM + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (Patterns.forScroll(registries, inSlot.getItem()) != null) {
			if (!this.moveItemStackTo(inSlot, SLOT_SCROLL, SLOT_SCROLL + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (inSlot.getItem() instanceof ChiselItem) {
			if (!this.moveItemStackTo(inSlot, SLOT_CHISEL, SLOT_CHISEL + 1, false)) {
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
		return AbstractContainerMenu.stillValid(access, player, ModBlocks.ENGRAVING_TABLE);
	}
}
