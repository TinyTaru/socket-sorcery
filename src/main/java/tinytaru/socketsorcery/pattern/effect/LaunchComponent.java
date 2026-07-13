package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;

/**
 * Propels the wearer: a look-direction dash (Leaping) or a straight upward launch (Wind). Magnitude
 * respects the Power modifier; {@code aim_scale} lets Direction modifiers bias the dash. Always
 * forces a velocity packet and clears fall distance.
 */
public record LaunchComponent(Direction direction, double magnitude, double yBoost, double aimScale,
		EffectWhen when) implements PatternEffectComponent {

	public enum Direction implements StringRepresentable {
		LOOK("look"),
		UP("up");

		public static final Codec<Direction> CODEC = StringRepresentable.fromEnum(Direction::values);

		private final String name;

		Direction(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final MapCodec<LaunchComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Direction.CODEC.optionalFieldOf("direction", Direction.LOOK).forGetter(LaunchComponent::direction),
			Codec.DOUBLE.fieldOf("magnitude").forGetter(LaunchComponent::magnitude),
			Codec.DOUBLE.optionalFieldOf("y_boost", 0.0).forGetter(LaunchComponent::yBoost),
			Codec.DOUBLE.optionalFieldOf("aim_scale", 0.0).forGetter(LaunchComponent::aimScale),
			EffectWhen.CODEC.optionalFieldOf("when", EffectWhen.ALWAYS).forGetter(LaunchComponent::when)
	).apply(instance, LaunchComponent::new));

	@Override
	public MapCodec<LaunchComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		if (!when.matches(target)) {
			return;
		}
		Vec3 velocity;
		if (direction == Direction.UP) {
			velocity = player.getDeltaMovement().add(0.0, mods.magnitude(magnitude), 0.0);
		} else {
			velocity = player.getViewVector(1.0F).scale(mods.magnitude(magnitude)).add(0.0, yBoost, 0.0);
			if (aimScale > 0.0 && mods.hasAim()) {
				velocity = velocity.add(mods.worldAim(player).scale(aimScale));
			}
		}
		player.setDeltaMovement(velocity);
		player.hurtMarked = true; // forces a velocity packet to the client
		player.fallDistance = 0.0F;
	}
}
