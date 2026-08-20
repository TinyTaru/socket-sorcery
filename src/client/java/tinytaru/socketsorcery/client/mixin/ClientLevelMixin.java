package tinytaru.socketsorcery.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tinytaru.socketsorcery.client.CrystalLampRenderer;

/** Keeps Crystal Lamp projection caches current without polling the world twice per second. */
@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {

	// Server corrections call Level#setBlock directly, bypassing ClientLevel#setBlock below.
	@Inject(method = "setServerVerifiedBlockState", at = @At("TAIL"))
	private void socketSorcery$invalidateLampProjectionFromServer(BlockPos pos, BlockState state, int flags,
			CallbackInfo ci) {
		CrystalLampRenderer.invalidateProjectionGeometry(pos);
	}

	@Inject(method = "setBlock", at = @At("RETURN"))
	private void socketSorcery$invalidateLampProjection(BlockPos pos, BlockState state, int flags, int maxUpdateDepth,
			CallbackInfoReturnable<Boolean> cir) {
		if (Boolean.TRUE.equals(cir.getReturnValue())) {
			CrystalLampRenderer.invalidateProjectionGeometry(pos);
		}
	}

	@Inject(method = "onChunkLoaded", at = @At("TAIL"))
	private void socketSorcery$invalidateLampProjectionOnLoad(ChunkPos chunkPos, CallbackInfo ci) {
		CrystalLampRenderer.invalidateProjectionGeometry(chunkPos);
	}

	@Inject(method = "unload", at = @At("HEAD"))
	private void socketSorcery$invalidateLampProjectionOnUnload(LevelChunk chunk, CallbackInfo ci) {
		CrystalLampRenderer.invalidateProjectionGeometry(chunk.getPos());
	}

	@Inject(method = "disconnect", at = @At("HEAD"))
	private void socketSorcery$clearLampProjectionInvalidation(CallbackInfo ci) {
		CrystalLampRenderer.clearProjectionGeometryInvalidation();
	}
}
