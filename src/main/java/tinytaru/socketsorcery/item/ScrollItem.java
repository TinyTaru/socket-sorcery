package tinytaru.socketsorcery.item;

import java.util.function.Consumer;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.PatternTooltip;
import tinytaru.socketsorcery.pattern.Patterns;

/**
 * A scroll teaching a single pattern. Which pattern is declared by the pattern's own data definition
 * (its {@code scroll} field) — the reverse lookup happens against the synced pattern registry. Used
 * as the template at the Engraving Table; consumed when an engraving completes. Found in loot.
 */
public class ScrollItem extends Item {

	public ScrollItem(Properties properties) {
		super(properties);
	}

	/** The pattern id this scroll teaches, or null when no pattern claims it / registries unavailable. */
	public Identifier patternId(HolderLookup.Provider registries) {
		Holder.Reference<Pattern> pattern = Patterns.forScroll(registries, this);
		return pattern == null ? null : pattern.key().identifier();
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true; // scrolls always shimmer with latent magic
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		Holder.Reference<Pattern> pattern = Patterns.forScroll(context.registries(), this);
		if (pattern != null) {
			tooltip.accept(Component.translatable("tooltip.socket-sorcery.scroll_pattern")
					.withStyle(ChatFormatting.GRAY).append(Component.literal(" "))
					.append(pattern.value().coloredName(pattern.key().identifier())));
		}
		tooltip.accept(Component.translatable("tooltip.socket-sorcery.scroll_hint").withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		// No registry access here — the client-side renderer resolves the scroll's pattern itself.
		return Optional.of(new PatternTooltip(this));
	}
}
