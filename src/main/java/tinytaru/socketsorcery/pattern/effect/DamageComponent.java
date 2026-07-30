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
 * Deals magic damage to the resolved targets, optionally knocking them away from the wearer.
 * Amount respects the Power modifier; radius (for {@code area}) respects Range.
 *
 * <p>{@code knockback_aim_scale} lets Direction modifiers steer the shove instead: down slams
 * targets into the floor, up pops them airborne, left/right sweeps them aside. Aimed shoves go
 * through {@link Entity#setDeltaMovement} rather than {@link LivingEntity#knockback}, which is
 * horizontal-only, so knockback resistance is applied here by hand to keep an iron golem as hard to
 * move as it is under a plain shove.
 */
public record DamageComponent(double amount, EffectTarget target, double radius, EffectFilter filter,
		double knockback, double knockbackAimScale, EffectWhen when) implements PatternEffectComponent {

	public static final MapCodec<DamageComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.DOUBLE.fieldOf("amount").forGetter(DamageComponent::amount),
			EffectTarget.CODEC.optionalFieldOf("target", EffectTarget.HIT_ENTITY).forGetter(DamageComponent::target),
			Codec.DOUBLE.optionalFieldOf("radius", 3.0).forGetter(DamageComponent::radius),
			EffectFilter.CODEC.optionalFieldOf("filter", EffectFilter.MONSTERS).forGetter(DamageComponent::filter),
			Codec.DOUBLE.optionalFieldOf("knockback", 0.0).forGetter(DamageComponent::knockback),
			Codec.DOUBLE.optionalFieldOf("knockback_aim_scale", 0.0).forGetter(DamageComponent::knockbackAimScale),
			EffectWhen.CODEC.optionalFieldOf("when", EffectWhen.ALWAYS).forGetter(DamageComponent::when)
	).apply(instance, DamageComponent::new));

	@Override
	public MapCodec<DamageComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		if (!when.matches(target)) {
			return;
		}
		boolean aimed = knockbackAimScale > 0.0 && mods.hasAim();
		Vec3 push = aimed ? mods.worldAim(player).scale(knockback * knockbackAimScale) : Vec3.ZERO;
		for (LivingEntity entity : EffectTargets.resolve(this.target, player, target, mods, radius, filter)) {
			entity.hurt(player.level().damageSources().magic(), (float) mods.magnitude(amount));
			if (knockback <= 0.0) {
				continue;
			}
			if (aimed && push.lengthSqr() > 1.0e-6) {
				double resisted = 1.0 - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
				if (resisted > 0.0) {
					entity.setDeltaMovement(entity.getDeltaMovement().add(push.scale(resisted)));
					entity.hurtMarked = true; // forces a velocity packet for player targets
				}
			} else {
				entity.knockback(knockback, player.getX() - entity.getX(), player.getZ() - entity.getZ());
			}
		}
	}
}
