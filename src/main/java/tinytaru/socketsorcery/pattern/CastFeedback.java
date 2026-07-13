package tinytaru.socketsorcery.pattern;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A pattern's shared activation feedback: an optional sound at the caster and an optional particle
 * burst at the impact point (or just ahead of the eyes on a miss). Fired once per activated gem,
 * before that gem's effect components run.
 */
public record CastFeedback(Optional<SoundEvent> sound, Optional<ParticleOptions> particle, boolean skipOnMiss) {

	public static final Codec<CastFeedback> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound").forGetter(CastFeedback::sound),
			ParticleTypes.CODEC.optionalFieldOf("particle").forGetter(CastFeedback::particle),
			Codec.BOOL.optionalFieldOf("skip_on_miss", false).forGetter(CastFeedback::skipOnMiss)
	).apply(instance, CastFeedback::new));

	/** Plays the feedback for one activation. Server-side; nearby clients receive both. */
	public void play(ServerPlayer player, HitResult target) {
		if (skipOnMiss && target.getType() == HitResult.Type.MISS) {
			return;
		}
		ServerLevel level = player.serverLevel();
		sound.ifPresent(event -> level.playSound(null, player.getX(), player.getY(), player.getZ(),
				event, SoundSource.PLAYERS, 0.8F, 1.0F));
		particle.ifPresent(options -> {
			Vec3 at = target.getType() == HitResult.Type.MISS
					? player.getEyePosition().add(player.getViewVector(1.0F).scale(2.0))
					: target.getLocation();
			level.sendParticles(options, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.02);
		});
	}
}
