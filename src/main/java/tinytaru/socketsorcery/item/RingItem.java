package tinytaru.socketsorcery.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;

/**
 * Ring — holds a single gem and fires its <em>active</em> behaviour automatically when the wearer
 * takes damage, on its own cooldown (see {@link RingReactions}). A third pillar alongside the
 * necklace's always-on passive and the bangle's manually-triggered active: the ring fights back for you.
 */
public class RingItem extends AccessoryItem {

	public static final int CAPACITY = 1;

	public RingItem(Properties properties) {
		super(properties, CAPACITY);
	}

	@Override
	public Component getName(ItemStack stack) {
		return stack.has(ModComponents.ENGRAVING) ? super.getName(stack)
				: Component.translatable("item.socket-sorcery.blank_ring");
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.has(ModComponents.ENGRAVING) || super.isFoil(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		EngravingData engraving = stack.get(ModComponents.ENGRAVING);
		if (engraving != null) {
			Holder.Reference<Pattern> pattern = Patterns.get(context.registries(), engraving.pattern());
			Identifier id = engraving.pattern();
			Component name = pattern != null ? pattern.value().coloredName(pattern.key().identifier())
					: Component.literal(id.toString());
			tooltip.accept(Component.translatable("tooltip.socket-sorcery.ring_trigger")
					.withStyle(ChatFormatting.GRAY).append(Component.literal(" ")).append(name));
		}
		super.appendHoverText(stack, context, display, tooltip, flag);
		if (context.registries() != null) {
			int cooldown = Cooldowns.forBangle(stack, context.registries());
			if (cooldown > 0) {
				String seconds = String.format("%.1f", cooldown / 20.0);
				tooltip.accept(Component.translatable("tooltip.socket-sorcery.cooldown", seconds).withStyle(ChatFormatting.GRAY));
			}
		}
		String hint = engraving == null ? "tooltip.socket-sorcery.ring_hint_blank"
				: "tooltip.socket-sorcery.ring_hint";
		tooltip.accept(Component.translatable(hint).withStyle(ChatFormatting.DARK_GRAY));
	}
}
