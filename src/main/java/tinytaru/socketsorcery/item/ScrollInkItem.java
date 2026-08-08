package tinytaru.socketsorcery.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A colored pigment consumed while a pattern is painted onto a blank scroll. */
public final class ScrollInkItem extends Item {
	private final ScrollInkColor color;

	public ScrollInkItem(Properties properties, ScrollInkColor color) {
		super(properties);
		this.color = color;
	}

	public ScrollInkColor color() {
		return color;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.socket-sorcery.scroll_ink")
				.withStyle(ChatFormatting.DARK_GRAY));
	}
}
