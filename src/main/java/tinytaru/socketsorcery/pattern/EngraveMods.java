package tinytaru.socketsorcery.pattern;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The combined effect adjustments from an engraving's modifier set, passed to {@link PatternEffect}s.
 * Effects opt into whichever knobs are relevant ({@code amp}/{@code duration}/{@code magnitude}/
 * {@code radius}/{@code aim}); the rest are no-ops.
 *
 * <p>{@code aim} is a bias in the player's local frame (x = right, y = up, z = forward); resolve it
 * to world space with {@link #worldAim(LivingEntity)}.
 */
public record EngraveMods(int power, int durationMult, double rangeBonus, Vec3 aim) {

	public static final EngraveMods NONE = new EngraveMods(0, 1, 0.0, Vec3.ZERO);

	public int amp(int base) {
		return base + power;
	}

	public int duration(int base) {
		return base * durationMult;
	}

	public double magnitude(double base) {
		return power > 0 ? base * 1.5 : base;
	}

	public double radius(double base) {
		return base + rangeBonus;
	}

	public boolean hasAim() {
		return aim.lengthSqr() > 1.0e-6;
	}

	/** Resolves the local-frame aim to a world-space unit vector for {@code entity}. */
	public Vec3 worldAim(LivingEntity entity) {
		Vec3 forward = entity.getLookAngle();
		Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
		right = right.lengthSqr() < 1.0e-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
		Vec3 world = right.scale(aim.x).add(0.0, aim.y, 0.0).add(forward.scale(aim.z));
		return world.lengthSqr() < 1.0e-6 ? Vec3.ZERO : world.normalize();
	}
}
