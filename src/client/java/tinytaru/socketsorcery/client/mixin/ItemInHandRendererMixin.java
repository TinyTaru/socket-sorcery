package tinytaru.socketsorcery.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tinytaru.socketsorcery.client.ScrollDrawingMode;
import tinytaru.socketsorcery.client.ScrollItemRenderer;
import tinytaru.socketsorcery.item.BlankScrollItem;

/** Holds the active Blank Scroll near the camera without opening a screen. */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void socketSorcery$holdScrollUp(AbstractClientPlayer player, float partialTick, float pitch,
			InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack,
			SubmitNodeCollector collector, int light, CallbackInfo ci) {
		if (!ScrollDrawingMode.isActive() || hand != InteractionHand.MAIN_HAND || !(stack.getItem() instanceof BlankScrollItem)) return;
		poseStack.pushPose();
		// Render the icon directly rather than through an item-display transform. That gives drawing a
		// stable camera plane whose exact geometry is ray-tested in ScrollDrawingMode.
		poseStack.translate(-ScrollDrawingMode.SCROLL_SCALE / 2.0F,
				-ScrollDrawingMode.SCROLL_SCALE / 2.0F + ScrollDrawingMode.SCROLL_Y_OFFSET,
				-ScrollDrawingMode.SCROLL_DISTANCE - ScrollDrawingMode.SCROLL_SCALE / 2.0F);
		poseStack.scale(ScrollDrawingMode.SCROLL_SCALE, ScrollDrawingMode.SCROLL_SCALE, ScrollDrawingMode.SCROLL_SCALE);
		ScrollItemRenderer.submitCloseUp(stack, poseStack, collector, light);
		poseStack.popPose();
		ci.cancel();
	}
}
