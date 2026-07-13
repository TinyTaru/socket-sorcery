package tinytaru.socketsorcery.pattern.effect;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.pattern.EngraveMods;

/** Shared target resolution for effect components. */
final class EffectTargets {

	/** The living entities a component with the given targeting applies to (possibly empty). */
	static List<LivingEntity> resolve(EffectTarget target, ServerPlayer player, HitResult hit,
			EngraveMods mods, double radius, EffectFilter filter) {
		return switch (target) {
			case SELF -> List.of(player);
			case HIT_ENTITY -> hitLiving(hit) instanceof LivingEntity living ? List.of(living) : List.of();
			case HIT_ENTITY_OR_SELF -> hitLiving(hit) instanceof LivingEntity living ? List.of(living) : List.of(player);
			case AREA -> {
				AABB area = player.getBoundingBox().inflate(mods.radius(radius));
				yield player.level().getEntitiesOfClass(filter.entityClass(), area,
						e -> e.isAlive() && e != player).stream().map(e -> (LivingEntity) e).toList();
			}
		};
	}

	private static LivingEntity hitLiving(HitResult hit) {
		return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living
				? living : null;
	}

	private EffectTargets() {
	}
}
