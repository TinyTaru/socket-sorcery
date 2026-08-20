package tinytaru.socketsorcery.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Vector3fc;

import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.block.CrystalLampBlockEntity;
import tinytaru.socketsorcery.component.CrystalLampData;
import tinytaru.socketsorcery.config.SocketSorceryConfig;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.registry.ModBlockEntities;

/**
 * Physically-inspired gobo lighting for Crystal Lamps.
 *
 * <p>The old renderer stamped the 16x16 engraving onto nearby blocks at a fixed size, which made
 * every opening look like a square emissive sticker. This renderer instead treats each engraved
 * face as an aperture in front of a virtual point light. The projection therefore expands with
 * distance, loses energy with distance and surface angle, keeps the engraving pixels mostly crisp
 * with a restrained edge blur, and is clipped by actual world geometry. Shadow edges may still
 * soften slightly from the area-source visibility samples.</p>
 *
 * <p>This deliberately stays renderer-only rather than depending on Iris or a particular shader
 * pack. Shader packs can still add their normal bloom/tonemapping on top of it.</p>
 */
public class CrystalLampRenderer implements BlockEntityRenderer<CrystalLampBlockEntity, CrystalLampRenderState> {

	private static final List<Direction> LAMP_FACES = List.of(
			Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP);
	private static final List<Direction> SURFACE_FACES = List.of(Direction.values());

	private static final int MAX_DISTANCE = 12;
	private static final double PANEL_HALF_SIZE = 0.49;
	private static final float PANEL_FACE_OFFSET = 0.005F;
	private static final double PANEL_PLANE = 0.502;
	private static final double VIRTUAL_SOURCE_OFFSET = 0.78;
	private static final double CONE_SLOPE = PANEL_HALF_SIZE / (VIRTUAL_SOURCE_OFFSET + PANEL_PLANE);
	private static final double DECAL_OFFSET = 0.0025;
	// Model-based receivers (grass, vines, flowers, crops, etc.) are drawn directly over the
	// baked model. Give those overlays their own tiny normal-space bias so the decal does not
	// fight the original cutout texture in the depth buffer.
	private static final double MODEL_DECAL_OFFSET = 0.0035;
	private static final double SHADOW_RAY_EPSILON = 0.012;
	/** Safety refresh only. Normal rebuilds are driven by relevant client-world changes. */
	private static final long CACHE_TICKS = 20L * 5L;
	/** Includes the final receiver block at the edge of a maximum-range projection. */
	private static final int PROJECTION_INVALIDATION_RADIUS = MAX_DISTANCE + 1;

	private static final int MIN_MESH_STEPS_PER_BLOCK = 2;
	private static final int MAX_MESH_STEPS_PER_BLOCK = 16;

	private static final float LIGHT_RED = 1.00F;
	private static final float LIGHT_GREEN = 0.78F;
	private static final float LIGHT_BLUE = 0.32F;

	/** Incremented by the /lamplight command so cached projection meshes rebuild immediately. */
	private static long tuningRevision;
	/**
	 * Per-chunk revisions let a lamp ignore world changes outside its projection volume. The client
	 * receives both server block updates and local prediction updates through ClientLevel#setBlock.
	 */
	private static final Map<Long, Long> projectionGeometryRevisions = new HashMap<>();
	private static long projectionGeometryRevision;

	private static final RenderPipeline PROJECTION_PIPELINE = createProjectionPipeline();
	private static final RenderPipeline MODEL_PROJECTION_PIPELINE = createModelProjectionPipeline();
	private static final RenderType PROJECTION_TYPE = RenderType.create(
			"socket_sorcery_crystal_lamp_projection",
			RenderSetup.builder(PROJECTION_PIPELINE).createRenderSetup());
	private static final RenderType MODEL_PROJECTION_TYPE = RenderType.create(
			"socket_sorcery_crystal_lamp_model_projection",
			RenderSetup.builder(MODEL_PROJECTION_PIPELINE)
					.withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
					.createRenderSetup());

	private final Map<CrystalLampBlockEntity, ProjectionCache> projectionCache = new WeakHashMap<>();

	public CrystalLampRenderer(BlockEntityRendererProvider.Context context) {
	}

	/** Fabric has not yet exposed a non-deprecated replacement for this registry on 26.1. */
	@SuppressWarnings("deprecation")
	public static void register() {
		BlockEntityRendererRegistry.register(ModBlockEntities.CRYSTAL_LAMP, CrystalLampRenderer::new);
	}

	/** Forces all lamp projection meshes to rebuild on the next render-state extraction. */
	public static void invalidateTuning() {
		tuningRevision++;
	}

	/** Marks all lamps whose receiver volume overlaps this chunk as dirty on their next render. */
	public static void invalidateProjectionGeometry(BlockPos pos) {
		invalidateProjectionGeometry(ChunkPos.containing(pos));
	}

	/** Marks all lamps whose receiver volume overlaps this chunk as dirty on their next render. */
	public static void invalidateProjectionGeometry(ChunkPos chunkPos) {
		projectionGeometryRevisions.put(chunkPos.pack(), ++projectionGeometryRevision);
	}

	/** Clears client-world bookkeeping when disconnecting, preventing stale chunk revisions from accumulating. */
	public static void clearProjectionGeometryInvalidation() {
		projectionGeometryRevisions.clear();
		projectionGeometryRevision = 0L;
	}

	@Override
	public CrystalLampRenderState createRenderState() {
		return new CrystalLampRenderState();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}

