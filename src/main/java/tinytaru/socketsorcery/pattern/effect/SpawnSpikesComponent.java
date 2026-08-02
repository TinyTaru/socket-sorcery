package tinytaru.socketsorcery.pattern.effect;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.entity.SpikeEntity;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;
import tinytaru.socketsorcery.util.ColorCodecs;

/**
 * Raises a set of {@link SpikeEntity spikes} along a shape, giving an otherwise invisible radius or line
 * a body the player can actually see. {@code radius} respects Range, {@code scale} respects Power, and a
 * {@code line} shape follows the Direction modifiers — so the spikes trace the cast rather than
 * decorating it.
 *
 * <p>The spikes carry no damage of their own; the pattern's {@code damage} component still owns that.
 * See {@link SpikeEntity} for why that separation is load-bearing.
 *
 * <p>{@code stagger_ticks} delays each spike by its index, which is what turns a set of positions into a
 * gesture: a ring erupts around the caster, a line marches away from them.
 */
public record SpawnSpikesComponent(EffectShape shape, double radius, int points, int color, float scale,
		int lifeTicks, int staggerTicks, int snapDown, EffectWhen when) implements PatternEffectComponent {

	/**
	 * Hard ceiling on spikes per cast, whatever the data asks for. Range multiplies the point count, and
	 * a datapack is free to write a silly one — neither should be able to dump hundreds of entities into
	 * the world on a keypress.
	 */
	private static final int MAX_SPIKES = 32;

	public static final MapCodec<SpawnSpikesComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			EffectShape.CODEC.optionalFieldOf("shape", EffectShape.RING).forGetter(SpawnSpikesComponent::shape),
			Codec.doubleRange(0.0, 32.0).optionalFieldOf("radius", 3.0).forGetter(SpawnSpikesComponent::radius),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("points", 8).forGetter(SpawnSpikesComponent::points),
			ColorCodecs.RGB.optionalFieldOf("color", 0x8B99A3).forGetter(SpawnSpikesComponent::color),
			Codec.floatRange(0.25F, 3.0F).optionalFieldOf("scale", 1.0F).forGetter(SpawnSpikesComponent::scale),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("life_ticks", SpikeEntity.DEFAULT_LIFE_TICKS)
					.forGetter(SpawnSpikesComponent::lifeTicks),
			ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("stagger_ticks", 2).forGetter(SpawnSpikesComponent::staggerTicks),
			ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("snap_down", 3).forGetter(SpawnSpikesComponent::snapDown),
			EffectWhen.CODEC.optionalFieldOf("when", EffectWhen.ALWAYS).forGetter(SpawnSpikesComponent::when)
	).apply(instance, SpawnSpikesComponent::new));

	@Override
	public MapCodec<SpawnSpikesComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		if (!when.matches(target)) {
			return;
		}
		Level level = player.level();
		List<Vec3> positions = EffectShapes.snapToGround(level,
				EffectShapes.resolve(shape, player, target, mods, radius, points), snapDown, true);
		float size = (float) mods.magnitude(scale);
		int spawned = 0;
		for (Vec3 position : positions) {
			if (spawned >= MAX_SPIKES) {
				break;
			}
			level.addFreshEntity(new SpikeEntity(level, position.x, position.y, position.z,
					facingAway(player, position), spawned * staggerTicks, lifeTicks, color, size));
			spawned++;
		}
	}

	/** Each spike leans away from the caster, so a ring reads as bursting outward rather than inward. */
	private static float facingAway(ServerPlayer player, Vec3 position) {
		double dx = position.x - player.getX();
		double dz = position.z - player.getZ();
		if (dx * dx + dz * dz < 1.0e-6) {
			return player.getYRot();
		}
		return (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG);
	}
}
