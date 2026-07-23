package tinytaru.socketsorcery.compat.jei;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * One accessory's socketing, as JEI displays it: the accessory plus its capacity in engraved gems
 * (any engraved gem fits any socket) producing the accessory fully socketed. Synthetic: socketing
 * happens at the bench, gem by gem.
 *
 * @param accessory the empty accessory
 * @param gems      one representative engraved gem per gem type, offered for every socket
 * @param capacity  how many sockets this accessory has
 * @param filled    the fully-socketed result, carrying real socket data so JEI shows the composited
 *                  icon
 */
public record SocketingRecipe(ItemStack accessory, List<ItemStack> gems, int capacity, ItemStack filled) {
}