	@Override
	public void extractRenderState(CrystalLampBlockEntity lamp, CrystalLampRenderState state, float partialTick,
			Vec3 cameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(lamp, state, partialTick, cameraPos, breakProgress);
		state.lampData = lamp.lampData();
		Level level = lamp.getLevel();
		if (level == null) {
			state.projections = List.of();
			return;
		}
		long tick = level.getGameTime();
		ProjectionCache cached = projectionCache.get(lamp);
		if (cached == null || !cached.data().equals(state.lampData) || cached.tuningRevision() != tuningRevision
				|| projectionGeometryChanged(cached) || tick - cached.tick() >= CACHE_TICKS) {
			cached = new ProjectionCache(state.lampData, tick, tuningRevision, projectionGeometryRevision,
					projectionMinChunk(lamp.getBlockPos().getX()), projectionMaxChunk(lamp.getBlockPos().getX()),
					projectionMinChunk(lamp.getBlockPos().getZ()), projectionMaxChunk(lamp.getBlockPos().getZ()),
					buildProjectors(lamp, state.lampData));
			projectionCache.put(lamp, cached);
		}
		state.projections = cached.batches();
	}

	private static boolean projectionGeometryChanged(ProjectionCache cached) {
		for (int chunkX = cached.minChunkX(); chunkX <= cached.maxChunkX(); chunkX++) {
			for (int chunkZ = cached.minChunkZ(); chunkZ <= cached.maxChunkZ(); chunkZ++) {
				if (projectionGeometryRevisions.getOrDefault(ChunkPos.pack(chunkX, chunkZ), 0L)
						> cached.geometryRevision()) {
					return true;
				}
			}
		}
		return false;
	}

	private static int projectionMinChunk(int blockCoordinate) {
		return Math.floorDiv(blockCoordinate - PROJECTION_INVALIDATION_RADIUS, 16);
	}

	private static int projectionMaxChunk(int blockCoordinate) {
		return Math.floorDiv(blockCoordinate + PROJECTION_INVALIDATION_RADIUS, 16);
	}

	@Override
	public void submit(CrystalLampRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		for (Direction face : LAMP_FACES) {
			long[] mask = state.lampData.mask(face);
			if (!GridBits.isEmpty(mask)) {
				float[] center = panelCenter(face);
				submitFaceMask(collector, poseStack, mask, center[0], center[1], center[2], face);
			}
		}
		for (CrystalLampRenderState.ProjectionBatch batch : state.projections) {
			if (!batch.quads().isEmpty()) {
				collector.submitCustomGeometry(poseStack, PROJECTION_TYPE, (pose, consumer) -> {
					for (CrystalLampRenderState.SurfaceQuad quad : batch.quads()) {
						emitSurfaceQuad(consumer, pose, quad);
					}
				});
			}
			if (!batch.modelQuads().isEmpty()) {
				collector.submitCustomGeometry(poseStack, MODEL_PROJECTION_TYPE, (pose, consumer) -> {
					for (CrystalLampRenderState.ModelSurfaceQuad quad : batch.modelQuads()) {
						emitModelSurfaceQuad(consumer, pose, quad);
					}
				});
			}
		}
	}

