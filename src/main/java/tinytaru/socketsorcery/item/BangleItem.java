package tinytaru.socketsorcery.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Bangle — holds up to 3 gems and runs their <em>active</em> behaviour when the player presses the
 * Activate Bangle keybind (handled server-side via {@link tinytaru.socketsorcery.net.ModNetworking}).
 */
public class BangleItem extends AccessoryItem {

	public static final int CAPACITY = 3;

	public BangleItem(Properties properties) {
		super(properties, CAPACITY);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		if (context.registries() != null) {
			int cooldown = Cooldowns.forBangle(stack, context.registries());
			if (cooldown > 0) {
				String seconds = String.format("%.1f", cooldown / 20.0);
				tooltip.accept(Component.translatable("tooltip.socket-sorcery.cooldown", seconds).withStyle(ChatFormatting.GRAY));
			}
		}
		tooltip.accept(Component.translatable("tooltip.socket-sorcery.bangle_hint").withStyle(ChatFormatting.DARK_GRAY));
	}
}
