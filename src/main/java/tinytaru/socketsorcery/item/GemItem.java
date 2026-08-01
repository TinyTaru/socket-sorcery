package tinytaru.socketsorcery.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import tinytaru.socketsorcery.component.CarvingData;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.pattern.Modifier;
import tinytaru.socketsorcery.pattern.Modifiers;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;

/**
 * The mod's own gem items. Which patterns a gem can hold is declared by the pattern data definitions
 * (each pattern lists its gems) — datapacks may also declare non-{@code GemItem} items as gems, so
 * gameplay checks use the {@code engraving} component / registry lookups, never {@code instanceof}.
 */
public class GemItem extends Item {

	public GemItem(Properties properties) {
		super(properties);
	}

	/** True if the stack carries an engraving (the "can be socketed" check — item class irrelevant). */
	public static boolean isEngravedGem(ItemStack stack) {
		return stack.has(ModComponents.ENGRAVING);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.has(ModComponents.ENGRAVING) || super.isFoil(stack);
	}

	/**
	 * A gem carrying half-finished cuts stops being a plain gem: it is stone with an unreadable mark on
	 * it and grants nothing until the pattern is closed, so it says so rather than passing for blank.
	 */
	@Override
	public Component getName(ItemStack stack) {
		return stack.has(ModComponents.CARVING)
				? Component.translatable("item.socket-sorcery.partially_engraved_gem")
				: super.getName(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		HolderLookup.Provider registries = context.registries(); // may be null; lookups tolerate it
		EngravingData engraving = stack.get(ModComponents.ENGRAVING);
		CarvingData carving = stack.get(ModComponents.CARVING);
		if (carving != null) {
			Holder.Reference<Pattern> pattern = Patterns.get(registries, carving.pattern());
			Component name = pattern != null
					? pattern.value().coloredName(pattern.key().identifier())
					: Component.literal(carving.pattern().toString());
			tooltip.accept(Component.translatable("tooltip.socket-sorcery.carving_unfinished")
					.withStyle(ChatFormatting.GRAY).append(Component.literal(" ")).append(name));
		} else if (engraving != null) {
			Holder.Reference<Pattern> pattern = Patterns.get(registries, engraving.pattern());
			Component name = pattern != null
					? pattern.value().coloredName(pattern.key().identifier())
					: Component.literal(engraving.pattern().toString());
			tooltip.accept(Component.translatable("tooltip.socket-sorcery.engraved_with")
					.withStyle(ChatFormatting.GRAY).append(Component.literal(" ")).append(name));
			for (Identifier modifierId : Modifiers.ordered(engraving.modifiers())) {
				Holder.Reference<Modifier> modifier = Modifiers.get(registries, modifierId);
				if (modifier != null) {
					tooltip.accept(Component.literal(" + ").withStyle(ChatFormatting.DARK_GRAY)
							.append(modifier.value().coloredName(modifier.key().identifier())));
				}
			}
		} else if (registries != null) {
			tooltip.accept(Component.translatable("tooltip.socket-sorcery.supports").withStyle(ChatFormatting.DARK_GRAY));
			for (Identifier id : Patterns.patternsFor(registries, this)) {
				Holder.Reference<Pattern> pattern = Patterns.get(registries, id);
				if (pattern != null) {
					tooltip.accept(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
							.append(pattern.value().coloredName(id)));
				}
			}
		}
	}
}
