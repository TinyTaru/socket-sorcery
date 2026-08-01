package tinytaru.socketsorcery.pattern.effect;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;

/**
 * Calls lightning down at the hit location (entity or block); does nothing on a miss.
 *
 * <p>With a Direction modifier the single bolt becomes a march of them, one per cell of the aimed
 * line — {@code length} blocks long, extended by Range — so the strike walks outward from what was
 * struck instead of standing still. Power adds a bolt at every strike point: a vanilla bolt's damage
 * and ignition are fixed, so the only honest way to make a lightning strike harder is more
 * lightning.
 */
public record SummonLightningComponent(double length) implements PatternEffectComponent {

	public static final MapCodec<SummonLightningComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.doubleRange(1.0, 32.0).optionalFieldOf("length", 4.0).forGetter(SummonLightningComponent::length)
	).apply(instance, SummonLightningComponent::new));

	@Override
	public MapCodec<SummonLightningComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		if (target.getType() == HitResult.Type.MISS) {
			return;
		}
		int bolts = 1 + Math.max(0, mods.power());
		List<BlockPos> line = EffectTargets.aimedLineCells(player, target, mods, length);
		if (line.isEmpty()) { // no Direction modifier, or opposing ones cancelled out
			strike(player, target.getLocation(), bolts);
			return;
		}
		for (BlockPos pos : line) {
			strike(player, Vec3.atBottomCenterOf(pos), bolts);
		}
	}

	private static void strike(ServerPlayer player, Vec3 at, int bolts) {
		Level level = player.level();
		for (int i = 0; i < bolts; i++) {
			LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
			if (bolt != null) {
				bolt.snapTo(at.x, at.y, at.z);
				bolt.setCause(player);
				level.addFreshEntity(bolt);
			}
		}
	}
}
