package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.config.SocketSorceryConfig;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;
import tinytaru.socketsorcery.pattern.Patterns;

/**
 * Teleports the wearer to the activation's hit location — or, on a miss, straight ahead to the
 * configured bangle reach (read live from config, never baked into data). Direction modifiers nudge
 * the destination by {@code aim_scale}.
 *
 * <p>The blind-jump reach respects Range and Power, so both knobs mean "blink farther" — the only
 * distance a teleport has. A hit-location jump is unaffected: the destination is already chosen by
 * what the ray struck, and the bangle's own reach bounds that.
 *
 * <p>An extended blind jump re-casts the ray, because the activation only proved the first
 * {@code bangleReach} blocks clear — the stretch a modifier adds beyond that has never been tested
 * for terrain. Anything the longer ray finds stops the jump at the air cell in front of it rather
 * than inside it, so a modified Blink can't bury the wearer in a wall a plain one would have
 * fallen short of.
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
				? blindJump(player, mods)
				: target.getLocation();
		if (aimScale > 0.0 && mods.hasAim()) {
			destination = destination.add(mods.worldAim(player).scale(aimScale));
		}
		player.teleportTo(destination.x, destination.y, destination.z);
		player.fallDistance = 0.0F;
	}

	/** Where a blind jump lands: straight ahead to the modified reach, stopping short of anything hit. */
	private static Vec3 blindJump(ServerPlayer player, EngraveMods mods) {
		double base = SocketSorceryConfig.get().bangleReach;
		double reach = mods.magnitude(mods.radius(base));
		Vec3 ahead = player.getEyePosition().add(player.getViewVector(1.0F).scale(reach));
		if (reach <= base) {
			return ahead; // unmodified: the activation already cleared this stretch
		}
		HitResult extended = Patterns.raycast(player, reach);
		if (extended instanceof BlockHitResult blockHit && extended.getType() == HitResult.Type.BLOCK) {
			return Vec3.atCenterOf(blockHit.getBlockPos().relative(blockHit.getDirection()));
		}
		return extended.getType() == HitResult.Type.MISS ? ahead : extended.getLocation();
	}
}
