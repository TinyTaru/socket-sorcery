package tinytaru.socketsorcery.component;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Data component value stored on an accessory (necklace / bangle). Holds the ordered list of
 * engraved gems socketed into it. The list is treated as immutable; mutations return a new instance.
 * Effects fire in list order, so the order is meaningful.
 */
public record SocketData(List<ItemStack> gems) {

	public static final SocketData EMPTY = new SocketData(List.of());

	public static final Codec<SocketData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemStack.CODEC.listOf().fieldOf("gems").forGetter(SocketData::gems)
	).apply(instance, SocketData::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, SocketData> STREAM_CODEC = StreamCodec.composite(
			ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), SocketData::gems,
			SocketData::new
	);

	public SocketData(List<ItemStack> gems) {
		// Defensive copy so callers can't mutate our backing list.
		this.gems = List.copyOf(gems);
	}

	public boolean isEmpty() {
		return gems.isEmpty();
	}

	public int size() {
		return gems.size();
	}

	/** Returns a mutable working copy of the gem list, for editing in the socketing UI. */
	public List<ItemStack> toMutableList() {
		List<ItemStack> copy = new ArrayList<>(gems.size());
		for (ItemStack gem : gems) {
			copy.add(gem.copy());
		}
		return copy;
	}

	// ItemStack does not implement value equality, so a plain record equals/hashCode would compare
	// gem instances by identity and break data-component comparison. Compare contents explicitly.
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SocketData other) || gems.size() != other.gems.size()) {
			return false;
		}
		for (int i = 0; i < gems.size(); i++) {
			if (!ItemStack.matches(gems.get(i), other.gems.get(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		for (ItemStack gem : gems) {
			hash = hash * 31 + (gem.getItem().hashCode() * 31 + gem.getCount());
		}
		return hash;
	}
}
