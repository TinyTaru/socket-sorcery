package tinytaru.socketsorcery.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener.SharedState;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * Base for special item model renderers that composite a 16x16 icon at runtime, cache it as a
 * {@link DynamicTexture} keyed by appearance, and draw it as an extruded icon — a front and back
 * face separated by {@link #THICKNESS}, plus a thin side face wherever an opaque pixel borders a
 * transparent one, matching the pseudo-3D depth vanilla's {@code item/generated} parent bakes for
 * flat sprites. Everything is drawn with {@code entityCutout} (which is the no-cull variant now) and
 * each face carries an outward normal, so depth-testing shows the nearer (always-camera-facing,
 * always-bright) face and the icon looks correct from any side — the inventory GUI and the held /
 * dropped / item-frame views alike. Subclasses supply the per-stack texture (typically via
 * {@link #composeCached}); this class owns the geometry, the texture cache, and reload cleanup.
 *
 * <p>Rendering is submit-based now: geometry goes through
 * {@link SubmitNodeCollector#submitCustomGeometry}, whose callback hands back the same
 * {@code (Pose, VertexConsumer)} pair the old immediate-mode renderer wrote into, so the extrusion
 * below is unchanged. The per-stack lookup moved into {@link #extractArgument}, which runs before
 * submission and yields the {@link Icon} that {@link #submit} draws.
 */
public abstract class DynamicIconRenderer extends SimpleReloadListener<Void>
		implements SpecialModelRenderer<DynamicIconRenderer.Icon> {

	protected static final int SIZE = 16;

	/** Extrusion depth, in blocks — matches vanilla's 1-texel-deep flat item extrusion (1/16). */
	private static final float THICKNESS = 1.0F / SIZE;
	private static final float FRONT_Z = 0.5F - THICKNESS / 2.0F;
	private static final float BACK_Z = 0.5F + THICKNESS / 2.0F;
	private static final RenderPipeline CLOSE_UP_PIPELINE = RenderPipelines.register(RenderPipeline.builder(
			RenderPipelines.MATRICES_PROJECTION_SNIPPET)
			.withLocation(SocketSorcery.id("pipeline/scroll_close_up"))
			.withVertexShader(SocketSorcery.id("core/scroll_close_up"))
			.withFragmentShader(SocketSorcery.id("core/scroll_close_up"))
			.withSampler("Sampler0")
			.withSampler("Sampler2")
			.withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
			.withDepthStencilState(DepthStencilState.DEFAULT)
			.withCull(false)
			.build());
	private static final RenderPipeline SHINE_PIPELINE = RenderPipelines.register(RenderPipeline.builder(
			RenderPipelines.MATRICES_PROJECTION_SNIPPET)
			.withLocation(SocketSorcery.id("pipeline/scroll_shine"))
			.withVertexShader(SocketSorcery.id("core/scroll_shine"))
			.withFragmentShader(SocketSorcery.id("core/scroll_shine"))
			.withSampler("Sampler0")
			.withUniform("Globals", UniformType.UNIFORM_BUFFER)
			.withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
			.withCull(false)
			.build());

	/** What a single stack resolves to: the composited texture plus its silhouette mask. */
	public record Icon(Identifier texture, boolean[][] opaque, Identifier backTexture) {
	}

	private final String idPrefix;
	private final Identifier fabricId;
	private final Map<String, Identifier> cache = new HashMap<>();
	private final Map<Identifier, boolean[][]> opacityCache = new HashMap<>();
	private final Map<Identifier, RenderType> closeUpTypes = new HashMap<>();
	private final Map<Identifier, RenderType> shineTypes = new HashMap<>();
	private int counter;

	protected DynamicIconRenderer(String idPrefix) {
		this.idPrefix = idPrefix;
		this.fabricId = SocketSorcery.id(idPrefix);
	}

	/** The texture to draw for the stack, or null to skip. Usually built via {@link #composeCached}. */
	protected abstract Identifier texture(ItemStack stack);

	/** Optional texture for the reverse face; null keeps the front texture on both faces. */
	protected Identifier backTexture(ItemStack stack) {
		return null;
	}

	/** Render layer used by the regular item-model path. Subclasses may opt into an item-specific layer. */
	protected RenderType renderType(Identifier texture) {
		return RenderTypes.entityCutout(texture);
	}

	/** Hook for subclasses to clear their own caches on resource reload. */
	protected void onReload() {
	}

	@Override
	public final Icon extractArgument(ItemStack stack) {
		Identifier texture = texture(stack);
		if (texture == null) return null;
		Identifier reverse = backTexture(stack);
		if (reverse == null) reverse = texture;
		boolean[][] opaque = opacityCache.get(texture);
		if (opaque == null) opaque = opacityCache.get(reverse);
		return new Icon(texture, opaque, reverse);
	}

	@Override
	public final void submit(Icon icon, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay,
			boolean hasFoil, int outlineColor) {
		if (icon == null) {
			return;
		}
		RenderType backType = renderType(icon.backTexture());
		RenderType frontType = renderType(icon.texture());
		collector.submitCustomGeometry(poseStack, backType,
				(pose, consumer) -> quad(consumer, pose, light, overlay, true));
		collector.submitCustomGeometry(poseStack, frontType, (pose, consumer) -> {
			quad(consumer, pose, light, overlay, false);
			if (icon.opaque() != null) sides(consumer, pose, light, overlay, icon.opaque());
		});
		if (hasFoil) {
			collector.submitCustomGeometry(poseStack, RenderTypes.entityGlint(),
					(pose, consumer) -> quad(consumer, pose, light, overlay, false));
		}
	}

	/**
	 * Draws the transcription close-up with an opaque, depth-writing pipeline that samples the texture
	 * directly. It preserves the current world light level but avoids entity fog and directional
	 * colour shifts, so the parchment remains faithful to its texture while naturally darkening at night.
	 */
	protected final void submitCloseUp(Icon icon, PoseStack poseStack, SubmitNodeCollector collector, int light,
			boolean hasFoil) {
		if (icon == null) return;
		collector.submitCustomGeometry(poseStack, closeUpType(icon.backTexture()),
				(pose, consumer) -> quad(consumer, pose, light, 0, true));
		collector.submitCustomGeometry(poseStack, closeUpType(icon.texture()), (pose, consumer) -> {
			quad(consumer, pose, light, 0, false);
			if (icon.opaque() != null) sides(consumer, pose, light, 0, icon.opaque());
		});
		if (hasFoil) {
			collector.submitCustomGeometry(poseStack, shineType(icon.backTexture()),
					(pose, consumer) -> shineQuad(consumer, pose, light, true));
			collector.submitCustomGeometry(poseStack, shineType(icon.texture()),
					(pose, consumer) -> shineQuad(consumer, pose, light, false));
		}
	}

	private RenderType shineType(Identifier texture) {
		return shineTypes.computeIfAbsent(texture, id -> RenderType.create(
				"socket_sorcery_scroll_shine",
				RenderSetup.builder(SHINE_PIPELINE).withTexture("Sampler0", id).createRenderSetup()));
	}

	private RenderType closeUpType(Identifier texture) {
		return closeUpTypes.computeIfAbsent(texture, id -> RenderType.create(
				"socket_sorcery_scroll_close_up",
				RenderSetup.builder(CLOSE_UP_PIPELINE).withTexture("Sampler0", id).useLightmap().createRenderSetup()));
	}

	/** The corners of the extruded slab, for view culling. */
	@Override
	public final void getExtents(Consumer<Vector3fc> consumer) {
		for (float z : new float[] { FRONT_Z, BACK_Z }) {
			consumer.accept(new Vector3f(0.0F, 0.0F, z));
			consumer.accept(new Vector3f(1.0F, 0.0F, z));
			consumer.accept(new Vector3f(0.0F, 1.0F, z));
			consumer.accept(new Vector3f(1.0F, 1.0F, z));
		}
	}

	/** Returns a cached dynamic texture for {@code key}, building it once; null if the builder yields null. */
	protected final Identifier composeCached(String key, Supplier<NativeImage> builder) {
		Identifier cached = cache.get(key);
		if (cached != null) {
			return cached;
		}
		NativeImage image = builder.get();
		if (image == null) {
			return null;
		}
		Identifier id = SocketSorcery.id(idPrefix + "_" + (counter++));
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
		cache.put(key, id);
		opacityCache.put(id, opacityOf(image));
		return id;
	}

	/** Per-pixel opacity (alpha &gt; 16) of a {@code SIZE x SIZE} composed image, used to find silhouette edges. */
	private static boolean[][] opacityOf(NativeImage image) {
		boolean[][] opaque = new boolean[SIZE][SIZE];
		for (int row = 0; row < SIZE && row < image.getHeight(); row++) {
			for (int col = 0; col < SIZE && col < image.getWidth(); col++) {
				opaque[row][col] = ((image.getPixel(col, row) >>> 24) & 0xFF) > 16;
			}
		}
		return opaque;
	}

	public final Identifier getFabricId() {
		return fabricId;
	}

	@Override
	protected final Void prepare(SharedState sharedState) {
		return null;
	}

	@Override
	protected final void apply(Void ignored, SharedState sharedState) {
		for (Identifier id : cache.values()) {
			Minecraft.getInstance().getTextureManager().release(id);
		}
		cache.clear();
		opacityCache.clear();
		closeUpTypes.clear();
		shineTypes.clear();
		counter = 0;
		onReload();
	}

	/** The {@code textures/item/<path>.png} location for an item id. */
	protected static Identifier itemTexture(Identifier itemId) {
		return Identifier.fromNamespaceAndPath(itemId.getNamespace(), "textures/item/" + itemId.getPath() + ".png");
	}

	/** Loads a PNG into a pixel buffer (ARGB), or null if missing/unreadable. */
	protected static Pixels loadPixels(Identifier textureFile) {
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(textureFile);
		if (resource.isEmpty()) {
			return null;
		}
		try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
			int width = image.getWidth();
			int height = image.getHeight();
			int[] data = new int[width * height];
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					data[y * width + x] = image.getPixel(x, y);
				}
			}
			return new Pixels(data, width, height);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * A loaded image as a flat <b>ARGB</b> pixel array. NativeImage's public accessors are ARGB now
	 * ({@code getPixelABGR} went private), so the composited colours are packed ARGB throughout.
	 */
	protected record Pixels(int[] data, int width, int height) {
		public int get(int x, int y) {
			return data[y * width + x];
		}

		public int alpha(int x, int y) {
			return (get(x, y) >>> 24) & 0xFF;
		}

		/**
		 * The opaque bounding box as {@code {minX, minY, maxX, maxY}} (inclusive), or null if the
		 * image is fully transparent. Callers that want the pixels use {@link #cropToOpaque()};
		 * callers that want to address the region in the original image (UVs, say) use this.
		 */
		public int[] opaqueBounds() {
			int minX = width, minY = height, maxX = -1, maxY = -1;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (alpha(x, y) > 16) {
						minX = Math.min(minX, x);
						minY = Math.min(minY, y);
						maxX = Math.max(maxX, x);
						maxY = Math.max(maxY, y);
					}
				}
			}
			return maxX < 0 ? null : new int[] { minX, minY, maxX, maxY };
		}

		/** A copy cropped to the opaque bounding box, or null if fully transparent. */
		public Pixels cropToOpaque() {
			int[] bounds = opaqueBounds();
			if (bounds == null) {
				return null;
			}
			int minX = bounds[0];
			int minY = bounds[1];
			int w = bounds[2] - minX + 1;
			int h = bounds[3] - minY + 1;
			int[] out = new int[w * h];
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					out[y * w + x] = get(minX + x, minY + y);
				}
			}
			return new Pixels(out, w, h);
		}
	}

	/**
	 * The front or back face, each with an <em>outward</em> normal (front at the smaller z points −Z,
	 * back at the larger z points +Z — both away from the slab centre). With no culling, both faces
	 * always draw and depth-testing keeps the nearer one on top; because that nearer face's outward
	 * normal always points toward whatever camera is viewing, it is always lit at full brightness. This
	 * is camera-direction-agnostic, so it looks right in the inventory GUI and the held / dropped /
	 * item-frame views alike (which view the model from opposite sides via their display transforms).
	 */
	private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, boolean back) {
		quad(vc, pose, light, overlay, back, 0.0F);
	}

	private static void shineQuad(VertexConsumer vc, PoseStack.Pose pose, int light, boolean back) {
		quad(vc, pose, light, 0, back, -0.002F);
	}

	private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, boolean back,
			float zOffset) {
		float z = (back ? BACK_Z : FRONT_Z) + zOffset;
		float n = back ? 1.0F : -1.0F; // outward
		if (back) {
			vertex(vc, pose, 0, 0, z, 0, 1, light, overlay, 0, 0, n);
			vertex(vc, pose, 1, 0, z, 1, 1, light, overlay, 0, 0, n);
			vertex(vc, pose, 1, 1, z, 1, 0, light, overlay, 0, 0, n);
			vertex(vc, pose, 0, 1, z, 0, 0, light, overlay, 0, 0, n);
		} else {
			// The front points toward -Z. Its winding must agree so item-cutout's face culling keeps
			// it when the table renderer rotates it upward.
			vertex(vc, pose, 0, 1, z, 0, 0, light, overlay, 0, 0, n);
			vertex(vc, pose, 1, 1, z, 1, 0, light, overlay, 0, 0, n);
			vertex(vc, pose, 1, 0, z, 1, 1, light, overlay, 0, 0, n);
			vertex(vc, pose, 0, 0, z, 0, 1, light, overlay, 0, 0, n);
		}
	}

	/**
	 * Emits a thin side face at every silhouette edge — an opaque cell bordering a transparent one (or
	 * the grid edge) — spanning {@link #FRONT_Z}..{@link #BACK_Z}. Each side face samples a single
	 * texel (its own pixel's centre UV) so it renders as that pixel's solid colour, giving the icon the
	 * same 1-texel-deep extruded silhouette vanilla bakes for flat {@code item/generated} sprites.
	 */
	private static void sides(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, boolean[][] opaque) {
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (!opaque[row][col]) {
					continue;
				}
				float x0 = col / (float) SIZE;
				float x1 = (col + 1) / (float) SIZE;
				float yTop = 1.0F - row / (float) SIZE;
				float yBot = 1.0F - (row + 1) / (float) SIZE;
				float u = (col + 0.5F) / SIZE;
				float v = (row + 0.5F) / SIZE;

				if (row == 0 || !opaque[row - 1][col]) { // top edge exposed
					sideQuad(vc, pose, light, overlay, u, v,
							x0, yTop, FRONT_Z, x0, yTop, BACK_Z, x1, yTop, BACK_Z, x1, yTop, FRONT_Z, 0, 1, 0);
				}
				if (row == SIZE - 1 || !opaque[row + 1][col]) { // bottom edge exposed
					sideQuad(vc, pose, light, overlay, u, v,
							x0, yBot, FRONT_Z, x1, yBot, FRONT_Z, x1, yBot, BACK_Z, x0, yBot, BACK_Z, 0, -1, 0);
				}
				if (col == 0 || !opaque[row][col - 1]) { // left edge exposed
					sideQuad(vc, pose, light, overlay, u, v,
							x0, yBot, FRONT_Z, x0, yBot, BACK_Z, x0, yTop, BACK_Z, x0, yTop, FRONT_Z, -1, 0, 0);
				}
				if (col == SIZE - 1 || !opaque[row][col + 1]) { // right edge exposed
					sideQuad(vc, pose, light, overlay, u, v,
							x1, yBot, FRONT_Z, x1, yTop, FRONT_Z, x1, yTop, BACK_Z, x1, yBot, BACK_Z, 1, 0, 0);
				}
			}
		}
	}

	/** Emits the quad once; {@code entityCutout} (see {@link #quad}) makes winding irrelevant to visibility. */
	private static void sideQuad(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, float u, float v,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3,
			float nx, float ny, float nz) {
		vertex(vc, pose, x0, y0, z0, u, v, light, overlay, nx, ny, nz);
		vertex(vc, pose, x1, y1, z1, u, v, light, overlay, nx, ny, nz);
		vertex(vc, pose, x2, y2, z2, u, v, light, overlay, nx, ny, nz);
		vertex(vc, pose, x3, y3, z3, u, v, light, overlay, nx, ny, nz);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
			float u, float v, int light, int overlay, float nx, float ny, float nz) {
		vc.addVertex(pose, x, y, z)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setOverlay(overlay)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);
	}
}
