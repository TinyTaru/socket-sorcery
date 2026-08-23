package tinytaru.socketsorcery.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.pattern.RingTrigger;
import tinytaru.socketsorcery.registry.ModItems;

/** Server-side trigger routing for engraved rings. */
public final class RingReactions {
	private static final int RADIUS_CHECK_INTERVAL = 5;
	private static final Map<UUID, FallState> FALLING = new HashMap<>();
	private static final Map<UUID, Boolean> LOW_HEALTH = new HashMap<>();
	private static int radiusTick;

	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, damage, blocked) -> {
			if (!blocked && damage > 0.0F && entity instanceof ServerPlayer player) {
				trigger(player, RingTrigger.ON_HIT, reactTarget(player, source));
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			Entity killer = entity.getKillCredit();
			if (killer instanceof ServerPlayer player) {
				trigger(player, RingTrigger.ON_KILL, new EntityHitResult(entity));
			}
		});
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				trigger(serverPlayer, RingTrigger.ON_ATTACK, new EntityHitResult(entity));
			}
			return InteractionResult.PASS;
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			FALLING.remove(id);
			LOW_HEALTH.remove(id);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			boolean checkRadius = ++radiusTick % RADIUS_CHECK_INTERVAL == 0;
			server.getAllLevels().forEach(level -> {
				for (ServerPlayer player : level.players()) {
					checkFall(player);
					checkHealth(player);
					if (checkRadius && hasReadyTrigger(player, RingTrigger.IN_RADIUS)
							&& player.level().getEntitiesOfClass(LivingEntity.class,
							player.getBoundingBox().inflate(4.0), e -> e.isAlive() && e instanceof Enemy)
							.stream().findAny().isPresent()) {
						trigger(player, RingTrigger.IN_RADIUS, miss(player));
					}
				}
			});
		});
	}

	private static boolean hasReadyTrigger(ServerPlayer player, RingTrigger wanted) {
		for (TrinketSlotAccess slot : TrinketsApi.getAttachment(player)
				.equipped(stack -> stack.getItem() instanceof RingItem, false)) {
			ItemStack ring = slot.get();
			if (ringTrigger(player, ring) == wanted && !player.getCooldowns().isOnCooldown(ring)
					&& !AccessoryItem.getSockets(ring).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private static void checkFall(ServerPlayer player) {
		FallState state = FALLING.computeIfAbsent(player.getUUID(), id -> new FallState());
		if (!player.onGround()) {
			state.airborne = true;
			state.maxDistance = Math.max(state.maxDistance, Math.max(player.fallDistance, 0.0F));
		} else if (state.airborne) {
			if (state.maxDistance >= 4.0F) {
				trigger(player, RingTrigger.ON_FALLING, miss(player));
			}
			state.airborne = false;
			state.maxDistance = 0.0F;
		}
	}

	private static void checkHealth(ServerPlayer player) {
		boolean below = player.getHealth() < 7.0F;
		boolean wasBelow = LOW_HEALTH.getOrDefault(player.getUUID(), false);
		if (below && !wasBelow) {
			trigger(player, RingTrigger.HP_THRESHOLD, miss(player));
		}
		LOW_HEALTH.put(player.getUUID(), below);
	}

	private static void trigger(ServerPlayer player, RingTrigger wanted, HitResult target) {
		for (TrinketSlotAccess slot : TrinketsApi.getAttachment(player)
				.equipped(stack -> stack.getItem() instanceof RingItem, false)) {
			ItemStack ring = slot.get();
			RingTrigger actual = ringTrigger(player, ring);
			if (actual != wanted || player.getCooldowns().isOnCooldown(ring)) {
				continue;
			}
			AccessoryItem.runBangle(player, ring, target);
			int cooldown = Cooldowns.forBangle(ring, player.registryAccess());
			if (cooldown > 0) {
				player.getCooldowns().addCooldown(ring, cooldown);
			}
		}
	}

	private static RingTrigger ringTrigger(ServerPlayer player, ItemStack ring) {
		EngravingData data = ring.get(tinytaru.socketsorcery.registry.ModComponents.ENGRAVING);
		if (data == null) return null;
		var holder = Patterns.get(player.registryAccess(), data.pattern());
		return holder == null ? null : holder.value().ringTrigger().orElse(null);
	}

	private static HitResult reactTarget(ServerPlayer player, DamageSource source) {
		Entity attacker = source.getEntity();
		return attacker == null ? miss(player) : new EntityHitResult(attacker);
	}

	private static HitResult miss(ServerPlayer player) {
		return BlockHitResult.miss(player.position(), Direction.UP, BlockPos.containing(player.position()));
	}

	private static final class FallState {
		boolean airborne;
		double maxDistance;
	}

	private RingReactions() {}
}
