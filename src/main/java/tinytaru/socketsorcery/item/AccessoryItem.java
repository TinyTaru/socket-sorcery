package tinytaru.socketsorcery.item;

import java.util.List;

import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.component.SocketData;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.Modifiers;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;

/**
 * Base class for worn accessories that hold socketed gems. A {@link dev.emi.trinkets.api.Trinket},
 * via {@link TrinketItem}, so it can be equipped through Trinkets' accessory slots.
 *
 * <p>The same engraved gem produces different behaviour depending on the accessory: a necklace runs
 * each gem's passive every tick; a bangle runs each gem's active on the keybind. Both iterate the
 * sockets in order, so socket order is the cast order.
 */
public abstract class AccessoryItem extends TrinketItem {

	private final int capacity;

	protected AccessoryItem(Properties properties, int capacity) {
		super(properties);
		this.capacity = capacity;
	}

	/** Maximum number of gems that may be socketed into this accessory. */
	public int capacity() {
		return capacity;
	}

	public static SocketData getSockets(ItemStack stack) {
		return stack.getOrDefault(ModComponents.SOCKETS, SocketData.EMPTY);
	}

	public static void setSockets(ItemStack stack, SocketData data) {
		if (data == null || data.isEmpty()) {
			stack.remove(ModComponents.SOCKETS);
		} else {
			stack.set(ModComponents.SOCKETS, data);
		}
	}

	static Pattern patternOf(ItemStack gem) {
		EngravingData data = gem.get(ModComponents.ENGRAVING);
		return data == null ? null : Patterns.get(data.pattern());
	}

	private static EngraveMods modsOf(ItemStack gem) {
		EngravingData data = gem.get(ModComponents.ENGRAVING);
		return data == null ? EngraveMods.NONE : Modifiers.toMods(data.modifiers());
	}

	/** Runs every socketed gem's passive (necklace) behaviour, in socket order. Server side. */
	public static void runNecklace(ServerPlayer player, ItemStack necklace) {
		List<ItemStack> gems = getSockets(necklace).gems();
		for (int i = 0; i < gems.size(); i++) {
			Pattern pattern = patternOf(gems.get(i));
			if (pattern != null) {
				pattern.effect().onNecklaceTick(player, modsOf(gems.get(i)), i);
			}
		}
	}

	/** Runs every socketed gem's active (bangle) behaviour against the look target, in socket order. */
	public static void runBangle(ServerPlayer player, ItemStack bangle, HitResult target) {
		List<ItemStack> gems = getSockets(bangle).gems();
		for (int i = 0; i < gems.size(); i++) {
			Pattern pattern = patternOf(gems.get(i));
			if (pattern != null) {
				pattern.effect().onBangleActivate(player, target, modsOf(gems.get(i)), i);
			}
		}
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return !getSockets(stack).isEmpty() || super.isFoil(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		SocketData data = getSockets(stack);
		tooltip.add(Component.translatable("tooltip.socket-sorcery.sockets", data.size(), capacity)
				.withStyle(ChatFormatting.GRAY));
		for (ItemStack gem : data.gems()) {
			Pattern pattern = patternOf(gem);
			Component label = pattern != null ? pattern.coloredName() : gem.getHoverName();
			tooltip.add(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY).append(label));
		}
	}
}
