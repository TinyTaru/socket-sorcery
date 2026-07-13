package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.util.ExtraCodecs;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;

/**
 * Applies a status effect to the resolved targets. Duration and amplifier respect the engraving's
 * Duration / Power modifiers; radius (for {@code area} targeting) respects Range.
 */
public record MobEffectComponent(Holder<MobEffect> effect, int duration, int amplifier, EffectTarget target,
		double radius, EffectFilter filter, boolean ambient, boolean showParticles, EffectWhen when)
		implements PatternEffectComponent {

	public static final MapCodec<MobEffectComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(MobEffectComponent::effect),
			ExtraCodecs.POSITIVE_INT.fieldOf("duration").forGetter(MobEffectComponent::duration),
			ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("amplifier", 0).forGetter(MobEffectComponent::amplifier),
			EffectTarget.CODEC.optionalFieldOf("target", EffectTarget.SELF).forGetter(MobEffectComponent::target),
			com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("radius", 5.0).forGetter(MobEffectComponent::radius),
			EffectFilter.CODEC.optionalFieldOf("filter", EffectFilter.MONSTERS).forGetter(MobEffectComponent::filter),
			com.mojang.serialization.Codec.BOOL.optionalFieldOf("ambient", false).forGetter(MobEffectComponent::ambient),
			com.mojang.serialization.Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(MobEffectComponent::showParticles),
			EffectWhen.CODEC.optionalFieldOf("when", EffectWhen.ALWAYS).forGetter(MobEffectComponent::when)
	).apply(instance, MobEffectComponent::new));

	@Override
	public MapCodec<MobEffectComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		if (!when.matches(target)) {
			return;
		}
		for (LivingEntity entity : EffectTargets.resolve(this.target, player, target, mods, radius, filter)) {
			entity.addEffect(new MobEffectInstance(effect, mods.duration(duration), mods.amp(amplifier),
					ambient, showParticles, true));
		}
	}
}
