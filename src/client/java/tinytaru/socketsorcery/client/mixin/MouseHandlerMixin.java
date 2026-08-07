package tinytaru.socketsorcery.client.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tinytaru.socketsorcery.client.ScrollDrawingMode;

/** Keeps vanilla from grabbing the cursor again when a transcription stroke begins. */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
	@Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
	private void socketSorcery$keepCursorReleased(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
		if (!ScrollDrawingMode.isActive()) return;
		ScrollDrawingMode.onMouseButton(button.button(), action);
		// Releases must still reach vanilla so its key mappings clear. If we swallow them, the use
		// key remains logically held and repeatedly opens/closes the mode (and recenters the cursor).
		if (action == org.lwjgl.glfw.GLFW.GLFW_RELEASE) return;
		ci.cancel();
	}
}
