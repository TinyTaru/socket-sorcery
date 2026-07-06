package tinytaru.socketsorcery.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * Base for {@code builtin/entity} item renderers that composite a 16x16 icon at runtime, cache it as
 * a {@link DynamicTexture} keyed by appearance, and draw it as an extruded icon — a front and back
 * face separated by {@link #THICKNESS}, plus a thin side face wherever an opaque pixel borders a
 * transparent one, matching the pseudo-3D depth vanilla's {@code item/generated} parent bakes for
 * flat sprites. Everything is drawn with {@code entityCutoutNoCull} and each face carries an outward
 * normal, so depth-testing shows the nearer (always-camera-facing, always-bright) face and the icon
 * looks correct from any side — the inventory GUI and the held / dropped / item-frame views alike.
 * Subclasses supply the per-stack texture (typically via {@link #composeCached}); this class owns the
 * geometry, the texture cache, and reload cleanup.
 */
public abstract class DynamicIconRenderer
		implements BuiltinItemRendererRegistry.DynamicItemRenderer, SimpleSynchronousResourceReloadListener {

	protected static final int SIZE = 16;

	/** Extrusion depth, in blocks — matches vanilla's 1-texel-deep flat item extrusion (1/16). */
	private static final float THICKNESS = 1.0F / SIZE;
	private static final float FRONT_Z = 0.5F - THICKNESS / 2.0F;
	private static final float BACK_Z = 0.5F + THICKNESS / 2.0F;

	private final String idPrefix;
	private final ResourceLocation fabricId;
	private final Map<String, ResourceLocation> cache = new HashMap<>();
	private final Map<ResourceLocation, boolean[][]> opacityCache = new HashMap<>();
	private int counter;

	protected DynamicIconRenderer(String idPrefix) {
		this.idPrefix = idPrefix;
		this.fabricId = SocketSorcery.id(idPrefix);
	}

	/** The texture to draw for the stack, or null to skip. Usually built via {@link #composeCached}. */
	protected abstract ResourceLocation texture(ItemStack stack);

	/** Hook for subclasses to clear their own caches on resource reload. */
	protected void onReload() {
	}

	@Override
	public final void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
			MultiBufferSource buffers, int light, int overlay) {
		ResourceLocation texture = texture(stack);
		if (texture == null) {
			return;
		}
		PoseStack.Pose pose = poseStack.last();
		VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
		quad(consumer, pose, light, overlay, false);
		quad(consumer, pose, light, overlay, true);
		boolean[][] opaque = opacityCache.get(texture);
		if (opaque != null) {
			sides(consumer, pose, light, overlay, opaque);
		}
	}

	/** Returns a cached dynamic texture for {@code key}, building it once; null if the builder yields null. */
	protected final ResourceLocation composeCached(String key, Supplier<NativeImage> builder) {
		ResourceLocation cached = cache.get(key);
		if (cached != null) {
			return cached;
		}
		NativeImage image = builder.get();
		if (image == null) {
			return null;
		}
		ResourceLocation id = SocketSorcery.id(idPrefix + "_" + (counter++));
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
		cache.put(key, id);
		opacityCache.put(id, opacityOf(image));
		return id;
	}

	/** Per-pixel opacity (alpha &gt; 16) of a {@code SIZE x SIZE} composed image, used to find silhouette edges. */
	private static boolean[][] opacityOf(NativeImage image) {
		boolean[][] opaque = new boolean[SIZE][SIZE];
		for (int row = 0; row < SIZE && row < image.getHeight(); row++) {
			for (int col = 0; col < SIZE && col < image.getWidth(); col++) {
				opaque[row][col] = ((image.getPixelRGBA(col, row) >>> 24) & 0xFF) > 16;
			}
		}
		return opaque;
	}

	@Override
	public final ResourceLocation getFabricId() {
		return fabricId;
	}

	@Override
	public final void onResourceManagerReload(ResourceManager resourceManager) {
		for (ResourceLocation id : cache.values()) {
			Minecraft.getInstance().getTextureManager().release(id);
		}
		cache.clear();
		opacityCache.clear();
		counter = 0;
		onReload();
	}

	/** The {@code textures/item/<path>.png} location for an item id. */
	protected static ResourceLocation itemTexture(ResourceLocation itemId) {
		return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "textures/item/" + itemId.getPath() + ".png");
	}

	/** Loads a PNG into a pixel buffer (NativeImage ABGR), or null if missing/unreadable. */
	protected static Pixels loadPixels(ResourceLocation textureFile) {
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
					data[y * width + x] = image.getPixelRGBA(x, y);
				}
			}
			return new Pixels(data, width, height);
		} catch (IOException e) {
			return null;
		}
	}

	/** A loaded image as a flat ABGR pixel array. */
	protected record Pixels(int[] data, int width, int height) {
		public int get(int x, int y) {
			return data[y * width + x];
		}

		public int alpha(int x, int y) {
			return (get(x, y) >>> 24) & 0xFF;
		}

		/** A copy cropped to the opaque bounding box, or null if fully transparent. */
		public Pixels cropToOpaque() {
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
			if (maxX < 0) {
				return null;
			}
			int w = maxX - minX + 1;
			int h = maxY - minY + 1;
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
		float z = back ? BACK_Z : FRONT_Z;
		float n = back ? 1.0F : -1.0F; // outward
		vertex(vc, pose, 0, 0, z, 0, 1, light, overlay, 0, 0, n);
		vertex(vc, pose, 1, 0, z, 1, 1, light, overlay, 0, 0, n);
		vertex(vc, pose, 1, 1, z, 1, 0, light, overlay, 0, 0, n);
		vertex(vc, pose, 0, 1, z, 0, 0, light, overlay, 0, 0, n);
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

	/** Emits the quad once; {@code entityCutoutNoCull} (see {@link #quad}) makes winding irrelevant to visibility. */
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
