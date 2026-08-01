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
 * Heals the resolved targets. Amount respects the Power modifier; {@code radius} — the aura width
 * for {@code area} targeting, the line length for {@code aimed_line} — respects Range.
 */
public record HealComponent(double amount, EffectTarget target, double radius,
		EffectWhen when) implements PatternEffectComponent {

	public static final MapCodec<HealComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.DOUBLE.fieldOf("amount").forGetter(HealComponent::amount),
			EffectTarget.CODEC.optionalFieldOf("target", EffectTarget.HIT_ENTITY_OR_SELF).forGetter(HealComponent::target),
			Codec.DOUBLE.optionalFieldOf("radius", 0.0).forGetter(HealComponent::radius),
			EffectWhen.CODEC.optionalFieldOf("when", EffectWhen.ALWAYS).forGetter(HealComponent::when)
	).apply(instance, HealComponent::new));

	@Override
	public MapCodec<HealComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		if (!when.matches(target)) {
			return;
		}
		for (LivingEntity entity : EffectTargets.resolve(this.target, player, target, mods, radius, EffectFilter.LIVING)) {
			entity.heal((float) mods.magnitude(amount));
		}
	}
}
