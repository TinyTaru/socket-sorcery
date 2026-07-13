package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.MapCodec;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;

/** Calls a lightning bolt down at the hit location (entity or block); does nothing on a miss. */
public record SummonLightningComponent() implements PatternEffectComponent {

	public static final SummonLightningComponent INSTANCE = new SummonLightningComponent();
	public static final MapCodec<SummonLightningComponent> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<SummonLightningComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		if (target.getType() == HitResult.Type.MISS) {
			return;
		}
		Vec3 at = target.getLocation();
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(player.level());
		if (bolt != null) {
			bolt.moveTo(at.x, at.y, at.z);
			bolt.setCause(player);
			player.level().addFreshEntity(bolt);
		}
	}
}
