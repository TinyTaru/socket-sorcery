package tinytaru.socketsorcery.item;

import java.util.List;

import dev.emi.trinkets.api.SlotReference;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Necklace — holds up to 5 gems and runs their <em>passive</em> behaviour while worn.
 */
public class NecklaceItem extends AccessoryItem {

	public static final int CAPACITY = 5;

	/** Run passive effects every this many ticks (buffs are applied with a longer duration so they persist). */
	private static final int TICK_INTERVAL = 20;

	public NecklaceItem(Properties properties) {
		super(properties, CAPACITY);
	}

	@Override
	public void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
		if (entity.level().isClientSide || !(entity instanceof ServerPlayer player)) {
			return;
		}
		if (entity.tickCount % TICK_INTERVAL == 0) {
			runNecklace(player, stack);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("tooltip.socket-sorcery.necklace_hint").withStyle(ChatFormatting.DARK_GRAY));
	}
}
