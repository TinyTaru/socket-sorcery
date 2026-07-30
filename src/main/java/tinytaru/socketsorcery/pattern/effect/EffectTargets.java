package tinytaru.socketsorcery.pattern.effect;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
			case AIMED_LINE -> {
				Set<LivingEntity> found = new LinkedHashSet<>(); // one entity can straddle two cells
				for (BlockPos pos : aimedLineCells(player, hit, mods, radius)) {
					found.addAll(player.level().getEntitiesOfClass(filter.entityClass(), new AABB(pos),
							e -> e.isAlive() && e != player));
				}
				yield List.copyOf(found);
			}
		};
	}

	/**
	 * The block cells an aimed line passes through: one per block stepped outward from the hit
	 * location along the Direction modifiers' aim, {@code length} blocks long (scaled by Range).
	 * Empty without an aim, so an engraving carrying no Direction modifier casts no line at all, and
	 * opposing Directions cancel to nothing rather than to an arbitrary axis.
	 *
	 * <p>This is the single source of that geometry: {@link EffectTarget#AIMED_LINE} resolves
	 * entities from these cells, and positional effects such as {@link FlameLineComponent} place
	 * blocks along the very same ones.
	 */
	static List<BlockPos> aimedLineCells(ServerPlayer player, HitResult hit, EngraveMods mods, double length) {
		if (!mods.hasAim()) {
			return List.of();
		}
		Vec3 aim = mods.worldAim(player);
		if (aim.lengthSqr() < 1.0e-6) {
			return List.of();
		}
		Vec3 origin = origin(hit);
		int steps = Math.max(1, Mth.floor(mods.radius(length)));
		List<BlockPos> cells = new ArrayList<>(steps);
		BlockPos last = null;
		for (int step = 1; step <= steps; step++) {
			BlockPos pos = BlockPos.containing(origin.add(aim.scale(step)));
			if (!pos.equals(last)) { // a shallow aim can round two steps into the same cell
				cells.add(pos);
				last = pos;
			}
		}
		return cells;
	}

	/** Where an aimed line starts: the air cell in front of a struck face, else the hit point. */
	private static Vec3 origin(HitResult hit) {
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
			return Vec3.atCenterOf(blockHit.getBlockPos().relative(blockHit.getDirection()));
		}
		return hit.getLocation();
	}

	private static LivingEntity hitLiving(HitResult hit) {
		return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living
				? living : null;
	}

	private EffectTargets() {
	}
}
