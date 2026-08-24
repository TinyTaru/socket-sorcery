package tinytaru.socketsorcery.mixin;

import java.util.Optional;

import com.mojang.datafixers.util.Either;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tinytaru.socketsorcery.SocketSorcery;

/** Keeps the bunker out of water and sinks its saved terrain layer into the ground. */
@Mixin(JigsawStructure.class)
abstract class JigsawStructureWaterMixin {

	private static final int BUNKER_FOOTPRINT_RADIUS = 10;
	private static final int BUNKER_SINK_DEPTH = 2;

	@Inject(method = "findGenerationPoint", at = @At("HEAD"), cancellable = true)
	private void socketSorcery$skipBunkerInWater(Structure.GenerationContext context,
			CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
		if (!socketSorcery$isBunker()) {
			return;
		}

		ChunkPos chunkPos = context.chunkPos();
		int originX = chunkPos.getMinBlockX();
		int originZ = chunkPos.getMinBlockZ();
		for (int xOffset = -BUNKER_FOOTPRINT_RADIUS; xOffset <= BUNKER_FOOTPRINT_RADIUS; xOffset++) {
			for (int zOffset = -BUNKER_FOOTPRINT_RADIUS; zOffset <= BUNKER_FOOTPRINT_RADIUS; zOffset++) {
				int x = originX + xOffset;
				int z = originZ + zOffset;
				int surfaceY = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
						context.heightAccessor(), context.randomState());
				int oceanFloorY = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
						context.heightAccessor(), context.randomState());
				if (surfaceY > oceanFloorY) {
					cir.setReturnValue(Optional.empty());
					return;
				}
			}
		}
	}

	/** Moves the complete saved bunker, including its foundation, into the surrounding terrain. */
	@Inject(method = "findGenerationPoint", at = @At("RETURN"), cancellable = true)
	private void socketSorcery$sinkBunker(Structure.GenerationContext context,
			CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
		if (!socketSorcery$isBunker()) {
			return;
		}

		cir.setReturnValue(cir.getReturnValue().map(stub -> {
			StructurePiecesBuilder pieces = stub.getPiecesBuilder();
			var originalBounds = pieces.getBoundingBox();
			pieces.offsetPiecesVertically(-BUNKER_SINK_DEPTH);
			SocketSorcery.LOGGER.debug("Lowered bunker structure pieces by {} blocks: {} -> {}",
					BUNKER_SINK_DEPTH, originalBounds, pieces.getBoundingBox());
			return new Structure.GenerationStub(stub.position().below(BUNKER_SINK_DEPTH), Either.right(pieces));
		}));
	}

	private boolean socketSorcery$isBunker() {
		JigsawStructure structure = (JigsawStructure) (Object) this;
		boolean hasBunkerPoolKey = structure.getStartPool().unwrapKey()
				.map(key -> key.identifier().equals(SocketSorcery.id("bunker_start")))
				.orElse(false);
		if (hasBunkerPoolKey) {
			return true;
		}

		return structure.getStartPool().value().getTemplates().stream()
				.map(template -> template.getFirst())
				.filter(SinglePoolElement.class::isInstance)
				.map(SinglePoolElement.class::cast)
				.anyMatch(element -> element.getTemplateLocation().equals(SocketSorcery.id("bunker_1")));
	}
}