	private static RenderPipeline createProjectionPipeline() {
		return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
				.withLocation(SocketSorcery.id("pipeline/crystal_lamp_projection"))
				.withVertexShader(SocketSorcery.id("core/crystal_lamp_projection"))
				.withFragmentShader(SocketSorcery.id("core/crystal_lamp_projection"))
				.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
				// LIGHTNING is SRC_ALPHA + ONE. Unlike ADDITIVE (ONE + ONE), it lets per-vertex
				// alpha actually control light energy, which is essential for falloff and soft edges.
				.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
				.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
				.withCull(false)
				.build());
	}

	private static RenderPipeline createModelProjectionPipeline() {
		return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
				.withLocation(SocketSorcery.id("pipeline/crystal_lamp_model_projection"))
				.withVertexShader(SocketSorcery.id("core/crystal_lamp_model_projection"))
				.withFragmentShader(SocketSorcery.id("core/crystal_lamp_model_projection"))
				.withSampler("Sampler0")
				.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
				.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
				.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
				.withCull(false)
				.build());
	}

	private List<CrystalLampRenderState.ProjectionBatch> buildProjectors(CrystalLampBlockEntity lamp,
			CrystalLampData data) {
		Level level = lamp.getLevel();
		if (level == null) {
			return List.of();
		}

		SocketSorceryConfig tuning = SocketSorceryConfig.get();
		BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
		RandomSource modelRandom = RandomSource.create();
		List<CrystalLampRenderState.ProjectionBatch> batches = new ArrayList<>();
		BlockPos origin = lamp.getBlockPos();
		Vec3 lampCenter = Vec3.atCenterOf(origin);

		for (Direction projectorFace : LAMP_FACES) {
			long[] mask = data.mask(projectorFace);
			if (GridBits.isEmpty(mask)) {
				continue;
			}

			float[] projectorRight = right(projectorFace);
			float[] projectorUp = up(projectorFace);
			float[] projectorForward = directionVector(projectorFace);
			List<CrystalLampRenderState.SurfaceQuad> quads = new ArrayList<>();
			List<CrystalLampRenderState.ModelSurfaceQuad> modelQuads = new ArrayList<>();

			for (int depth = 1; depth <= MAX_DISTANCE; depth++) {
				double farHalfWidth = projectionHalfWidth(depth + 1.0);
				int radius = Math.min(MAX_DISTANCE, (int) Math.ceil(farHalfWidth + 1.0));
				for (int vertical = -radius; vertical <= radius; vertical++) {
					for (int horizontal = -radius; horizontal <= radius; horizontal++) {
						BlockPos target = offset(origin, projectorFace, depth, projectorRight, horizontal,
								projectorUp, vertical);
						BlockState blockState = level.getBlockState(target);
						VoxelShape shape = blockState.getShape(level, target, CollisionContext.empty());

						// Thin/cutout blocks are visually defined by their baked model, not by their voxel shape.
						// Render the light on that actual model (and preserve its texture alpha) so grass, vines,
						// flowers, crops, panes, rails, etc. do not glow as crude hitboxes.
						boolean useRenderedModel = blockState.getRenderShape() == RenderShape.MODEL
								&& (!blockState.isSolidRender() || shape.isEmpty());
						if (useRenderedModel) {
							Vec3 modelOffset = blockState.getOffset(target);
							AABB modelBounds = new AABB(target).move(modelOffset).inflate(0.35);
							if (boxTouchesProjectorFrustum(modelBounds, lampCenter, projectorRight,
									projectorUp, projectorForward, tuning)) {
								addLitModel(level, origin, lampCenter, projectorFace, projectorRight,
										projectorUp, projectorForward, mask, target, blockState, modelSet,
										modelRandom, tuning, modelQuads);
							}
							continue;
						}

						if (shape.isEmpty()) {
							continue;
						}

						for (AABB localBox : shape.toAabbs()) {
							AABB worldBox = localBox.move(target);
							if (!boxTouchesProjectorFrustum(worldBox, lampCenter, projectorRight,
									projectorUp, projectorForward, tuning)) {
								continue;
							}
							for (Direction surfaceFace : SURFACE_FACES) {
								addLitSurface(level, origin, lampCenter, projectorFace, projectorRight,
										projectorUp, projectorForward, mask, worldBox, surfaceFace, tuning, quads);
							}
						}
					}
				}
			}

			if (!quads.isEmpty() || !modelQuads.isEmpty()) {
				batches.add(new CrystalLampRenderState.ProjectionBatch(projectorFace, List.copyOf(quads),
						List.copyOf(modelQuads)));
			}
		}
		return List.copyOf(batches);
	}

	private static boolean boxTouchesProjectorFrustum(AABB box, Vec3 lampCenter, float[] projectorRight,
			float[] projectorUp, float[] projectorForward, SocketSorceryConfig tuning) {
		Vec3 center = box.getCenter();
		Vec3 relative = center.subtract(lampCenter);
		double forward = dot(relative, projectorForward);
		double forwardExtent = tangentExtent(box, projectorForward);
		double minForward = forward - forwardExtent;
		double maxForward = forward + forwardExtent;
		if (maxForward < PANEL_PLANE || minForward > MAX_DISTANCE + 0.75) {
			return false;
		}

		double halfWidth = projectionHalfWidth(Math.max(PANEL_PLANE, maxForward));
		double horizontal = dot(relative, projectorRight);
		double vertical = dot(relative, projectorUp);
		double horizontalExtent = tangentExtent(box, projectorRight);
		double verticalExtent = tangentExtent(box, projectorUp);
		double blurMargin = halfWidth * tuning.lampLightCullMarginFactor + tuning.lampLightCullMarginBase;
		return Math.abs(horizontal) - horizontalExtent <= halfWidth + blurMargin
				&& Math.abs(vertical) - verticalExtent <= halfWidth + blurMargin;
	}

	private static void addLitModel(Level level, BlockPos lampOrigin, Vec3 lampCenter, Direction projectorFace,
			float[] projectorRight, float[] projectorUp, float[] projectorForward, long[] mask, BlockPos target,
			BlockState blockState, BlockStateModelSet modelSet, RandomSource modelRandom, SocketSorceryConfig tuning,
			List<CrystalLampRenderState.ModelSurfaceQuad> output) {
		BlockStateModel model = modelSet.get(blockState);
		List<BlockStateModelPart> parts = new ArrayList<>();
		modelRandom.setSeed(blockState.getSeed(target));
		model.collectParts(modelRandom, parts);
		if (parts.isEmpty()) {
			return;
		}

		Vec3 modelOrigin = Vec3.atLowerCornerOf(target).add(blockState.getOffset(target));
		for (BlockStateModelPart part : parts) {
			for (Direction cullFace : SURFACE_FACES) {
				addLitModelQuads(level, lampOrigin, lampCenter, projectorFace, projectorRight, projectorUp,
						projectorForward, mask, modelOrigin, part.getQuads(cullFace), tuning, output);
			}
			addLitModelQuads(level, lampOrigin, lampCenter, projectorFace, projectorRight, projectorUp,
					projectorForward, mask, modelOrigin, part.getQuads(null), tuning, output);
		}
	}

	private static void addLitModelQuads(Level level, BlockPos lampOrigin, Vec3 lampCenter,
			Direction projectorFace, float[] projectorRight, float[] projectorUp, float[] projectorForward,
			long[] mask, Vec3 modelOrigin, List<BakedQuad> bakedQuads, SocketSorceryConfig tuning,
			List<CrystalLampRenderState.ModelSurfaceQuad> output) {
		for (BakedQuad quad : bakedQuads) {
			addLitModelQuad(level, lampOrigin, lampCenter, projectorFace, projectorRight, projectorUp,
					projectorForward, mask, modelOrigin, quad, tuning, output);
		}
	}

	private static void addLitModelQuad(Level level, BlockPos lampOrigin, Vec3 lampCenter,
			Direction projectorFace, float[] projectorRight, float[] projectorUp, float[] projectorForward,
			long[] mask, Vec3 modelOrigin, BakedQuad quad, SocketSorceryConfig tuning,
			List<CrystalLampRenderState.ModelSurfaceQuad> output) {
		Vec3 q0 = modelPoint(modelOrigin, quad.position0());
		Vec3 q1 = modelPoint(modelOrigin, quad.position1());
		Vec3 q2 = modelPoint(modelOrigin, quad.position2());
		Vec3 q3 = modelPoint(modelOrigin, quad.position3());

		Vec3 normal = q1.subtract(q0).cross(q3.subtract(q0));
		double normalLength = normal.length();
		if (normalLength <= 1.0E-7) {
			return;
		}
		normal = normal.scale(1.0 / normalLength);

		Vec3 quadCenter = q0.add(q1).add(q2).add(q3).scale(0.25);
		// Baked cutout quads are commonly two-sided. Bias the projected-light copy toward the
		// lamp-facing side so the overlay is consistently in front of the source model instead
		// of sitting exactly coplanar with it (which causes Z-fighting on grass/cave vines).
		if (normal.dot(lampCenter.subtract(quadCenter)) < 0.0) {
			normal = normal.scale(-1.0);
		}
		double forwardAtCenter = dot(quadCenter.subtract(lampCenter), projectorForward);
		if (forwardAtCenter < PANEL_PLANE - 0.75 || forwardAtCenter > MAX_DISTANCE + 0.75) {
			return;
		}

		double edgeU = Math.max(q0.distanceTo(q3), q1.distanceTo(q2));
		double edgeV = Math.max(q0.distanceTo(q1), q3.distanceTo(q2));
		if (edgeU <= 1.0E-6 || edgeV <= 1.0E-6) {
			return;
		}

		double representativeForward = Math.max(PANEL_PLANE, forwardAtCenter);
		double projectedCellSize = (projectionHalfWidth(representativeForward) * 2.0) / 16.0;
		int stepsPerBlock = clampInt((int) Math.ceil(tuning.lampLightSamplesPerPatternCell
				/ Math.max(0.035, projectedCellSize)), MIN_MESH_STEPS_PER_BLOCK, MAX_MESH_STEPS_PER_BLOCK);
		int stepsU = Math.max(1, (int) Math.ceil(edgeU * stepsPerBlock));
		int stepsV = Math.max(1, (int) Math.ceil(edgeV * stepsPerBlock));

		float quadU0 = UVPair.unpackU(quad.packedUV0());
		float quadV0 = UVPair.unpackV(quad.packedUV0());
		float quadU1 = UVPair.unpackU(quad.packedUV1());
		float quadV1 = UVPair.unpackV(quad.packedUV1());
		float quadU2 = UVPair.unpackU(quad.packedUV2());
		float quadV2 = UVPair.unpackV(quad.packedUV2());
		float quadU3 = UVPair.unpackU(quad.packedUV3());
		float quadV3 = UVPair.unpackV(quad.packedUV3());

		for (int y = 0; y < stepsV; y++) {
			double t0 = (double) y / stepsV;
			double t1 = (double) (y + 1) / stepsV;
			for (int x = 0; x < stepsU; x++) {
				double s0 = (double) x / stepsU;
				double s1 = (double) (x + 1) / stepsU;

				Vec3 p0 = bilerp(q0, q1, q2, q3, s0, t0);
				Vec3 p1 = bilerp(q0, q1, q2, q3, s0, t1);
				Vec3 p2 = bilerp(q0, q1, q2, q3, s1, t1);
				Vec3 p3 = bilerp(q0, q1, q2, q3, s1, t0);

				double a0 = unshadowedIntensity(mask, lampCenter, projectorRight, projectorUp,
						projectorForward, normal, true, p0, tuning);
				double a1 = unshadowedIntensity(mask, lampCenter, projectorRight, projectorUp,
						projectorForward, normal, true, p1, tuning);
				double a2 = unshadowedIntensity(mask, lampCenter, projectorRight, projectorUp,
						projectorForward, normal, true, p2, tuning);
				double a3 = unshadowedIntensity(mask, lampCenter, projectorRight, projectorUp,
						projectorForward, normal, true, p3, tuning);

				Vec3 center = p0.add(p1).add(p2).add(p3).scale(0.25);
				double centerIntensity = unshadowedIntensity(mask, lampCenter, projectorRight, projectorUp,
						projectorForward, normal, true, center, tuning);
				double peak = Math.max(centerIntensity, Math.max(Math.max(a0, a1), Math.max(a2, a3)));
				if (peak < tuning.lampLightMinOpacity) {
					continue;
				}

				if (centerIntensity > 0.0) {
					double centerFloor = centerIntensity * tuning.lampLightCenterFloor;
					a0 = Math.max(a0, centerFloor);
					a1 = Math.max(a1, centerFloor);
					a2 = Math.max(a2, centerFloor);
					a3 = Math.max(a3, centerFloor);
				}

				double visibility = softVisibility(level, lampCenter, projectorFace, projectorRight,
						projectorUp, projectorForward, center, tuning);
				if (visibility <= 0.001) {
					continue;
				}

				float u0 = bilerp(quadU0, quadU1, quadU2, quadU3, s0, t0);
				float v0 = bilerp(quadV0, quadV1, quadV2, quadV3, s0, t0);
				float u1 = bilerp(quadU0, quadU1, quadU2, quadU3, s0, t1);
				float v1 = bilerp(quadV0, quadV1, quadV2, quadV3, s0, t1);
				float u2 = bilerp(quadU0, quadU1, quadU2, quadU3, s1, t1);
				float v2 = bilerp(quadV0, quadV1, quadV2, quadV3, s1, t1);
				float u3 = bilerp(quadU0, quadU1, quadU2, quadU3, s1, t0);
				float v3 = bilerp(quadV0, quadV1, quadV2, quadV3, s1, t0);

				output.add(modelSurfaceQuad(lampOrigin, normal,
						p0, a0 * visibility, u0, v0, p1, a1 * visibility, u1, v1,
						p2, a2 * visibility, u2, v2, p3, a3 * visibility, u3, v3));
			}
		}
	}

	private static void addLitSurface(Level level, BlockPos lampOrigin, Vec3 lampCenter, Direction projectorFace,
			float[] projectorRight, float[] projectorUp, float[] projectorForward, long[] mask, AABB box,
			Direction surfaceFace, SocketSorceryConfig tuning, List<CrystalLampRenderState.SurfaceQuad> output) {
		Vec3 faceCenter = faceCenter(box, surfaceFace);
		Vec3 toLamp = lampCenter.subtract(faceCenter);
		double facing = toLamp.x * surfaceFace.getStepX() + toLamp.y * surfaceFace.getStepY()
				+ toLamp.z * surfaceFace.getStepZ();
		if (facing <= 1.0E-4) {
			return;
		}

		Vec3 relative = faceCenter.subtract(lampCenter);
		double forwardAtCenter = dot(relative, projectorForward);
		if (forwardAtCenter < PANEL_PLANE - 0.75 || forwardAtCenter > MAX_DISTANCE + 0.75) {
			return;
		}

		float[] surfaceRight = right(surfaceFace);
		float[] surfaceUp = up(surfaceFace);
		double halfRight = tangentExtent(box, surfaceRight);
		double halfUp = tangentExtent(box, surfaceUp);
		if (halfRight <= 1.0E-6 || halfUp <= 1.0E-6) {
			return;
		}

		double representativeForward = Math.max(PANEL_PLANE,
				forwardAtCenter + tangentExtent(box, projectorForward) * 0.5);
		double projectedCellSize = (projectionHalfWidth(representativeForward) * 2.0) / 16.0;
		int stepsPerBlock = clampInt((int) Math.ceil(tuning.lampLightSamplesPerPatternCell
				/ Math.max(0.035, projectedCellSize)),
				MIN_MESH_STEPS_PER_BLOCK, MAX_MESH_STEPS_PER_BLOCK);
		int stepsRight = Math.max(1, (int) Math.ceil(halfRight * 2.0 * stepsPerBlock));
		int stepsUp = Math.max(1, (int) Math.ceil(halfUp * 2.0 * stepsPerBlock));

		for (int y = 0; y < stepsUp; y++) {
			double v0 = -halfUp + (halfUp * 2.0) * y / stepsUp;
			double v1 = -halfUp + (halfUp * 2.0) * (y + 1) / stepsUp;
			for (int x = 0; x < stepsRight; x++) {
				double u0 = -halfRight + (halfRight * 2.0) * x / stepsRight;
				double u1 = -halfRight + (halfRight * 2.0) * (x + 1) / stepsRight;

				Vec3 p0 = surfacePoint(faceCenter, surfaceRight, surfaceUp, u0, v1);
				Vec3 p1 = surfacePoint(faceCenter, surfaceRight, surfaceUp, u0, v0);
				Vec3 p2 = surfacePoint(faceCenter, surfaceRight, surfaceUp, u1, v0);
				Vec3 p3 = surfacePoint(faceCenter, surfaceRight, surfaceUp, u1, v1);

				double a0 = unshadowedIntensity(mask, lampCenter, projectorRight, projectorUp,
						projectorForward, surfaceFace, p0, tuning);
				double a1 = unshadowedIntensity(mask, lampCenter, projectorRight, projectorUp,
						projectorForward, surfaceFace, p1, tuning);
				double a2 = unshadowedIntensity(mask, lampCenter, projectorRight, projectorUp,
						projectorForward, surfaceFace, p2, tuning);
				double a3 = unshadowedIntensity(mask, lampCenter, projectorRight, projectorUp,
						projectorForward, surfaceFace, p3, tuning);

				Vec3 center = p0.add(p1).add(p2).add(p3).scale(0.25);
				double centerIntensity = unshadowedIntensity(mask, lampCenter, projectorRight, projectorUp,
						projectorForward, surfaceFace, center, tuning);
				double peak = Math.max(centerIntensity, Math.max(Math.max(a0, a1), Math.max(a2, a3)));
				if (peak < tuning.lampLightMinOpacity) {
					continue;
				}

				// If a tiny engraving opening falls between mesh vertices, retain enough of the center sample
				// to prevent the 16x16 pattern from disappearing at close range.
				if (centerIntensity > 0.0) {
					double centerFloor = centerIntensity * tuning.lampLightCenterFloor;
					a0 = Math.max(a0, centerFloor);
					a1 = Math.max(a1, centerFloor);
					a2 = Math.max(a2, centerFloor);
					a3 = Math.max(a3, centerFloor);
				}

				double visibility = softVisibility(level, lampCenter, projectorFace, projectorRight,
						projectorUp, projectorForward, center, tuning);
				if (visibility <= 0.001) {
					continue;
				}

				output.add(surfaceQuad(lampOrigin, surfaceFace, p0, a0 * visibility,
						p1, a1 * visibility, p2, a2 * visibility, p3, a3 * visibility));
			}
		}
	}

	private static double unshadowedIntensity(long[] mask, Vec3 lampCenter, float[] projectorRight,
			float[] projectorUp, float[] projectorForward, Direction surfaceFace, Vec3 point,
			SocketSorceryConfig tuning) {
		Vec3 normal = new Vec3(surfaceFace.getStepX(), surfaceFace.getStepY(), surfaceFace.getStepZ());
		return unshadowedIntensity(mask, lampCenter, projectorRight, projectorUp, projectorForward, normal,
				false, point, tuning);
	}

	private static double unshadowedIntensity(long[] mask, Vec3 lampCenter, float[] projectorRight,
			float[] projectorUp, float[] projectorForward, Vec3 surfaceNormal, boolean twoSided, Vec3 point,
			SocketSorceryConfig tuning) {
		Vec3 relative = point.subtract(lampCenter);
		double forward = dot(relative, projectorForward);
		if (forward < PANEL_PLANE - 0.02 || forward > MAX_DISTANCE + 0.35) {
			return 0.0;
		}

		double halfWidth = projectionHalfWidth(forward);
		if (halfWidth <= 1.0E-5) {
			return 0.0;
		}
		double u = 0.5 + dot(relative, projectorRight) / (halfWidth * 2.0);
		double v = 0.5 - dot(relative, projectorUp) / (halfWidth * 2.0);
		double distance = relative.length();
		double blur = tuning.lampLightBlurBase + Math.max(0.0, distance - 0.5) * tuning.lampLightBlurPerBlock;
		double aperture = blurredMask(mask, u, v, blur, tuning.lampLightWideBlurWeight);
		if (aperture <= 0.0001) {
			return 0.0;
		}

		Vec3 towardLamp = relative.scale(-1.0).normalize();
		double incidence = towardLamp.dot(surfaceNormal);
		if (twoSided) {
			incidence = Math.abs(incidence);
		} else {
			incidence = Math.max(0.0, incidence);
		}
		if (incidence <= 0.0) {
			return 0.0;
		}
		incidence = Math.sqrt(incidence);

		// A game-friendly inverse-square-ish rolloff. Pure inverse-square is much too harsh at Minecraft
		// block scale, so the constant/linear terms keep the near field readable.
		double attenuation = 1.0 / (1.0 + tuning.lampLightLinearFalloff * distance
				+ tuning.lampLightQuadraticFalloff * distance * distance);
		double rangeStart = MAX_DISTANCE * tuning.lampLightRangeFadeStart;
		double rangeFade = 1.0 - smoothstep(rangeStart, MAX_DISTANCE, distance);
		return tuning.lampLightBrightness * aperture * incidence * attenuation * rangeFade;
	}

	/** 3x3 Gaussian-ish aperture sample plus a faint wider lobe for a restrained penumbra. */
	private static double blurredMask(long[] mask, double u, double v, double radius, double wideWeight) {
		// Crisp light is the normal setting. Avoid thirteen identical bit lookups for that case.
		if (radius <= 0.0 && wideWeight <= 0.0) {
			return maskAt(mask, u, v);
		}
		double narrow = maskAt(mask, u, v) * 4.0
				+ maskAt(mask, u - radius, v) * 2.0
				+ maskAt(mask, u + radius, v) * 2.0
				+ maskAt(mask, u, v - radius) * 2.0
				+ maskAt(mask, u, v + radius) * 2.0
				+ maskAt(mask, u - radius, v - radius)
				+ maskAt(mask, u + radius, v - radius)
				+ maskAt(mask, u - radius, v + radius)
				+ maskAt(mask, u + radius, v + radius);
		narrow /= 16.0;
		if (wideWeight <= 0.0) {
			return narrow;
		}

		double wideRadius = radius * 2.15;
		double wide = (maskAt(mask, u - wideRadius, v) + maskAt(mask, u + wideRadius, v)
				+ maskAt(mask, u, v - wideRadius) + maskAt(mask, u, v + wideRadius)) * 0.25;
		return narrow * (1.0 - wideWeight) + wide * wideWeight;
	}

	private static double maskAt(long[] mask, double u, double v) {
		if (u < 0.0 || u >= 1.0 || v < 0.0 || v >= 1.0) {
			return 0.0;
		}
		int col = Math.min(15, (int) (u * 16.0));
		int row = Math.min(15, (int) (v * 16.0));
		return GridBits.getIndex(mask, row * 16 + col) ? 1.0 : 0.0;
	}

	/** One, two, or three visibility rays approximate a small area source at the selected quality. */
	private static double softVisibility(Level level, Vec3 lampCenter, Direction projectorFace,
			float[] projectorRight, float[] projectorUp, float[] projectorForward, Vec3 point,
			SocketSorceryConfig tuning) {
		Vec3 relative = point.subtract(lampCenter);
		double forward = dot(relative, projectorForward);
		double halfWidth = projectionHalfWidth(forward);
		if (halfWidth <= 1.0E-5) {
			return 0.0;
		}
		double u = 0.5 + dot(relative, projectorRight) / (halfWidth * 2.0);
		double v = 0.5 - dot(relative, projectorUp) / (halfWidth * 2.0);
		Vec3 aperture = aperturePoint(lampCenter, projectorFace, projectorRight, projectorUp, u, v);
		int samples = tuning.lampLightShadowSamples;
		if (samples <= 1 || tuning.lampLightShadowSoftness <= 0.0) {
			return rayVisible(level, aperture, point);
		}

		Vec3 diagonal = new Vec3(projectorRight[0] + projectorUp[0],
				projectorRight[1] + projectorUp[1], projectorRight[2] + projectorUp[2]).normalize();
		double offset = tuning.lampLightShadowSoftness;
		if (samples == 2) {
			double visibility = rayVisible(level, aperture.add(diagonal.scale(offset * 0.5)), point);
			visibility += rayVisible(level, aperture.add(diagonal.scale(-offset * 0.5)), point);
			return visibility * 0.5;
		}
		double visibility = rayVisible(level, aperture, point);
		visibility += rayVisible(level, aperture.add(diagonal.scale(offset)), point);
		visibility += rayVisible(level, aperture.add(diagonal.scale(-offset)), point);
		return visibility / 3.0;
	}

	private static Vec3 aperturePoint(Vec3 lampCenter, Direction projectorFace, float[] projectorRight,
			float[] projectorUp, double u, double v) {
		double clampedU = clamp(u, 0.001, 0.999);
		double clampedV = clamp(v, 0.001, 0.999);
		double horizontal = (clampedU - 0.5) * PANEL_HALF_SIZE * 2.0;
		double vertical = (0.5 - clampedV) * PANEL_HALF_SIZE * 2.0;
		return lampCenter.add(projectorFace.getStepX() * (PANEL_PLANE + 0.004),
				projectorFace.getStepY() * (PANEL_PLANE + 0.004),
				projectorFace.getStepZ() * (PANEL_PLANE + 0.004))
				.add(projectorRight[0] * horizontal + projectorUp[0] * vertical,
						projectorRight[1] * horizontal + projectorUp[1] * vertical,
						projectorRight[2] * horizontal + projectorUp[2] * vertical);
	}

	private static double rayVisible(Level level, Vec3 start, Vec3 point) {
		Vec3 delta = point.subtract(start);
		double length = delta.length();
		if (length <= SHADOW_RAY_EPSILON * 2.0) {
			return 1.0;
		}
		Vec3 end = point.subtract(delta.scale(SHADOW_RAY_EPSILON / length));
		BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.VISUAL,
				ClipContext.Fluid.NONE, CollisionContext.empty()));
		return hit.getType() == HitResult.Type.MISS ? 1.0 : 0.0;
	}

	private static CrystalLampRenderState.SurfaceQuad surfaceQuad(BlockPos lampOrigin, Direction surfaceFace,
			Vec3 a, double alphaA, Vec3 b, double alphaB, Vec3 c, double alphaC, Vec3 d, double alphaD) {
		Vec3 origin = Vec3.atLowerCornerOf(lampOrigin);
		Vec3 offset = new Vec3(surfaceFace.getStepX() * DECAL_OFFSET,
				surfaceFace.getStepY() * DECAL_OFFSET, surfaceFace.getStepZ() * DECAL_OFFSET);
		a = a.subtract(origin).add(offset);
		b = b.subtract(origin).add(offset);
		c = c.subtract(origin).add(offset);
		d = d.subtract(origin).add(offset);
		return new CrystalLampRenderState.SurfaceQuad(surfaceFace,
				(float) a.x, (float) a.y, (float) a.z, (float) clamp(alphaA, 0.0, 1.0),
				(float) b.x, (float) b.y, (float) b.z, (float) clamp(alphaB, 0.0, 1.0),
				(float) c.x, (float) c.y, (float) c.z, (float) clamp(alphaC, 0.0, 1.0),
				(float) d.x, (float) d.y, (float) d.z, (float) clamp(alphaD, 0.0, 1.0));
	}

	private static CrystalLampRenderState.ModelSurfaceQuad modelSurfaceQuad(BlockPos lampOrigin, Vec3 surfaceNormal,
			Vec3 a, double alphaA, float uA, float vA, Vec3 b, double alphaB, float uB, float vB,
			Vec3 c, double alphaC, float uC, float vC, Vec3 d, double alphaD, float uD, float vD) {
		Vec3 origin = Vec3.atLowerCornerOf(lampOrigin);
		Vec3 decalBias = surfaceNormal.scale(MODEL_DECAL_OFFSET);
		a = a.add(decalBias).subtract(origin);
		b = b.add(decalBias).subtract(origin);
		c = c.add(decalBias).subtract(origin);
		d = d.add(decalBias).subtract(origin);
		return new CrystalLampRenderState.ModelSurfaceQuad(
				(float) a.x, (float) a.y, (float) a.z, (float) clamp(alphaA, 0.0, 1.0), uA, vA,
				(float) b.x, (float) b.y, (float) b.z, (float) clamp(alphaB, 0.0, 1.0), uB, vB,
				(float) c.x, (float) c.y, (float) c.z, (float) clamp(alphaC, 0.0, 1.0), uC, vC,
				(float) d.x, (float) d.y, (float) d.z, (float) clamp(alphaD, 0.0, 1.0), uD, vD);
	}

	private static Vec3 modelPoint(Vec3 modelOrigin, Vector3fc local) {
		return modelOrigin.add(local.x(), local.y(), local.z());
	}

	private static Vec3 bilerp(Vec3 q0, Vec3 q1, Vec3 q2, Vec3 q3, double s, double t) {
		Vec3 top = q0.lerp(q3, s);
		Vec3 bottom = q1.lerp(q2, s);
		return top.lerp(bottom, t);
	}

	private static float bilerp(float q0, float q1, float q2, float q3, double s, double t) {
		double top = q0 + (q3 - q0) * s;
		double bottom = q1 + (q2 - q1) * s;
		return (float) (top + (bottom - top) * t);
	}

	private static double projectionHalfWidth(double forward) {
		return Math.max(0.001, (forward + VIRTUAL_SOURCE_OFFSET) * CONE_SLOPE);
	}

	private static double smoothstep(double edge0, double edge1, double x) {
		if (edge1 <= edge0) {
			return x >= edge1 ? 1.0 : 0.0;
		}
		double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
		return t * t * (3.0 - 2.0 * t);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double dot(Vec3 vector, float[] axis) {
		return vector.x * axis[0] + vector.y * axis[1] + vector.z * axis[2];
	}

	private static float[] directionVector(Direction direction) {
		return new float[] { direction.getStepX(), direction.getStepY(), direction.getStepZ() };
	}

	private static BlockPos offset(BlockPos origin, Direction face, int depth, float[] right, int horizontal,
			float[] up, int vertical) {
		return origin.offset(
				face.getStepX() * depth + Math.round(right[0]) * horizontal + Math.round(up[0]) * vertical,
				face.getStepY() * depth + Math.round(right[1]) * horizontal + Math.round(up[1]) * vertical,
				face.getStepZ() * depth + Math.round(right[2]) * horizontal + Math.round(up[2]) * vertical);
	}

	private static Vec3 faceCenter(AABB box, Direction normal) {
		double x = (box.minX + box.maxX) * 0.5;
		double y = (box.minY + box.maxY) * 0.5;
		double z = (box.minZ + box.maxZ) * 0.5;
		return switch (normal) {
			case EAST -> new Vec3(box.maxX, y, z);
			case WEST -> new Vec3(box.minX, y, z);
			case UP -> new Vec3(x, box.maxY, z);
			case DOWN -> new Vec3(x, box.minY, z);
			case SOUTH -> new Vec3(x, y, box.maxZ);
			case NORTH -> new Vec3(x, y, box.minZ);
		};
	}

	private static double tangentExtent(AABB box, float[] axis) {
		return Math.abs(axis[0]) * (box.maxX - box.minX) * 0.5
				+ Math.abs(axis[1]) * (box.maxY - box.minY) * 0.5
				+ Math.abs(axis[2]) * (box.maxZ - box.minZ) * 0.5;
	}

	private static Vec3 surfacePoint(Vec3 center, float[] right, float[] up, double horizontal, double vertical) {
		return center.add(right[0] * horizontal + up[0] * vertical,
				right[1] * horizontal + up[1] * vertical,
				right[2] * horizontal + up[2] * vertical);
	}

	private static void submitFaceMask(SubmitNodeCollector collector, PoseStack poseStack, long[] mask,
			float cx, float cy, float cz, Direction normal) {
		float[] right = right(normal);
		float[] up = up(normal);
		collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
			for (int cell = 0; cell < 256; cell++) {
				if (!GridBits.getIndex(mask, cell)) {
					continue;
				}
				int row = cell / 16;
				int col = cell % 16;
				float left = -0.49F + col * 0.98F / 16.0F;
				float rightEdge = -0.49F + (col + 1) * 0.98F / 16.0F;
				float top = 0.49F - row * 0.98F / 16.0F;
				float bottom = 0.49F - (row + 1) * 0.98F / 16.0F;
				emitFaceQuad(consumer, pose, cx, cy, cz, right, up, normal, left, rightEdge, bottom, top);
			}
		});
	}

	private static void emitSurfaceQuad(VertexConsumer consumer, PoseStack.Pose pose,
			CrystalLampRenderState.SurfaceQuad q) {
		surfaceVertex(consumer, pose, q.x0(), q.y0(), q.z0(), q.a0());
		surfaceVertex(consumer, pose, q.x1(), q.y1(), q.z1(), q.a1());
		surfaceVertex(consumer, pose, q.x2(), q.y2(), q.z2(), q.a2());
		surfaceVertex(consumer, pose, q.x3(), q.y3(), q.z3(), q.a3());
	}

	private static void surfaceVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
			float alpha) {
		consumer.addVertex(pose, x, y, z).setColor(LIGHT_RED, LIGHT_GREEN, LIGHT_BLUE, alpha);
	}

	private static void emitModelSurfaceQuad(VertexConsumer consumer, PoseStack.Pose pose,
			CrystalLampRenderState.ModelSurfaceQuad q) {
		modelSurfaceVertex(consumer, pose, q.x0(), q.y0(), q.z0(), q.a0(), q.u0(), q.v0());
		modelSurfaceVertex(consumer, pose, q.x1(), q.y1(), q.z1(), q.a1(), q.u1(), q.v1());
		modelSurfaceVertex(consumer, pose, q.x2(), q.y2(), q.z2(), q.a2(), q.u2(), q.v2());
		modelSurfaceVertex(consumer, pose, q.x3(), q.y3(), q.z3(), q.a3(), q.u3(), q.v3());
	}

	private static void modelSurfaceVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
			float alpha, float u, float v) {
		consumer.addVertex(pose, x, y, z)
				.setColor(LIGHT_RED, LIGHT_GREEN, LIGHT_BLUE, alpha)
				.setUv(u, v);
	}

	private static void emitFaceQuad(VertexConsumer consumer, PoseStack.Pose pose, float cx, float cy, float cz,
			float[] right, float[] up, Direction normal, float left, float rightEdge, float bottom, float top) {
		float[] topLeft = point(cx, cy, cz, right, up, left, top);
		float[] bottomLeft = point(cx, cy, cz, right, up, left, bottom);
		float[] bottomRight = point(cx, cy, cz, right, up, rightEdge, bottom);
		float[] topRight = point(cx, cy, cz, right, up, rightEdge, top);
		if (normal == Direction.UP) {
			faceVertex(consumer, pose, topLeft);
			faceVertex(consumer, pose, bottomLeft);
			faceVertex(consumer, pose, bottomRight);
			faceVertex(consumer, pose, topRight);
		} else {
			// The side-face basis points inward with the normal winding; reverse it so the
			// exterior-facing pattern is retained when the render type culls back faces.
			faceVertex(consumer, pose, topLeft);
			faceVertex(consumer, pose, topRight);
			faceVertex(consumer, pose, bottomRight);
			faceVertex(consumer, pose, bottomLeft);
		}
	}

	private static void faceVertex(VertexConsumer consumer, PoseStack.Pose pose, float[] point) {
		consumer.addVertex(pose.pose(), point[0], point[1], point[2]).setColor(1.0F, 0.91F, 0.55F, 0.92F);
	}

	private static float[] panelCenter(Direction direction) {
		return switch (direction) {
			case NORTH -> new float[] { 0.5F, 0.5F, -PANEL_FACE_OFFSET };
			case EAST -> new float[] { 1.0F + PANEL_FACE_OFFSET, 0.5F, 0.5F };
			case SOUTH -> new float[] { 0.5F, 0.5F, 1.0F + PANEL_FACE_OFFSET };
			case WEST -> new float[] { -PANEL_FACE_OFFSET, 0.5F, 0.5F };
			case UP -> new float[] { 0.5F, 1.0F + PANEL_FACE_OFFSET, 0.5F };
			case DOWN -> throw new IllegalArgumentException("Crystal Lamps do not have a lower glass panel");
		};
	}

	private static float[] point(float cx, float cy, float cz, float[] right, float[] up, float horizontal,
			float vertical) {
		return new float[] { cx + right[0] * horizontal + up[0] * vertical,
				cy + right[1] * horizontal + up[1] * vertical,
				cz + right[2] * horizontal + up[2] * vertical };
	}

	private static float[] right(Direction normal) {
		return switch (normal) {
			case NORTH -> new float[] { 1, 0, 0 };
			case EAST -> new float[] { 0, 0, 1 };
			case SOUTH -> new float[] { -1, 0, 0 };
			case WEST -> new float[] { 0, 0, -1 };
			case UP, DOWN -> new float[] { 1, 0, 0 };
		};
	}

	private static float[] up(Direction normal) {
		return switch (normal) {
			case NORTH, EAST, SOUTH, WEST -> new float[] { 0, 1, 0 };
			case UP -> new float[] { 0, 0, -1 };
			case DOWN -> new float[] { 0, 0, 1 };
		};
	}

	private record ProjectionCache(CrystalLampData data, long tick, long tuningRevision, long geometryRevision,
			int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ,
			List<CrystalLampRenderState.ProjectionBatch> batches) {
	}
}
