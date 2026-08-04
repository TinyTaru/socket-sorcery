package tinytaru.socketsorcery.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * Keeps the 64x64 advancement background at native pixel density. Vanilla's
 * advancement renderer tiles every background as a 16x16 logical texture,
 * which makes this artwork appear four times too small.
 */
@Mixin(AdvancementTab.class)
public abstract class AdvancementTabMixin {

	private static final Identifier SOCKET_SORCERY_BACKGROUND = SocketSorcery.id(
			"textures/gui/advancements/advancement_bg.png");
	private static final int TILE_SIZE = 64;

	@Shadow
	private double scrollX;

	@Shadow
	private double scrollY;

	@Unique
	private boolean socketSorcery$backgroundDrawn;

	@Inject(method = "extractContents", at = @At("HEAD"))
	private void socketSorcery$resetBackground(GuiGraphicsExtractor graphics, int x, int y, CallbackInfo ci) {
		socketSorcery$backgroundDrawn = false;
	}

	@Redirect(
			method = "extractContents",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
	private void socketSorcery$drawBackground(
			GuiGraphicsExtractor graphics,
			RenderPipeline pipeline,
			Identifier texture,
			int x,
			int y,
			float u,
			float v,
			int width,
			int height,
			int textureWidth,
			int textureHeight) {
		if (!SOCKET_SORCERY_BACKGROUND.equals(texture)) {
			graphics.blit(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
			return;
		}

		// The vanilla loop issues 144 16x16 blits. Replace that whole loop with
		// a single native-density 64x64 tile grid and let the existing scissor
		// rectangle clip it to the advancement content area.
		if (socketSorcery$backgroundDrawn) {
			return;
		}
		socketSorcery$backgroundDrawn = true;

		int startX = Mth.floor(scrollX) % TILE_SIZE - TILE_SIZE;
		int startY = Mth.floor(scrollY) % TILE_SIZE - TILE_SIZE;
		for (int tileX = startX; tileX < 234; tileX += TILE_SIZE) {
			for (int tileY = startY; tileY < 113; tileY += TILE_SIZE) {
				graphics.blit(pipeline, texture, tileX, tileY, 0.0F, 0.0F,
						TILE_SIZE, TILE_SIZE, TILE_SIZE, TILE_SIZE);
			}
		}
	}
}
