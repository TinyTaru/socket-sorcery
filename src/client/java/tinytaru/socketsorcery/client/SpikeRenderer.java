package tinytaru.socketsorcery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.entity.SpikeEntity;
import tinytaru.socketsorcery.registry.ModEntities;

/**
 * Draws a {@link SpikeEntity}. The transform chain is vanilla's {@code EvokerFangsRenderer} verbatim —
 * the same problem, solved the same way — with a per-entity scale added and the tint passed through the
 * long {@code submitModel} overload, whose colour argument is what lets one greyscale texture serve every
 * pattern.
 */
public class SpikeRenderer extends EntityRenderer<SpikeEntity, SpikeRenderState> {

	private static final Identifier TEXTURE = SocketSorcery.id("textures/entity/spike/spike.png");

	private final SpikeModel model;

	public SpikeRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new SpikeModel(context.bakeLayer(SpikeModel.LAYER));
		this.shadowRadius = 0.0F; // it is part of the ground; a blob shadow under it reads as floating
	}

	/**
	 * Fabric marks {@code EntityRendererRegistry} deprecated at class level in this API version but ships
	 * nothing to replace it — the registry class is the only entity-renderer hook in the jar, and vanilla's
	 * own {@code EntityRenderers.register} is private. Suppressed rather than worked around, so the warning
	 * doesn't read as an oversight; revisit if a successor appears.
	 */
	@SuppressWarnings("deprecation")
	public static void register() {
		ModelLayerRegistry.registerModelLayer(SpikeModel.LAYER, SpikeModel::create);
		EntityRendererRegistry.register(ModEntities.SPIKE, SpikeRenderer::new);
	}

	@Override
	public SpikeRenderState createRenderState() {
		return new SpikeRenderState();
	}

	@Override
	public void extractRenderState(SpikeEntity entity, SpikeRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yRot = entity.getYRot(partialTick);
		state.riseProgress = entity.riseProgress(partialTick);
		state.scale = entity.scale();
		state.color = (entity.color() & 0x00FFFFFF)
				| (Mth.floor(entity.alpha(partialTick) * 255.0F) << 24);
	}

	@Override
	public void submit(SpikeRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		if (state.riseProgress > 0.0F) {
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(90.0F - state.yRot));
			poseStack.scale(state.scale, state.scale, state.scale);
			poseStack.scale(-1.0F, -1.0F, 1.0F);
			poseStack.translate(0.0F, -1.501F, 0.0F);
			model.setupAnim(state);
			collector.submitModel(model, state, poseStack, model.renderType(TEXTURE),
					state.lightCoords, OverlayTexture.NO_OVERLAY, state.color,
					null, state.outlineColor, null);
			poseStack.popPose();
		}
		super.submit(state, poseStack, collector, camera);
	}
}
