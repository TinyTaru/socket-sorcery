package tinytaru.socketsorcery.pattern.effect;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;

/**
 * Traces a shape in particles. This is what makes an aimed line or an area radius readable: the geometry
 * is resolved through {@link EffectShapes}, the same source the mechanics use, so the drawing cannot
 * claim a reach the cast doesn't have.
 *
 * <p>Any vanilla particle works — the {@code particle} field takes the same JSON the pattern's
 * {@code cast_feedback} already accepts.
 */
public record ShapedParticlesComponent(ParticleOptions particle, EffectShape shape, double radius, int points,
		int count, double spread, double speed, double yOffset, boolean snapToGround,
		EffectWhen when) implements PatternEffectComponent {

	public static final MapCodec<ShapedParticlesComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ParticleTypes.CODEC.fieldOf("particle").forGetter(ShapedParticlesComponent::particle),
			EffectShape.CODEC.optionalFieldOf("shape", EffectShape.POINT).forGetter(ShapedParticlesComponent::shape),
			Codec.doubleRange(0.0, 32.0).optionalFieldOf("radius", 3.0).forGetter(ShapedParticlesComponent::radius),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("points", 16).forGetter(ShapedParticlesComponent::points),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(ShapedParticlesComponent::count),
			Codec.doubleRange(0.0, 4.0).optionalFieldOf("spread", 0.05).forGetter(ShapedParticlesComponent::spread),
			Codec.doubleRange(0.0, 4.0).optionalFieldOf("speed", 0.0).forGetter(ShapedParticlesComponent::speed),
			Codec.DOUBLE.optionalFieldOf("y_offset", 0.1).forGetter(ShapedParticlesComponent::yOffset),
			Codec.BOOL.optionalFieldOf("snap_to_ground", false).forGetter(ShapedParticlesComponent::snapToGround),
			EffectWhen.CODEC.optionalFieldOf("when", EffectWhen.ALWAYS).forGetter(ShapedParticlesComponent::when)
	).apply(instance, ShapedParticlesComponent::new));

	@Override
	public MapCodec<ShapedParticlesComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		if (!when.matches(target)) {
			return;
		}
		ServerLevel level = player.level();
		List<Vec3> positions = EffectShapes.resolve(shape, player, target, mods, radius, points);
		if (snapToGround) {
			// Kept even where no surface was found: a particle hanging in the air is fine, unlike a spike.
			positions = EffectShapes.snapToGround(level, positions, 3, false);
		}
		for (Vec3 position : positions) {
			level.sendParticles(particle, position.x, position.y + yOffset, position.z,
					count, spread, spread, spread, speed);
		}
	}
}
