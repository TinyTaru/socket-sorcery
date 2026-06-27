package tinytaru.socketsorcery.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The tool used to carve patterns onto gems at the Engraving Table. Has durability that is spent
 * when an engraving is completed (proportional to the pattern's size).
 */
public class ChiselItem extends Item {

	public ChiselItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.socket-sorcery.chisel").withStyle(ChatFormatting.DARK_GRAY));
	}
}
