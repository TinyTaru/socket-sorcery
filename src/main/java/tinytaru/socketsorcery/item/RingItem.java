package tinytaru.socketsorcery.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

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
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		int cooldown = Cooldowns.forBangle(stack);
		if (cooldown > 0) {
			String seconds = String.format("%.1f", cooldown / 20.0);
			tooltip.add(Component.translatable("tooltip.socket-sorcery.cooldown", seconds).withStyle(ChatFormatting.GRAY));
		}
		tooltip.add(Component.translatable("tooltip.socket-sorcery.ring_hint").withStyle(ChatFormatting.DARK_GRAY));
	}
}
