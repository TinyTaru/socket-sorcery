package tinytaru.socketsorcery.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;

/**
 * A scroll teaching a single pattern. Used as the template at the Engraving Table; consumed when an
 * engraving completes. Found in loot.
 */
public class ScrollItem extends Item {

	public ScrollItem(Properties properties) {
		super(properties);
	}

	/** The pattern id this scroll teaches, or null. */
	public ResourceLocation patternId() {
		return Patterns.patternForScroll(this);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true; // scrolls always shimmer with latent magic
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		Pattern pattern = Patterns.get(patternId());
		if (pattern != null) {
			tooltip.add(Component.translatable("tooltip.socket-sorcery.scroll_pattern")
					.withStyle(ChatFormatting.GRAY).append(Component.literal(" ")).append(pattern.coloredName()));
		}
		tooltip.add(Component.translatable("tooltip.socket-sorcery.scroll_hint").withStyle(ChatFormatting.DARK_GRAY));
	}
}
