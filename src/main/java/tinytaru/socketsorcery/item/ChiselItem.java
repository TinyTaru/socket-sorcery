package tinytaru.socketsorcery.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The tool used to carve patterns onto gems at the Engraving Table. Has durability that is spent
 * when an engraving is completed (proportional to the pattern's size). Higher tiers can also shave a
 * flat amount off that durability cost per engraving (see {@link #carveCostReduction()}).
 */
public class ChiselItem extends Item {

	private final int carveCostReduction;

	public ChiselItem(Properties properties) {
		this(properties, 0);
	}

	public ChiselItem(Properties properties, int carveCostReduction) {
		super(properties);
		this.carveCostReduction = carveCostReduction;
	}

	/** Flat durability discount applied to every engraving completed with this chisel, floored at 1. */
	public int carveCostReduction() {
		return carveCostReduction;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.socket-sorcery.chisel").withStyle(ChatFormatting.DARK_GRAY));
	}
}
