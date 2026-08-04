package tinytaru.socketsorcery.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import tinytaru.socketsorcery.advancement.ModCriteria;
import tinytaru.socketsorcery.block.EngravingTableBlockEntity;
import tinytaru.socketsorcery.component.CarvingData;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.item.ChiselItem;
import tinytaru.socketsorcery.pattern.Carvings;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.pattern.Modifier;
import tinytaru.socketsorcery.pattern.Modifiers;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.net.ChiselC2SPayload;
import tinytaru.socketsorcery.net.EngraveFeedbackS2CPayload;
import tinytaru.socketsorcery.registry.ModBlocks;
import tinytaru.socketsorcery.registry.ModComponents;
import tinytaru.socketsorcery.registry.ModItems;
import tinytaru.socketsorcery.registry.ModMenus;

/**
 * Menu for the Engraving Table. Holds the gem / scroll / chisel slots and owns the chiselling
 * minigame: the screen sends one {@link ChiselC2SPayload} per click and {@link #chisel} cuts the gem
 * in the table there and then, writing the carve straight onto the item. The gem itself is therefore
 * the only state the minigame has — pull it out half-carved and the cuts come with it.
 *
 * <p>Costs are charged as the carve happens rather than at a confirmation step: the chisel takes a
 * point of durability per cut (past the flat discount its tier grants), easing a cut back costs a
 * gem dust each time, and the scroll is spent by whichever stroke first closes the base symbol.
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
				return Patterns.isEngravableGem(registries, stack.getItem())
						|| Patterns.isEngravableRing(registries, stack.getItem());
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

	/**
	 * The pattern being chiselled: whatever the gem is already cut for, else the one the loaded scroll
	 * teaches. A gem that has been struck once carries its own pattern, so the scroll can be taken
	 * back out and swapped freely mid-carve — it only has to be back in place for whichever stroke
	 * closes the base symbol, since that's the one that spends it.
	 */
	public Holder.Reference<Pattern> targetPattern() {
		ItemStack gem = gemStack();
		Identifier carvedFor = gem.isEmpty() ? null : Carvings.patternId(gem);
		if (carvedFor != null) {
			return Patterns.get(registries, carvedFor);
		}
		ItemStack scroll = scrollStack();
		return scroll.isEmpty() ? null : Patterns.forScroll(registries, scroll.getItem());
	}

	/** True when a chisel and a gem this pattern accepts are both present, so strokes will land. */
	public boolean canCarve() {
		ItemStack gem = gemStack();
		Holder.Reference<Pattern> pattern = targetPattern();
		return pattern != null
				&& !gem.isEmpty()
				&& Patterns.canEngrave(registries, gem.getItem(), pattern.key().identifier())
				&& chiselStack().getItem() instanceof ChiselItem;
	}

	/** The eraser item spent easing a cut: matching gem dust, or gold nuggets for rings. */
	public Item currentGemDust() {
		ItemStack gem = gemStack();
		if (gem.isEmpty()) {
			return null;
		}
		return gem.getItem() instanceof tinytaru.socketsorcery.item.RingItem
				? Items.GOLD_NUGGET : ModItems.dustFor(gem.getItem());
	}

	/** Total count of {@code dust} across the player-inventory slots this menu tracks. */
	public int countDust(Item dust) {
		if (dust == null) {
			return 0;
		}
		int total = 0;
		for (int i = TABLE_SLOTS; i < this.slots.size(); i++) {
			ItemStack stack = this.slots.get(i).getItem();
			if (stack.is(dust)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	/** Shrinks {@code amount} of {@code dust} out of the player-inventory slots. Caller must have already
	 *  verified {@link #countDust} covers it. */
	private void consumeDust(Item dust, int amount) {
		int remaining = amount;
		for (int i = TABLE_SLOTS; i < this.slots.size() && remaining > 0; i++) {
			Slot slot = this.slots.get(i);
			ItemStack stack = slot.getItem();
			if (stack.is(dust)) {
				int take = Math.min(remaining, stack.getCount());
				stack.shrink(take);
				slot.setChanged();
				remaining -= take;
			}
		}
	}

	/** True if the player can cover {@code strokes} eased cuts — gems with no dust of their own ease free. */
	public boolean canAffordEase(int strokes) {
		Item dust = currentGemDust();
		return dust == null || countDust(dust) >= strokes;
	}

	/**
	 * Applies one chisel stroke to the gem in the table and saves the result onto it: an
	 * {@link EngravingData} once the cells form a finished engraving, otherwise a {@link CarvingData}
	 * of the cuts so far (and neither once the gem is bare again). Returns what the stroke finished,
	 * for the screen to celebrate, or null when nothing changed.
	 *
	 * <p>Every cost is checked before anything is charged, so a stroke either lands in full or not at
	 * all — never a half-charged one. {@link #mutateCell} itself charges nothing; it only mutates a
	 * local copy of the cells, which is simply discarded if the stroke turns out unaffordable.
	 */
	public EngraveFeedbackS2CPayload chisel(ServerPlayer player, int cell, ChiselC2SPayload.Action action) {
		if (!canCarve()) {
			return null;
		}
		ItemStack gem = gemStack();
		Holder.Reference<Pattern> holder = targetPattern();
		Pattern pattern = holder.value();
		Identifier patternId = holder.key().identifier();

		CarvingData before = Carvings.on(registries, gem);
		long[] carved = before == null ? GridBits.empty() : GridBits.copy(before.carved());
		long[] deep = before == null ? GridBits.empty() : GridBits.copy(before.deep());
		boolean symbolWasWhole = before != null && GridBits.subset(pattern.maskBits(), carved);
		// Which modifiers were already standing, read permissively (see Modifiers#formedSubset) so a
		// stray half-cut modifier elsewhere in the carve doesn't hide a different one that's complete.
		Set<Identifier> hadModifiers = Modifiers.formedSubset(registries, pattern, deep);
		int strokesBefore = GridBits.count(carved) + GridBits.count(deep);

		int easeCost = mutateCell(cell, action, carved, deep);
		if (easeCost < 0) {
			return null; // off the grid, already at the limit, or nothing to clear
		}
		Set<Identifier> modifiers = Carvings.acceptedModifiers(registries, pattern, carved, deep);
		boolean patternComplete = !symbolWasWhole && modifiers != null;

		if (easeCost > 0 && !canAffordEase(easeCost)) {
			return null;
		}
		if (patternComplete && scrollStack().isEmpty()) {
			return null; // the closing cut is what spends the scroll; without one, it doesn't land
		}

		if (easeCost > 0) {
			spendDust(easeCost);
		}
		if (patternComplete) {
			scrollStack().shrink(1);
		}
		if (action == ChiselC2SPayload.Action.DEEPEN) {
			damageChisel(strokesBefore);
		}

		writeCarve(gem, patternId, carved, deep, modifiers);
		table.setChanged();
		broadcastChanges();

		if (modifiers == null) {
			return null; // the carve is unfinished — nothing to celebrate yet
		}
		List<Identifier> formed = new ArrayList<>(modifiers);
		formed.removeAll(hadModifiers);
		if (symbolWasWhole && formed.isEmpty()) {
			return null; // a shape it had already reached (a modifier eased back off, say)
		}
		celebrate(pattern, patternComplete, formed);
		ModCriteria.ENGRAVE.trigger(player, patternId, !modifiers.isEmpty());
		return new EngraveFeedbackS2CPayload(patternComplete, Modifiers.ordered(formed));
	}

	/**
	 * Mutates {@code carved}/{@code deep} for a single stroke and returns its dust cost — 0 for a cut,
	 * 1 for easing one cell, the stroke count standing beforehand for a full clear — or -1 if the
	 * stroke can't land at all: off the grid, a cut already at its limit, an ease on a bare cell, or a
	 * clear with nothing to undo. Charges nothing itself, which is what makes it safe to call before
	 * costs are known to be affordable.
	 */
	private int mutateCell(int cell, ChiselC2SPayload.Action action, long[] carved, long[] deep) {
		if (action == ChiselC2SPayload.Action.CLEAR) {
			int strokes = GridBits.count(carved) + GridBits.count(deep);
			if (strokes == 0) {
				return -1;
			}
			Arrays.fill(carved, 0L);
			Arrays.fill(deep, 0L);
			return strokes;
		}
		if (cell < 0 || cell >= Pattern.GRID * Pattern.GRID) {
			return -1;
		}
		ItemStack item = table.getItem(SLOT_GEM);
		Holder.Reference<Pattern> target = targetPattern();
		if (item.getItem() instanceof tinytaru.socketsorcery.item.RingItem && target != null
				&& !ringCellAllowed(cell)) {
			return -1;
		}
		int depth = Carvings.depth(carved, deep, cell);
		if (action == ChiselC2SPayload.Action.EASE) {
			if (depth == 0) {
				return -1;
			}
			if (depth == 2) {
				GridBits.clearIndex(deep, cell);
			} else {
				GridBits.clearIndex(carved, cell);
			}
			return 1;
		}
		if (depth >= 2) {
			return -1;
		}
		if (depth == 0) {
			GridBits.setIndex(carved, cell);
		} else {
			GridBits.setIndex(deep, cell);
		}
		return 0;
	}

	private boolean ringCellAllowed(int cell) {
		int row = cell / Pattern.GRID;
		int col = cell % Pattern.GRID;
		// The ring face is the pure-black 6x4 pixel panel x=5..10, y=9..12.
		return col >= 5 && col <= 10 && row >= 9 && row <= 12;
	}

	/**
	 * Saves the carve onto the gem: a finished engraving when {@code modifiers} is non-null (the cuts
	 * are then re-derivable from it), the raw cuts while it's null, and neither once the gem is bare.
	 */
	private void writeCarve(ItemStack gem, Identifier patternId, long[] carved, long[] deep, Set<Identifier> modifiers) {
		if (modifiers != null) {
			gem.set(ModComponents.ENGRAVING, new EngravingData(patternId, Modifiers.ordered(modifiers)));
			gem.remove(ModComponents.CARVING);
		} else {
			gem.remove(ModComponents.ENGRAVING);
			if (GridBits.isEmpty(carved)) {
				gem.remove(ModComponents.CARVING);
			} else {
				gem.set(ModComponents.CARVING, new CarvingData(patternId, carved, deep));
			}
		}
	}

	/** Spends {@code amount} of the current gem's dust — a no-op for gems with no dust of their own. */
	private void spendDust(int amount) {
		Item dust = currentGemDust();
		if (dust != null) {
			consumeDust(dust, amount);
		}
	}

	/**
	 * Takes a point of chisel durability for one cut. A tier's flat discount is spent up front — the
	 * first {@code carveCostReduction()} cuts of a carve are free — so a whole pattern still costs what
	 * it used to. A chisel that runs out mid-carve breaks; the cuts already made stay on the gem.
	 */
	private void damageChisel(int strokesSoFar) {
		ItemStack chisel = chiselStack();
		if (!(chisel.getItem() instanceof ChiselItem tool)
				|| !chisel.isDamageableItem()
				|| strokesSoFar < tool.carveCostReduction()) {
			return;
		}
		int damage = chisel.getDamageValue() + 1;
		if (damage < chisel.getMaxDamage()) {
			chisel.setDamageValue(damage);
			return;
		}
		table.setItem(SLOT_CHISEL, ItemStack.EMPTY);
		access.execute((level, pos) ->
				level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 0.7F, 1.0F));
	}

	/** Sound and sparks at the table for the pattern falling into place, or a modifier joining it. */
	private void celebrate(Pattern pattern, boolean patternComplete, List<Identifier> formed) {
		int color = patternComplete ? pattern.color() : modifierColor(formed, pattern.color());
		access.execute((level, pos) -> {
			level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS,
					0.6F, patternComplete ? 1.4F : 1.8F);
			if (level instanceof ServerLevel server) {
				spawnEngraveBurst(server, pos, color, patternComplete ? 16 : 6);
			}
		});
	}

	private int modifierColor(List<Identifier> formed, int fallback) {
		for (Identifier id : formed) {
			Holder.Reference<Modifier> modifier = Modifiers.get(registries, id);
			if (modifier != null) {
				return modifier.value().color();
			}
		}
		return fallback;
	}

	/** A short celebratory burst above the table, in the colour of whatever just fell into place. */
	private static void spawnEngraveBurst(ServerLevel level, BlockPos pos, int color, int count) {
		double cx = pos.getX() + 0.5;
		double cy = pos.getY() + 1.0;
		double cz = pos.getZ() + 0.5;
		// DustParticleOptions takes the packed RGB directly now (it used to need a Vector3f).
		level.sendParticles(new DustParticleOptions(color, 1.2F), cx, cy, cz, count, 0.25, 0.2, 0.25, 0.0);
		level.sendParticles(ParticleTypes.ENCHANT, cx, cy, cz, count * 3 / 4, 0.3, 0.3, 0.3, 0.05);
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
		} else if (Patterns.isEngravableGem(registries, inSlot.getItem())
				|| Patterns.isEngravableRing(registries, inSlot.getItem())) {
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
