package tinytaru.socketsorcery.item;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * Wires the Ring's reactive trigger: when the wearer takes damage, each socketed gem's bangle-style
 * active behaviour fires automatically — against the attacker, if any — on the ring's own cooldown.
 * Server-side only.
 */
public final class RingReactions {

	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (blocked || damageTaken <= 0.0F || !(entity instanceof ServerPlayer player)) {
				return;
			}
			if (player.getCooldowns().isOnCooldown(ModItems.RING)) {
				return;
			}
			TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
				HitResult target = reactTarget(player, source);
				component.forEach((slot, stack) -> {
					if (stack.getItem() instanceof RingItem) {
						AccessoryItem.runBangle(player, stack, target);
						int ticks = Cooldowns.forBangle(stack);
						if (ticks > 0) {
							player.getCooldowns().addCooldown(ModItems.RING, ticks);
						}
					}
				});
			});
		});
	}

	/** Reacts toward whoever dealt the damage, or a "miss" at the player's feet if there's no attacker. */
	private static HitResult reactTarget(ServerPlayer player, DamageSource source) {
		Entity attacker = source.getEntity();
		if (attacker != null) {
			return new EntityHitResult(attacker);
		}
		return BlockHitResult.miss(player.position(), Direction.UP, BlockPos.containing(player.position()));
	}

	private RingReactions() {
	}
}
