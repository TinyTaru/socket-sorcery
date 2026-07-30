package tinytaru.socketsorcery.pattern.effect;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;

/**
 * Lays a line of flame outward from the hit location along the Direction modifiers' aim: down pools
 * it at the target's feet, up runs a column, left/right builds a wall across a corridor. The line's
 * {@code length} respects the Range modifier and its burn {@code seconds} the Duration and Power
 * modifiers, so every modifier bears on the same cast.
 *
 * <p>The geometry is {@link EffectTargets#aimedLineCells}, shared with the {@code aimed_line}
 * target — so an engraving with no Direction modifier casts nothing here and the plain
 * {@link IgniteComponent} alongside it carries the cast, exactly as before modifiers existed.
 *
 * <p>Entities along the line are always set alight; fire blocks are only kindled where fire could
 * survive, so a mid-air wall still burns what walks through it without littering doomed blocks.
 * This component exists for that block-laying half — the burning half is what {@code aimed_line}
 * would give any other component.
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
		if (!when.matches(target)) {
			return;
		}
		List<BlockPos> cells = EffectTargets.aimedLineCells(player, target, mods, length);
		if (cells.isEmpty()) {
			return; // no Direction modifier, or opposing ones cancelled out
		}
		float burn = (float) mods.magnitude(mods.duration(seconds));
		for (LivingEntity entity : EffectTargets.resolve(EffectTarget.AIMED_LINE, player, target, mods, length, filter)) {
			entity.igniteForSeconds(burn);
		}
		if (!placeFire) {
			return;
		}
		Level level = player.level();
		for (BlockPos pos : cells) {
			if (level.getBlockState(pos).isAir() && BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) {
				level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
			}
		}
	}
}
