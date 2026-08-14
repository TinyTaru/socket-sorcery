package tinytaru.socketsorcery.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
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
import tinytaru.socketsorcery.block.CrystalLampBlockEntity;
import tinytaru.socketsorcery.component.CrystalLampData;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.registry.ModBlockEntities;

/**
 * Casts one real client-side ray through every engraved opening. Rays fan away from the lamp so a
 * design grows across several blocks, and every projected pixel lands on the exact outline box it
 * struck. That keeps light on slab, stair, wall, and fence geometry instead of painting empty air.
 */
public class CrystalLampRenderer implements BlockEntityRenderer<CrystalLampBlockEntity, CrystalLampRenderState> {

	private static final List<Direction> FACES = List.of(
			Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP);
	private static final double MAX_PROJECTION_DISTANCE = 12.0;
	private static final double PROJECTION_FAN = 0.48;
	private static final double SURFACE_OFFSET = 0.002;
	private static final long CACHE_TICKS = 5;
	private final Map<CrystalLampBlockEntity, ProjectionCache> projectionCache = new WeakHashMap<>();

	public CrystalLampRenderer(BlockEntityRendererProvider.Context context) {
	}

	/** Fabric has not yet exposed a non-deprecated replacement for this registry on 26.1. */
	@SuppressWarnings("deprecation")
	public static void register() {
		BlockEntityRendererRegistry.register(ModBlockEntities.CRYSTAL_LAMP, CrystalLampRenderer::new);
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
			state.lightHits = List.of();
			return;
		}
		long tick = level.getGameTime();
		ProjectionCache cached = projectionCache.get(lamp);
		if (cached == null || !cached.data().equals(state.lampData) || tick - cached.tick() >= CACHE_TICKS) {
			cached = new ProjectionCache(state.lampData, tick, findLightHits(lamp));
			projectionCache.put(lamp, cached);
		}
		state.lightHits = cached.hits();
	}

	@Override
	public void submit(CrystalLampRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		for (Direction face : FACES) {
			long[] mask = state.lampData.mask(face);
			if (!GridBits.isEmpty(mask)) {
				float[] panel = panelCenter(face);
				submitFaceMask(collector, poseStack, mask, panel[0], panel[1], panel[2], face);
			}
		}
		if (!state.lightHits.isEmpty()) {
			collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
				for (CrystalLampRenderState.LightHit hit : state.lightHits) {
					emitLightSpot(consumer, pose, hit);
				}
			});
		}
	}

	private static List<CrystalLampRenderState.LightHit> findLightHits(CrystalLampBlockEntity lamp) {
		Level level = lamp.getLevel();
		if (level == null) {
			return List.of();
		}
		List<CrystalLampRenderState.LightHit> hits = new ArrayList<>();
		BlockPos origin = lamp.getBlockPos();
		Vec3 blockOrigin = Vec3.atLowerCornerOf(origin);
		for (Direction face : FACES) {
			long[] mask = lamp.lampData().mask(face);
			float[] right = right(face);
			float[] up = up(face);
			for (int cell = 0; cell < 256; cell++) {
				if (!GridBits.getIndex(mask, cell)) {
					continue;
				}
				int row = cell / 16;
				int col = cell % 16;
				double horizontal = (col + 0.5) / 16.0 - 0.5;
				double vertical = 0.5 - (row + 0.5) / 16.0;
				Vec3 start = blockOrigin.add(0.5, 0.5, 0.5)
						.add(face.getStepX() * 0.502, face.getStepY() * 0.502, face.getStepZ() * 0.502)
						.add(right[0] * horizontal, right[1] * horizontal, right[2] * horizontal)
						.add(up[0] * vertical, up[1] * vertical, up[2] * vertical);
				Vec3 ray = new Vec3(
						face.getStepX() + right[0] * horizontal * PROJECTION_FAN + up[0] * vertical * PROJECTION_FAN,
						face.getStepY() + right[1] * horizontal * PROJECTION_FAN + up[1] * vertical * PROJECTION_FAN,
						face.getStepZ() + right[2] * horizontal * PROJECTION_FAN + up[2] * vertical * PROJECTION_FAN)
						.normalize();
				BlockHitResult result = level.clip(new ClipContext(start, start.add(ray.scale(MAX_PROJECTION_DISTANCE)),
						ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty()));
				if (result.getType() == HitResult.Type.BLOCK) {
					hits.add(createLightHit(level, origin, start, result));
				}
			}
		}
		return List.copyOf(hits);
	}

	private static CrystalLampRenderState.LightHit createLightHit(Level level, BlockPos origin, Vec3 source,
			BlockHitResult result) {
		Vec3 location = result.getLocation();
		Direction normal = result.getDirection();
		Vec3 local = location.subtract(Vec3.atLowerCornerOf(origin)).add(
				normal.getStepX() * SURFACE_OFFSET,
				normal.getStepY() * SURFACE_OFFSET,
				normal.getStepZ() * SURFACE_OFFSET);
		double distance = source.distanceTo(location);
		float size = (float) Math.clamp((1.0 + PROJECTION_FAN * distance) / 16.0 * 0.96, 0.055, 0.44);
		double[] bounds = hitBoxBounds(level, origin, result);
		return new CrystalLampRenderState.LightHit(normal, (float) local.x, (float) local.y, (float) local.z,
				size, (float) bounds[0], (float) bounds[1], (float) bounds[2], (float) bounds[3]);
	}

	/** Finds the individual outline cuboid struck by the ray, then returns its two tangent bounds. */
	private static double[] hitBoxBounds(Level level, BlockPos origin, BlockHitResult hit) {
		BlockPos target = hit.getBlockPos();
		BlockState state = level.getBlockState(target);
		VoxelShape shape = state.getShape(level, target, CollisionContext.empty());
		Vec3 point = hit.getLocation();
		Direction normal = hit.getDirection();
		AABB chosen = null;
		double chosenArea = Double.MAX_VALUE;
		for (AABB localBox : shape.toAabbs()) {
			AABB box = localBox.move(target);
			if (liesOnFace(box, point, normal)) {
				double area = tangentArea(box, normal);
				if (area < chosenArea) {
					chosen = box;
					chosenArea = area;
				}
			}
		}
		if (chosen == null) {
			chosen = new AABB(target);
		}
		float[] right = right(normal);
		float[] up = up(normal);
		Vec3 relativeMin = new Vec3(chosen.minX - origin.getX(), chosen.minY - origin.getY(), chosen.minZ - origin.getZ());
		Vec3 relativeMax = new Vec3(chosen.maxX - origin.getX(), chosen.maxY - origin.getY(), chosen.maxZ - origin.getZ());
		double[] u = axisBounds(relativeMin, relativeMax, right);
		double[] v = axisBounds(relativeMin, relativeMax, up);
		return new double[] { u[0], u[1], v[0], v[1] };
	}

	private static boolean liesOnFace(AABB box, Vec3 point, Direction normal) {
		final double epsilon = 1.0E-5;
		return switch (normal) {
			case EAST -> Math.abs(point.x - box.maxX) < epsilon && between(point.y, box.minY, box.maxY, epsilon)
					&& between(point.z, box.minZ, box.maxZ, epsilon);
			case WEST -> Math.abs(point.x - box.minX) < epsilon && between(point.y, box.minY, box.maxY, epsilon)
					&& between(point.z, box.minZ, box.maxZ, epsilon);
			case UP -> Math.abs(point.y - box.maxY) < epsilon && between(point.x, box.minX, box.maxX, epsilon)
					&& between(point.z, box.minZ, box.maxZ, epsilon);
			case DOWN -> Math.abs(point.y - box.minY) < epsilon && between(point.x, box.minX, box.maxX, epsilon)
					&& between(point.z, box.minZ, box.maxZ, epsilon);
			case SOUTH -> Math.abs(point.z - box.maxZ) < epsilon && between(point.x, box.minX, box.maxX, epsilon)
					&& between(point.y, box.minY, box.maxY, epsilon);
			case NORTH -> Math.abs(point.z - box.minZ) < epsilon && between(point.x, box.minX, box.maxX, epsilon)
					&& between(point.y, box.minY, box.maxY, epsilon);
		};
	}

	private static boolean between(double value, double min, double max, double epsilon) {
		return value >= min - epsilon && value <= max + epsilon;
	}

	private static double tangentArea(AABB box, Direction normal) {
		return switch (normal.getAxis()) {
			case X -> (box.maxY - box.minY) * (box.maxZ - box.minZ);
			case Y -> (box.maxX - box.minX) * (box.maxZ - box.minZ);
			case Z -> (box.maxX - box.minX) * (box.maxY - box.minY);
		};
	}

	private static double[] axisBounds(Vec3 min, Vec3 max, float[] axis) {
		double a = min.x * axis[0] + min.y * axis[1] + min.z * axis[2];
		double b = max.x * axis[0] + max.y * axis[1] + max.z * axis[2];
		return new double[] { Math.min(a, b), Math.max(a, b) };
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
				emitQuad(consumer, pose, cx, cy, cz, right, up, left, rightEdge, bottom, top, 0.92F);
			}
		});
	}

	private static void emitLightSpot(VertexConsumer consumer, PoseStack.Pose pose,
			CrystalLampRenderState.LightHit hit) {
		float[] right = right(hit.normal());
		float[] up = up(hit.normal());
		float centerU = hit.x() * right[0] + hit.y() * right[1] + hit.z() * right[2];
		float centerV = hit.x() * up[0] + hit.y() * up[1] + hit.z() * up[2];
		float half = hit.size() * 0.5F;
		float left = Math.max(centerU - half, hit.minU());
		float rightEdge = Math.min(centerU + half, hit.maxU());
		float bottom = Math.max(centerV - half, hit.minV());
		float top = Math.min(centerV + half, hit.maxV());
		if (rightEdge - left < 0.002F || top - bottom < 0.002F) {
			return;
		}
		emitQuad(consumer, pose, hit.x(), hit.y(), hit.z(), right, up,
				left - centerU, rightEdge - centerU, bottom - centerV, top - centerV, 0.72F);
	}

	private static void emitQuad(VertexConsumer consumer, PoseStack.Pose pose, float cx, float cy, float cz,
			float[] right, float[] up, float left, float rightEdge, float bottom, float top, float alpha) {
		vertex(consumer, pose, point(cx, cy, cz, right, up, left, top), alpha);
		vertex(consumer, pose, point(cx, cy, cz, right, up, left, bottom), alpha);
		vertex(consumer, pose, point(cx, cy, cz, right, up, rightEdge, bottom), alpha);
		vertex(consumer, pose, point(cx, cy, cz, right, up, rightEdge, top), alpha);
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
		return new float[] {
				cx + right[0] * horizontal + up[0] * vertical,
				cy + right[1] * horizontal + up[1] * vertical,
				cz + right[2] * horizontal + up[2] * vertical
		};
	}

	private static float[] right(Direction normal) {
		return switch (normal) {
			case NORTH -> new float[] { 1, 0, 0 };
			case EAST -> new float[] { 0, 0, -1 };
			case SOUTH -> new float[] { -1, 0, 0 };
			case WEST -> new float[] { 0, 0, 1 };
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

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float[] point, float alpha) {
		consumer.addVertex(pose.pose(), point[0], point[1], point[2]).setColor(1.0F, 0.91F, 0.55F, alpha);
	}

	private record ProjectionCache(CrystalLampData data, long tick, List<CrystalLampRenderState.LightHit> hits) {
	}
}
