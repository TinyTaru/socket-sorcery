package tinytaru.socketsorcery.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.pattern.Modifier;
import tinytaru.socketsorcery.pattern.Modifiers;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;

/**
 * A gem. Each gem instance supports a fixed set of patterns (see {@link Patterns}). An engraved gem
 * is the same item carrying an {@code engraving} data component; it is what gets socketed.
 */
public class GemItem extends Item {

	public GemItem(Properties properties) {
		super(properties);
	}

	/** True if the stack is a gem that has been engraved with a pattern. */
	public static boolean isEngravedGem(ItemStack stack) {
		return stack.getItem() instanceof GemItem && stack.has(ModComponents.ENGRAVING);
	}

	/** The pattern engraved on this gem stack, or null if unengraved / unknown. */
	public static Pattern engravedPattern(ItemStack stack) {
		EngravingData data = stack.get(ModComponents.ENGRAVING);
		return data == null ? null : Patterns.get(data.pattern());
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.has(ModComponents.ENGRAVING) || super.isFoil(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		EngravingData engraving = stack.get(ModComponents.ENGRAVING);
		if (engraving != null) {
			Pattern pattern = Patterns.get(engraving.pattern());
			Component name = pattern != null ? pattern.coloredName() : Component.literal(engraving.pattern().toString());
			tooltip.add(Component.translatable("tooltip.socket-sorcery.engraved_with")
					.withStyle(ChatFormatting.GRAY).append(Component.literal(" ")).append(name));
			for (ResourceLocation modifierId : Modifiers.ordered(engraving.modifiers())) {
				Modifier modifier = Modifiers.get(modifierId);
				if (modifier != null) {
					tooltip.add(Component.literal(" + ").withStyle(ChatFormatting.DARK_GRAY).append(modifier.coloredName()));
				}
			}
		} else {
			tooltip.add(Component.translatable("tooltip.socket-sorcery.supports").withStyle(ChatFormatting.DARK_GRAY));
			for (ResourceLocation id : Patterns.patternsFor(this)) {
				Pattern pattern = Patterns.get(id);
				if (pattern != null) {
					tooltip.add(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY).append(pattern.coloredName()));
				}
			}
		}
	}
}
