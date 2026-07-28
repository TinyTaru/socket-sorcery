package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;

/**
 * Lays a line of flame outward from the hit location along the Direction modifiers' aim: down pools
 * it at the target's feet, up runs a column, left/right builds a wall across a corridor. The line's
 * {@code length} respects the Range modifier and its burn {@code seconds} the Duration and Power
 * modifiers, so every modifier bears on the same cast.
 *
 * <p>Does nothing without an aim — an unmodified engraving is the plain
 * {@link IgniteComponent} it sits alongside, and opposing Direction pairs cancel to no line at all.
 * Entities along the line are always set alight; fire blocks are only kindled where fire could
 * survive, so a mid-air wall still burns what walks through it without littering doomed blocks.
 */
public record FlameLineComponent(int seconds, double length, boolean placeFire, EffectFilter filter,
		EffectWhen when) implements PatternEffectComponent {

	public static final MapCodec<FlameLineComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ExtraCodecs.POSITIVE_INT.fieldOf("seconds").forGetter(FlameLineComponent::seconds),
			Codec.doubleRange(1.0, 32.0).optionalFieldOf("length", 4.0).forGetter(FlameLineComponent::length),
			Codec.BOOL.optionalFieldOf("place_fire", true).forGetter(FlameLineComponent::placeFire),
			EffectFilter.CODEC.optionalFieldOf("filter", EffectFilter.MONSTERS).forGetter(FlameLineComponent::filter),
			EffectWhen.CODEC.optionalFieldOf("when", EffectWhen.ALWAYS).forGetter(FlameLineComponent::when)
	).apply(instance, FlameLineComponent::new));

	@Override
	public MapCodec<FlameLineComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		if (!when.matches(target) || !mods.hasAim()) {
			return;
		}
		Vec3 aim = mods.worldAim(player);
		if (aim.lengthSqr() < 1.0e-6) {
			return; // opposing Direction modifiers cancelled out
		}
		Level level = player.level();
		Vec3 origin = origin(target);
		int steps = Math.max(1, Mth.floor(mods.radius(length)));
		float burn = (float) mods.magnitude(mods.duration(seconds));
		BlockPos last = null;
		for (int step = 1; step <= steps; step++) {
			BlockPos pos = BlockPos.containing(origin.add(aim.scale(step)));
			if (pos.equals(last)) {
				continue; // a shallow aim can round two steps into the same cell
			}
			last = pos;
			scorch(level, player, pos, burn);
		}
	}

	/** Where the line starts: the air cell in front of a struck face, else the hit point itself. */
	private static Vec3 origin(HitResult target) {
		if (target instanceof BlockHitResult hit && target.getType() == HitResult.Type.BLOCK) {
			return Vec3.atCenterOf(hit.getBlockPos().relative(hit.getDirection()));
		}
		return target.getLocation();
	}

	/** Sets alight whatever stands in this cell, and kindles fire there if fire could survive. */
	private void scorch(Level level, ServerPlayer player, BlockPos pos, float burn) {
		for (LivingEntity entity : level.getEntitiesOfClass(filter.entityClass(), new AABB(pos),
				e -> e.isAlive() && e != player)) {
			entity.igniteForSeconds(burn);
		}
		if (placeFire && level.getBlockState(pos).isAir() && BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) {
			level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
		}
	}
}
