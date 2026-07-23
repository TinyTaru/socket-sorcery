package tinytaru.socketsorcery.item;

import java.util.function.Consumer;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.advancement.ModCriteria;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.component.SocketData;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.Modifiers;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;

/**
 * Base class for worn accessories that hold socketed gems. Each is its own
 * {@link TrinketCallback}, bound to the item in {@code ModItems.init()}, so it can be equipped
 * through Trinkets' accessory slots. (Trinkets 4 dropped the {@code TrinketItem} base class in
 * favour of tags/components plus a callback, so equipability is declared by the item tags under
 * {@code data/trinkets/tags/item/socket_sorcery/} rather than by inheritance.)
 *
 * <p>The same engraved gem produces different behaviour depending on the accessory: a necklace runs
 * each gem's passive effect components every tick; a bangle (or reacting ring) runs its active
 * components on activation. Both iterate the sockets in order, so socket order is the cast order.
 */
public abstract class AccessoryItem extends Item implements TrinketCallback {

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
		boolean fired = false;
		for (ItemStack gem : getSockets(bangle).gems()) {
			Holder.Reference<Pattern> pattern = patternOf(registries, gem);
			if (pattern != null) {
				pattern.value().castFeedback().ifPresent(feedback -> feedback.play(player, target));
				EngraveMods mods = modsOf(registries, gem);
				for (PatternEffectComponent component : pattern.value().bangleEffects()) {
					component.apply(player, target, mods);
				}
				fired = true;
			}
		}
		if (fired) {
			ModCriteria.ACTIVATE_ABILITY.trigger(player);
		}
	}

	@Override
	public void onEquip(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
		TrinketCallback.super.onEquip(stack, slot, entity);
		if (entity instanceof ServerPlayer player) {
			ModCriteria.EQUIP_ACCESSORY.trigger(player);
		}
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return !getSockets(stack).isEmpty() || super.isFoil(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		SocketData data = getSockets(stack);
		tooltip.accept(Component.translatable("tooltip.socket-sorcery.sockets", data.size(), capacity)
				.withStyle(ChatFormatting.GRAY));
		HolderLookup.Provider registries = context.registries(); // may be null; patternOf tolerates it
		for (ItemStack gem : data.gems()) {
			Holder.Reference<Pattern> pattern = patternOf(registries, gem);
			Component label = pattern != null
					? pattern.value().coloredName(pattern.key().identifier())
					: gem.getHoverName();
			tooltip.accept(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY).append(label));
		}
	}
}
