package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;

/**
 * Deals magic damage to the resolved targets, optionally knocking them away from the wearer.
 * Amount respects the Power modifier; radius (for {@code area}) respects Range.
 */
public record DamageComponent(double amount, EffectTarget target, double radius, EffectFilter filter,
		double knockback, EffectWhen when) implements PatternEffectComponent {

	public static final MapCodec<DamageComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.DOUBLE.fieldOf("amount").forGetter(DamageComponent::amount),
			EffectTarget.CODEC.optionalFieldOf("target", EffectTarget.HIT_ENTITY).forGetter(DamageComponent::target),
			Codec.DOUBLE.optionalFieldOf("radius", 3.0).forGetter(DamageComponent::radius),
			EffectFilter.CODEC.optionalFieldOf("filter", EffectFilter.MONSTERS).forGetter(DamageComponent::filter),
			Codec.DOUBLE.optionalFieldOf("knockback", 0.0).forGetter(DamageComponent::knockback),
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
		for (LivingEntity entity : EffectTargets.resolve(this.target, player, target, mods, radius, filter)) {
			entity.hurt(player.level().damageSources().magic(), (float) mods.magnitude(amount));
			if (knockback > 0.0) {
				entity.knockback(knockback, player.getX() - entity.getX(), player.getZ() - entity.getZ());
			}
		}
	}
}
