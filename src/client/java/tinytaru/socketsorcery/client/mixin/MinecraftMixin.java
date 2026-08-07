package tinytaru.socketsorcery.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tinytaru.socketsorcery.client.ScrollDrawingMode;

/** Routes vanilla attack/use keybind actions into the GUI-less transcription interaction. */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void socketSorcery$paintInsteadOfAttack(CallbackInfoReturnable<Boolean> ci) {
		if (ScrollDrawingMode.isActive()) {
			ScrollDrawingMode.paintAtCursor();
			ci.setReturnValue(false);
		}
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void socketSorcery$toggleTranscription(CallbackInfo ci) {
		if (ScrollDrawingMode.isActive()) {
			ScrollDrawingMode.exit();
			ci.cancel();
		} else if (ScrollDrawingMode.openIfHoldingBlankScroll()) {
			ci.cancel();
		}
	}
}
