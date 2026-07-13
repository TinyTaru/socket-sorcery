package tinytaru.socketsorcery.pattern.effect;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;

/**
 * Builds up (or sets) powder-snow freezing ticks on the resolved targets. Exactly one of
 * {@code add_ticks} (incremental, capped by {@code max_ticks}) or {@code set_ticks} (absolute,
 * scaled by the Duration modifier) must be present.
 */
public record FreezeComponent(EffectTarget target, double radius, EffectFilter filter,
		Optional<Integer> addTicks, Optional<Integer> setTicks, int maxTicks) implements PatternEffectComponent {

	public static final MapCodec<FreezeComponent> CODEC = RecordCodecBuilder.<FreezeComponent>mapCodec(instance -> instance.group(
			EffectTarget.CODEC.optionalFieldOf("target", EffectTarget.HIT_ENTITY).forGetter(FreezeComponent::target),
			Codec.DOUBLE.optionalFieldOf("radius", 5.0).forGetter(FreezeComponent::radius),
			EffectFilter.CODEC.optionalFieldOf("filter", EffectFilter.MONSTERS).forGetter(FreezeComponent::filter),
			Codec.INT.optionalFieldOf("add_ticks").forGetter(FreezeComponent::addTicks),
			Codec.INT.optionalFieldOf("set_ticks").forGetter(FreezeComponent::setTicks),
			Codec.INT.optionalFieldOf("max_ticks", 140).forGetter(FreezeComponent::maxTicks)
	).apply(instance, FreezeComponent::new)).validate(component ->
			component.addTicks().isPresent() == component.setTicks().isPresent()
					? DataResult.error(() -> "freeze requires exactly one of add_ticks or set_ticks")
					: DataResult.success(component));

	@Override
	public MapCodec<FreezeComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		for (LivingEntity entity : EffectTargets.resolve(this.target, player, target, mods, radius, filter)) {
			if (setTicks.isPresent()) {
				entity.setTicksFrozen(mods.duration(setTicks.get()));
			} else if (addTicks.isPresent()) {
				entity.setTicksFrozen(Math.min(entity.getTicksFrozen() + addTicks.get(), maxTicks));
			}
		}
	}
}
