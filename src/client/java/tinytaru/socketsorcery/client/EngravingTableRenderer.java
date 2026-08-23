package tinytaru.socketsorcery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.block.EngravingTableBlockEntity;
import tinytaru.socketsorcery.registry.ModBlockEntities;

/** Draws the current engraving-table workpiece and chisel flat on its upper surface. */
public class EngravingTableRenderer
		implements BlockEntityRenderer<EngravingTableBlockEntity, EngravingTableRenderState> {

	/** Blockbench model coordinates use 16 units for one block. */
	private static final float MODEL_UNIT = 1.0F / 16.0F;

	// GEM_ATTACHMENT: from [3.32, 14.874, 3.32], to [11.32, 15.374, 11.32].
	private static final float GEM_X = 7.32F * MODEL_UNIT;
	private static final float GEM_Y = 15.124F * MODEL_UNIT;
	private static final float GEM_Z = 7.32F * MODEL_UNIT;
	private static final float GEM_SCALE = 8.0F * MODEL_UNIT;
	private static final float GEM_YAW = 0.0F;

	// CHISEL_ATTACHMENT: 22.5° around [12.16, 15.104, 11.48]. Its rotated centre is
	// [12.16, 15.104, 10.48], which is the centre used by the item renderer below. The item sprite
	// begins vertical and is laid flat by a 90° X rotation, so its texture axes are 90° offset from
	// Blockbench's top-face texture axes.
	private static final float CHISEL_X = 12.16F * MODEL_UNIT;
	private static final float CHISEL_Y = 15.104F * MODEL_UNIT;
	private static final float CHISEL_Z = 10.48F * MODEL_UNIT;
	private static final float CHISEL_SCALE = 7.36F * MODEL_UNIT;
	private static final float CHISEL_YAW = 90.0F + 22.5F;
	private final ItemModelResolver itemModelResolver;

	private EngravingTableRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModelResolver = context.itemModelResolver();
	}

	/** Fabric has not yet exposed a non-deprecated replacement for this registry on 26.1. */
	@SuppressWarnings("deprecation")
	public static void register() {
		BlockEntityRendererRegistry.register(ModBlockEntities.ENGRAVING_TABLE, EngravingTableRenderer::new);
	}

	@Override
	public EngravingTableRenderState createRenderState() {
		return new EngravingTableRenderState();
	}

	@Override
	public void extractRenderState(EngravingTableBlockEntity table, EngravingTableRenderState state,
			float partialTick, Vec3 cameraPos,
			net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(table, state, partialTick, cameraPos, breakProgress);
		Level level = table.getLevel();
		if (level == null) {
			state.gem = null;
			state.chisel = null;
			return;
		}
		// The base renderer samples light at the block entity's position, inside the solid tabletop.
		// The workpiece sits in the air immediately above it, so use that light sample instead.
		state.lightCoords = LevelRenderer.getLightCoords(level, table.getBlockPos().above());
		int seed = Long.hashCode(table.getBlockPos().asLong());
		state.gem = resolveItem(table.getItem(EngravingTableBlockEntity.SLOT_GEM), level, seed);
		state.chisel = resolveItem(table.getItem(EngravingTableBlockEntity.SLOT_CHISEL), level, seed + 1);
	}

	@Override
	public void submit(EngravingTableRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		submitItem(state.gem, poseStack, collector, state.lightCoords,
				GEM_X, GEM_Y, GEM_Z, GEM_SCALE, GEM_YAW);
		submitItem(state.chisel, poseStack, collector, state.lightCoords,
				CHISEL_X, CHISEL_Y, CHISEL_Z, CHISEL_SCALE, CHISEL_YAW);
	}

	private ItemStackRenderState resolveItem(ItemStack stack, Level level, int seed) {
		if (stack.isEmpty()) {
			return null;
		}
		ItemStackRenderState itemState = new ItemStackRenderState();
		itemModelResolver.updateForTopItem(itemState, stack, ItemDisplayContext.ON_SHELF, level, null, seed);
		return itemState;
	}

	private static void submitItem(ItemStackRenderState item, PoseStack poseStack, SubmitNodeCollector collector,
			int light, float x, float y, float z, float scale, float yaw) {
		if (item == null) {
			return;
		}
		poseStack.pushPose();
		// The table top ends at y=15/16. Rotate the item's face upward and give it enough clearance
		// to avoid depth fighting with the wood.
		poseStack.translate(x, y, z);
		poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
		poseStack.scale(scale, scale, scale);
		item.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}
}
