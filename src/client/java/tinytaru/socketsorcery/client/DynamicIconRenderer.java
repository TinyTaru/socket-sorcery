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
 * a {@link DynamicTexture} keyed by appearance, and draw it as a flat quad. Subclasses supply the
 * per-stack texture (typically via {@link #composeCached}); this class owns the quad drawing, the
 * texture cache, and reload cleanup.
 */
public abstract class DynamicIconRenderer
		implements BuiltinItemRendererRegistry.DynamicItemRenderer, SimpleSynchronousResourceReloadListener {

	protected static final int SIZE = 16;

	private final String idPrefix;
	private final ResourceLocation fabricId;
	private final Map<String, ResourceLocation> cache = new HashMap<>();
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
		VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(texture));
		quad(consumer, pose, light, overlay, false);
		quad(consumer, pose, light, overlay, true);
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
		return id;
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

	private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, boolean back) {
		float z = 0.5F;
		float n = back ? -1.0F : 1.0F;
		if (back) {
			vertex(vc, pose, 0, 0, z, 0, 1, light, overlay, n);
			vertex(vc, pose, 0, 1, z, 0, 0, light, overlay, n);
			vertex(vc, pose, 1, 1, z, 1, 0, light, overlay, n);
			vertex(vc, pose, 1, 0, z, 1, 1, light, overlay, n);
		} else {
			vertex(vc, pose, 0, 0, z, 0, 1, light, overlay, n);
			vertex(vc, pose, 1, 0, z, 1, 1, light, overlay, n);
			vertex(vc, pose, 1, 1, z, 1, 0, light, overlay, n);
			vertex(vc, pose, 0, 1, z, 0, 0, light, overlay, n);
		}
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
			float u, float v, int light, int overlay, float nz) {
		vc.addVertex(pose, x, y, z)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setOverlay(overlay)
				.setLight(light)
				.setNormal(pose, 0.0F, 0.0F, nz);
	}
}
