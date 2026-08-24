package tinytaru.socketsorcery.mixin;

import java.util.concurrent.atomic.AtomicBoolean;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import tinytaru.socketsorcery.SocketSorcery;

/** Lowers only the temple's terrain adaptation, without moving any structure pieces. */
@Mixin(Beardifier.class)
abstract class BeardifierTempleMixin {

	private static final int TEMPLE_BEARD_OFFSET = 1;
	private static final AtomicBoolean LOGGED_TEMPLE_OFFSET = new AtomicBoolean();

	@ModifyExpressionValue(method = "forStructuresInChunk", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/structure/PoolElementStructurePiece;getGroundLevelDelta()I"))
	private static int socketSorcery$lowerTemplePieceBeard(int original,
			@Local(ordinal = 0) StructureStart structureStart) {
		if (!socketSorcery$isTemple(structureStart.getStructure())) {
			return original;
		}

		if (LOGGED_TEMPLE_OFFSET.compareAndSet(false, true)) {
			SocketSorcery.LOGGER.debug("Lowering temple terrain adaptation by {} block", TEMPLE_BEARD_OFFSET);
		}
		return original - TEMPLE_BEARD_OFFSET;
	}

	@ModifyArg(method = "forStructuresInChunk", at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1), index = 0)
	private static Object socketSorcery$lowerTempleJunctionBeard(Object value,
			@Local(ordinal = 0) StructureStart structureStart) {
		if (!socketSorcery$isTemple(structureStart.getStructure()) || !(value instanceof JigsawJunction junction)) {
			return value;
		}

		return new JigsawJunction(junction.getSourceX(), junction.getSourceGroundY() - TEMPLE_BEARD_OFFSET,
				junction.getSourceZ(), junction.getDeltaY(), junction.getDestProjection());
	}

	private static boolean socketSorcery$isTemple(Structure structure) {
		if (!(structure instanceof JigsawStructure jigsawStructure)) {
			return false;
		}

		boolean hasTemplePoolKey = jigsawStructure.getStartPool().unwrapKey()
				.map(key -> key.identifier().equals(SocketSorcery.id("temple_start")))
				.orElse(false);
		if (hasTemplePoolKey) {
			return true;
		}

		return jigsawStructure.getStartPool().value().getTemplates().stream()
				.map(template -> template.getFirst())
				.filter(SinglePoolElement.class::isInstance)
				.map(SinglePoolElement.class::cast)
				.anyMatch(element -> element.getTemplateLocation().equals(SocketSorcery.id("temple_building")));
	}
}
