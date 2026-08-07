package tinytaru.socketsorcery.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A scroll prepared for hand transcription. Its painted cells live in {@code scroll_drawing}. */
public class BlankScrollItem extends Item {
	public BlankScrollItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.socket-sorcery.blank_scroll").withStyle(ChatFormatting.DARK_GRAY));
	}
}
