package tinytaru.socketsorcery.item;

import java.util.List;

import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.component.SocketData;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.Modifiers;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;

/**
 * Base class for worn accessories that hold socketed gems. A {@link dev.emi.trinkets.api.Trinket},
 * via {@link TrinketItem}, so it can be equipped through Trinkets' accessory slots.
 *
 * <p>The same engraved gem produces different behaviour depending on the accessory: a necklace runs
 * each gem's passive effect components every tick; a bangle (or reacting ring) runs its active
 * components on activation. Both iterate the sockets in order, so socket order is the cast order.
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

	static Holder.Reference<Pattern> patternOf(HolderLookup.Provider registries, ItemStack gem) {
		EngravingData data = gem.get(ModComponents.ENGRAVING);
		return data == null ? null : Patterns.get(registries, data.pattern());
	}

	private static EngraveMods modsOf(HolderLookup.Provider registries, ItemStack gem) {
		EngravingData data = gem.get(ModComponents.ENGRAVING);
		return data == null ? EngraveMods.NONE : Modifiers.toMods(registries, data.modifiers());
	}

	/** Runs every socketed gem's passive (necklace) effect components, in socket order. Server side. */
	public static void runNecklace(ServerPlayer player, ItemStack necklace) {
		HolderLookup.Provider registries = player.registryAccess();
		// Passives have no aim target: a synthetic miss at the wearer's feet.
		HitResult miss = BlockHitResult.miss(player.position(), Direction.UP, BlockPos.containing(player.position()));
		for (ItemStack gem : getSockets(necklace).gems()) {
			Holder.Reference<Pattern> pattern = patternOf(registries, gem);
			if (pattern != null) {
				EngraveMods mods = modsOf(registries, gem);
				for (PatternEffectComponent component : pattern.value().necklaceEffects()) {
					component.apply(player, miss, mods);
				}
			}
		}
	}

	/**
	 * Runs every socketed gem's active (bangle/ring) effect components against the target, in socket
	 * order, firing each pattern's cast feedback once per gem.
	 */
	public static void runBangle(ServerPlayer player, ItemStack bangle, HitResult target) {
		HolderLookup.Provider registries = player.registryAccess();
		for (ItemStack gem : getSockets(bangle).gems()) {
			Holder.Reference<Pattern> pattern = patternOf(registries, gem);
			if (pattern != null) {
				pattern.value().castFeedback().ifPresent(feedback -> feedback.play(player, target));
				EngraveMods mods = modsOf(registries, gem);
				for (PatternEffectComponent component : pattern.value().bangleEffects()) {
					component.apply(player, target, mods);
				}
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
		HolderLookup.Provider registries = context.registries(); // may be null; patternOf tolerates it
		for (ItemStack gem : data.gems()) {
			Holder.Reference<Pattern> pattern = patternOf(registries, gem);
			Component label = pattern != null
					? pattern.value().coloredName(pattern.key().location())
					: gem.getHoverName();
			tooltip.add(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY).append(label));
		}
	}
}
