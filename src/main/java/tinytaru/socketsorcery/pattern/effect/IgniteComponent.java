package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.pattern.EngraveMods;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;

/**
 * Sets the hit entity on fire for {@code seconds} (scaled by the Duration and Power modifiers), or —
 * when {@code place_fire_on_block} is set and a block was hit — kindles a fire on the struck face.
 */
public record IgniteComponent(int seconds, boolean placeFireOnBlock) implements PatternEffectComponent {

	public static final MapCodec<IgniteComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ExtraCodecs.POSITIVE_INT.fieldOf("seconds").forGetter(IgniteComponent::seconds),
			Codec.BOOL.optionalFieldOf("place_fire_on_block", false).forGetter(IgniteComponent::placeFireOnBlock)
	).apply(instance, IgniteComponent::new));

	@Override
	public MapCodec<IgniteComponent> codec() {
		return CODEC;
	}

	@Override
	public void apply(ServerPlayer player, HitResult target, EngraveMods mods) {
		if (target instanceof EntityHitResult hit) {
			hit.getEntity().igniteForSeconds((float) mods.magnitude(mods.duration(seconds)));
		} else if (placeFireOnBlock && target instanceof BlockHitResult hit && target.getType() == HitResult.Type.BLOCK) {
			Level level = player.level();
			BlockPos firePos = hit.getBlockPos().relative(hit.getDirection());
			if (level.getBlockState(firePos).isAir()) {
				level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
			}
		}
	}
}
