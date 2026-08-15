package tinytaru.socketsorcery.client;

import java.util.ArrayList;
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

import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
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
	private static final double PANEL_PLANE = 0.502;
	private static final double VIRTUAL_SOURCE_OFFSET = 0.78;
	private static final double CONE_SLOPE = PANEL_HALF_SIZE / (VIRTUAL_SOURCE_OFFSET + PANEL_PLANE);
	private static final double DECAL_OFFSET = 0.0025;
	private static final double SHADOW_RAY_EPSILON = 0.012;
	private static final long CACHE_TICKS = 10;

	private static final int MIN_MESH_STEPS_PER_BLOCK = 2;
	private static final int MAX_MESH_STEPS_PER_BLOCK = 16;

	private static final float LIGHT_RED = 1.00F;
	private static final float LIGHT_GREEN = 0.78F;
	private static final float LIGHT_BLUE = 0.32F;

	/** Incremented by the /lamplight command so cached projection meshes rebuild immediately. */
	private static long tuningRevision;

	private static final RenderPipeline PROJECTION_PIPELINE = createProjectionPipeline();
	private static final RenderType PROJECTION_TYPE = RenderType.create(
			"socket_sorcery_crystal_lamp_projection",
			RenderSetup.builder(PROJECTION_PIPELINE).createRenderSetup());

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
				|| tick - cached.tick() >= CACHE_TICKS) {
			cached = new ProjectionCache(state.lampData, tick, tuningRevision, buildProjectors(lamp, state.lampData));
			projectionCache.put(lamp, cached);
		}
		state.projections = cached.batches();
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
			collector.submitCustomGeometry(poseStack, PROJECTION_TYPE, (pose, consumer) -> {
				for (CrystalLampRenderState.SurfaceQuad quad : batch.quads()) {
					emitSurfaceQuad(consumer, pose, quad);
				}
			});
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

	private List<CrystalLampRenderState.ProjectionBatch> buildProjectors(CrystalLampBlockEntity lamp,
			CrystalLampData data) {
		Level level = lamp.getLevel();
		if (level == null) {
			return List.of();
		}

		SocketSorceryConfig tuning = SocketSorceryConfig.get();
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

			for (int depth = 1; depth <= MAX_DISTANCE; depth++) {
				double farHalfWidth = projectionHalfWidth(depth + 1.0);
				int radius = Math.min(MAX_DISTANCE, (int) Math.ceil(farHalfWidth + 1.0));
				for (int vertical = -radius; vertical <= radius; vertical++) {
					for (int horizontal = -radius; horizontal <= radius; horizontal++) {
						BlockPos target = offset(origin, projectorFace, depth, projectorRight, horizontal,
								projectorUp, vertical);
						BlockState blockState = level.getBlockState(target);
						VoxelShape shape = blockState.getShape(level, target, CollisionContext.empty());
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

			if (!quads.isEmpty()) {
				batches.add(new CrystalLampRenderState.ProjectionBatch(projectorFace, List.copyOf(quads)));
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
		double incidence = Math.max(0.0, towardLamp.x * surfaceFace.getStepX()
				+ towardLamp.y * surfaceFace.getStepY() + towardLamp.z * surfaceFace.getStepZ());
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

	/**
	 * Three nearby visibility rays approximate a small area source. This produces a soft blocker edge
	 * instead of the binary cutout/shadow boundary made by a single ray.
	 */
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

		Vec3 diagonal = new Vec3(projectorRight[0] + projectorUp[0],
				projectorRight[1] + projectorUp[1], projectorRight[2] + projectorUp[2]).normalize();
		double visibility = rayVisible(level, aperture, point);
		visibility += rayVisible(level, aperture.add(diagonal.scale(tuning.lampLightShadowSoftness)), point);
		visibility += rayVisible(level, aperture.add(diagonal.scale(-tuning.lampLightShadowSoftness)), point);
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
				emitFaceQuad(consumer, pose, cx, cy, cz, right, up, left, rightEdge, bottom, top);
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

	private static void emitFaceQuad(VertexConsumer consumer, PoseStack.Pose pose, float cx, float cy, float cz,
			float[] right, float[] up, float left, float rightEdge, float bottom, float top) {
		faceVertex(consumer, pose, point(cx, cy, cz, right, up, left, top));
		faceVertex(consumer, pose, point(cx, cy, cz, right, up, left, bottom));
		faceVertex(consumer, pose, point(cx, cy, cz, right, up, rightEdge, bottom));
		faceVertex(consumer, pose, point(cx, cy, cz, right, up, rightEdge, top));
	}

	private static void faceVertex(VertexConsumer consumer, PoseStack.Pose pose, float[] point) {
		consumer.addVertex(pose.pose(), point[0], point[1], point[2]).setColor(1.0F, 0.91F, 0.55F, 0.92F);
	}

	private static float[] panelCenter(Direction direction) {
		return switch (direction) {
			case NORTH -> new float[] { 0.5F, 0.5F, -0.002F };
			case EAST -> new float[] { 1.002F, 0.5F, 0.5F };
			case SOUTH -> new float[] { 0.5F, 0.5F, 1.002F };
			case WEST -> new float[] { -0.002F, 0.5F, 0.5F };
			case UP -> new float[] { 0.5F, 1.002F, 0.5F };
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

	private record ProjectionCache(CrystalLampData data, long tick, long tuningRevision,
			List<CrystalLampRenderState.ProjectionBatch> batches) {
	}
}
