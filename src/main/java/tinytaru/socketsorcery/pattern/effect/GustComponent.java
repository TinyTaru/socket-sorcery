package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;

/**
 * Shoves the resolved targets around without hurting them: a blast of air rather than a blow. The
 * push runs outward from the wearer, or along the Direction modifiers' aim when {@code aim_scale} is
 * set. {@code magnitude} respects Power and the {@code area} radius respects Range — which is the
 * point of the component, since a pattern whose only active effect moves the *wearer* has no radius
 * for Range to widen.
 *
 * <p>This is {@link DamageComponent}'s knockback half with the damage removed, so it repeats that
 * component's hand-rolled shove: {@link LivingEntity#knockback} is horizontal-only and would flatten
 * the lift out of a gust, so the velocity is added directly and knockback resistance applied here by
 * hand — an iron golem stays as hard to move as it is under any other shove.
 */
public record GustComponent(EffectTarget target, double radius, EffectFilter filter, double magnitude,
		double yBoost, double aimScale, EffectWhen when) implements PatternEffectComponent {

	public static final MapCodec<GustComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			EffectTarget.CODEC.optionalFieldOf("target", EffectTarget.AREA).forGetter(GustComponent::target),
			Codec.DOUBLE.optionalFieldOf("radius", 3.0).forGetter(GustComponent::radius),
			EffectFilter.CODEC.optionalFieldOf("filter", EffectFilter.MONSTERS).forGetter(GustComponent::filter),
			Codec.DOUBLE.fieldOf("magnitude").forGetter(GustComponent::magnitude),
			Codec.DOUBLE.optionalFieldOf("y_boost", 0.0).forGetter(GustComponent::yBoost),
			Codec.DOUBLE.optionalFieldOf("aim_scale", 0.0).forGetter(GustComponent::aimScale),
			EffectWhen.CODEC.optionalFieldOf("when", EffectWhen.ALWAYS).forGetter(GustComponent::when)
	).apply(instance, GustComponent::new));

	@Override
	public MapCodec<GustComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		if (!when.matches(target)) {
			return;
		}
		boolean aimed = aimScale > 0.0 && mods.hasAim();
		Vec3 aim = aimed ? mods.worldAim(player).scale(aimScale) : Vec3.ZERO;
		double strength = mods.magnitude(magnitude);
		for (LivingEntity entity : EffectTargets.resolve(this.target, player, target, mods, radius, filter)) {
			Vec3 push = aimed ? aim : outward(player, entity);
			push = push.scale(strength).add(0.0, yBoost, 0.0);
			double resisted = 1.0 - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
			if (resisted > 0.0) {
				entity.setDeltaMovement(entity.getDeltaMovement().add(push.scale(resisted)));
				entity.hurtMarked = true; // forces a velocity packet for player targets
			}
		}
	}

	/**
	 * The horizontal unit vector from the wearer to {@code entity}. An entity standing exactly on the
	 * wearer has no outward direction to find, so it gets lifted straight up rather than an arbitrary
	 * compass heading.
	 */
	private static Vec3 outward(ServerPlayer player, Entity entity) {
		Vec3 away = new Vec3(entity.getX() - player.getX(), 0.0, entity.getZ() - player.getZ());
		return away.lengthSqr() < 1.0e-6 ? new Vec3(0.0, 1.0, 0.0) : away.normalize();
	}
}
