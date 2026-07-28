package tinytaru.socketsorcery.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Ground from its matching gem (one gem yields nine dust). Spent at the Engraving Table each time a
 * chiselled cut is eased back (right-click) on that same gem type (see
 * {@link tinytaru.socketsorcery.registry.ModItems#dustFor}).
 */
public class GemDustItem extends Item {

	public GemDustItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.socket-sorcery.gem_dust").withStyle(ChatFormatting.DARK_GRAY));
	}
}
