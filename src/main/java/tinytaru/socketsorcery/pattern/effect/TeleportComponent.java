package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.config.SocketSorceryConfig;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;

/**
 * Teleports the wearer to the activation's hit location — or, on a miss, straight ahead to the
 * configured bangle reach (read live from config, never baked into data). Direction modifiers nudge
 * the destination by {@code aim_scale}.
 */
public record TeleportComponent(double aimScale) implements PatternEffectComponent {

	public static final MapCodec<TeleportComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.DOUBLE.optionalFieldOf("aim_scale", 0.0).forGetter(TeleportComponent::aimScale)
	).apply(instance, TeleportComponent::new));

	@Override
	public MapCodec<TeleportComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		Vec3 destination = target.getType() == HitResult.Type.MISS
				? player.getEyePosition().add(player.getViewVector(1.0F).scale(SocketSorceryConfig.get().bangleReach))
				: target.getLocation();
		if (aimScale > 0.0 && mods.hasAim()) {
			destination = destination.add(mods.worldAim(player).scale(aimScale));
		}
		player.teleportTo(destination.x, destination.y, destination.z);
		player.fallDistance = 0.0F;
	}
}
