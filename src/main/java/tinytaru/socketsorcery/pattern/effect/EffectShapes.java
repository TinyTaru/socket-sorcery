package tinytaru.socketsorcery.pattern.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import tinytaru.socketsorcery.pattern.EngraveMods;

/**
 * Shared position resolution for the visual components, as {@link EffectTargets} is for entities. Where
 * that class answers "who does this hit", this one answers "where should the player see it happen", using
 * the same knobs so the picture can't drift from the mechanics.
 */
final class EffectShapes {

	/** Ceiling on how many positions any shape may produce, whatever the data or Range asks for. */
	static final int MAX_POINTS = 64;

	/** The positions a shape covers (possibly empty). */
	static List<Vec3> resolve(EffectShape shape, ServerPlayer player, HitResult hit, EngraveMods mods,
			double radius, int points) {
		return switch (shape) {
			case POINT -> List.of(origin(player, hit));
			case SELF -> List.of(player.position());
			case RING -> circle(player, mods.radius(radius), scaled(points, radius, mods.radius(radius)));
			case LINE -> EffectTargets.aimedLineCells(player, hit, mods, radius).stream()
					.map(Vec3::atBottomCenterOf).toList();
			case BURST -> sphere(player, origin(player, hit), mods.radius(radius), Math.min(points, MAX_POINTS));
			case ALLY_AURA -> mods.rangeBonus() <= 0.0
					? List.of()
					: circle(player, mods.rangeBonus(), scaled(points, radius, mods.rangeBonus()));
		};
	}

	/**
	 * Where a {@code point} visual goes: the hit location, or two blocks ahead of the eyes on a miss.
	 * Kept identical to {@code CastFeedback}'s rule so the two land together.
	 */
	static Vec3 origin(ServerPlayer player, HitResult hit) {
		return hit.getType() == HitResult.Type.MISS
				? player.getEyePosition().add(player.getViewVector(1.0F).scale(2.0))
				: hit.getLocation();
	}

	/**
	 * Keeps a ring's spacing constant as Range widens it — a fixed count stretched over a bigger circle
	 * reads as a handful of scattered marks rather than a boundary.
	 */
	private static int scaled(int points, double baseRadius, double actualRadius) {
		if (baseRadius <= 0.0) {
			return Mth.clamp(points, 3, MAX_POINTS);
		}
		return Mth.clamp((int) Math.round(points * actualRadius / baseRadius), 3, MAX_POINTS);
	}

	/** Points evenly around the wearer, phased by their facing so a ring is never axis-locked. */
	private static List<Vec3> circle(ServerPlayer player, double radius, int points) {
		if (radius <= 0.0) {
			return List.of();
		}
		Vec3 centre = player.position();
		double phase = player.getYRot() * Mth.DEG_TO_RAD;
		List<Vec3> positions = new ArrayList<>(points);
		for (int i = 0; i < points; i++) {
			double angle = phase + (Math.PI * 2.0 * i) / points;
			positions.add(new Vec3(centre.x + Math.cos(angle) * radius, centre.y,
					centre.z + Math.sin(angle) * radius));
		}
		return positions;
	}

	/** Points scattered through a sphere; uses the level's own RNG so bursts differ cast to cast. */
	private static List<Vec3> sphere(ServerPlayer player, Vec3 centre, double radius, int points) {
		List<Vec3> positions = new ArrayList<>(points);
		for (int i = 0; i < points; i++) {
			positions.add(centre.add(
					(player.getRandom().nextDouble() * 2.0 - 1.0) * radius,
					(player.getRandom().nextDouble() * 2.0 - 1.0) * radius,
					(player.getRandom().nextDouble() * 2.0 - 1.0) * radius));
		}
		return positions;
	}

	/**
	 * Drops each position onto the surface beneath it. {@code requireGround} decides what happens when
	 * there is no surface within reach: spikes must vanish rather than hang in the air, while particles
	 * are happy to stay where they were put.
	 */
	static List<Vec3> snapToGround(Level level, List<Vec3> positions, int searchDown, boolean requireGround) {
		List<Vec3> snapped = new ArrayList<>(positions.size());
		for (Vec3 position : positions) {
			OptionalDouble ground = groundY(level, position.x, position.z, position.y, searchDown);
			if (ground.isPresent()) {
				snapped.add(new Vec3(position.x, ground.getAsDouble(), position.z));
			} else if (!requireGround) {
				snapped.add(position);
			}
		}
		return snapped;
	}

	/**
	 * The surface height under a column, hunting down at most {@code searchDown} blocks for a sturdy top
	 * face — vanilla's evoker-fang placement, which walks down looking for somewhere a spike could
	 * plausibly have come from and gives up rather than guessing.
	 *
	 * <p>The landing cell's own collision height is added on, so a spike stands on top of a slab or a
	 * snow layer instead of half-buried in it.
	 */
	static OptionalDouble groundY(Level level, double x, double z, double startY, int searchDown) {
		BlockPos pos = BlockPos.containing(x, startY, z);
		int floor = Mth.floor(startY) - searchDown;
		while (pos.getY() >= floor) {
			BlockPos below = pos.below();
			if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
				double top = 0.0;
				BlockState standing = level.getBlockState(pos);
				if (!standing.isAir()) {
					VoxelShape shape = standing.getCollisionShape(level, pos);
					if (!shape.isEmpty()) {
						top = shape.max(Direction.Axis.Y);
					}
				}
				return OptionalDouble.of(pos.getY() + top);
			}
			pos = below;
		}
		return OptionalDouble.empty();
	}

	private EffectShapes() {
	}
}
