package tinytaru.socketsorcery.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.component.CarvingData;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.pattern.GridBits;
import tinytaru.socketsorcery.pattern.Modifiers;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * Renders engraved gems by compositing their icon at runtime: base gem texture + the pattern symbol
 * (bright) + the modifier cells (darker), for the exact pattern and modifier set on the stack.
 * Uncut gems keep their plain model; stacks carrying either a finished engraving or an in-progress
 * carve reach this renderer, selected by the {@code minecraft:has_component} conditions in each
 * gem's item model definition.
 *
 * <p>Pattern data comes from the synced dynamic registry via the client level (null-guarded: with no
 * level, the plain gem texture renders). Other mods opt in by pointing their own item model
 * definition at {@code socket-sorcery:engraved_gem} — no Java call needed.
 */
public class GemItemRenderer extends DynamicIconRenderer {

	/** The special model type id gem item model definitions reference. */
	public static final Identifier ID = SocketSorcery.id("engraved_gem");

	private static final GemItemRenderer INSTANCE = new GemItemRenderer();

	private final Map<Item, Pixels> baseCache = new HashMap<>();

	private GemItemRenderer() {
		super("gem_icon");
	}

	/**
	 * The unbaked form referenced from item model JSON. It carries no data, so its codec is a unit and
	 * baking just hands back the shared instance (which owns the texture cache).
	 */
	public record Unbaked() implements SpecialModelRenderer.Unbaked<Icon> {

		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

		@Override
		public SpecialModelRenderer<Icon> bake(SpecialModelRenderer.BakingContext context) {
			return INSTANCE;
		}

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}
	}

	public static void register() {
		// Which items use this renderer is declared by their item model definitions, not here.
		SpecialModelRenderers.ID_MAPPER.put(ID, Unbaked.MAP_CODEC);
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(INSTANCE.getFabricId(), INSTANCE);
	}

	/**
	 * No-op. Base textures load lazily on first render — eagerly touching the resource manager here
	 * would run before it exists. Kept only for the deprecated client API shim.
	 */
	public static void registerFor(Item item) {
	}

	private static HolderLookup.Provider clientRegistries() {
		return Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.registryAccess();
	}

	@Override
	protected Identifier texture(ItemStack stack) {
		Item item = stack.getItem();
		Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
		EngravingData data = stack.get(ModComponents.ENGRAVING);
		CarvingData carving = stack.get(ModComponents.CARVING);
		Identifier patternId = data != null ? data.pattern() : carving == null ? null : carving.pattern();
		Holder.Reference<Pattern> pattern = patternId == null ? null : Patterns.get(clientRegistries(), patternId);
		if (pattern == null) {
			return itemTexture(itemId); // uncut / unknown pattern / no level: just the plain gem
		}
		String appearance = data != null ? Modifiers.ordered(data.modifiers()).toString()
				: Arrays.hashCode(carving.carved()) + ":" + Arrays.hashCode(carving.deep());
		String key = itemId + "|" + patternId + "|" + appearance;
		Identifier built = composeCached(key, () -> compose(item, itemId, pattern.value(), data, carving));
		return built != null ? built : itemTexture(itemId);
	}

	/** Use the same light-responsive item layer as the generated chisel model. */
	@Override
	protected RenderType renderType(Identifier texture) {
		return RenderTypes.itemCutout(texture);
	}

	private NativeImage compose(Item item, Identifier itemId, Pattern pattern, EngravingData data, CarvingData carving) {
		Pixels base = baseCache.computeIfAbsent(item, k -> loadPixels(itemTexture(itemId)));
		if (base == null || base.width() != SIZE || base.height() != SIZE) {
			return null;
		}
		long[] carved = carving == null ? pattern.maskBits() : carving.carved();
		long[] deep = carving == null ? Modifiers.cellsFor(clientRegistries(), pattern, data.modifiers()) : carving.deep();
		int bright = brightArgb(pattern.color());
		int dark = darkArgb(pattern.color());

		NativeImage image = new NativeImage(SIZE, SIZE, false);
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				int pixel = base.get(col, row);
				if (((pixel >>> 24) & 0xFF) <= 16) {
					image.setPixel(col, row, 0);
				} else if (GridBits.get(deep, row, col)) {
					image.setPixel(col, row, dark);
				} else if (GridBits.get(carved, row, col)) {
					image.setPixel(col, row, bright);
				} else {
					image.setPixel(col, row, liftBaseArgb(pixel));
				}
			}
		}
		return image;
	}

	@Override
	protected void onReload() {
		baseCache.clear();
	}

	// NativeImage's public accessors are ARGB now, so these pack ARGB (they used to pack ABGR).

	private static int brightArgb(int rgb) {
		int r = (int) (((rgb >> 16) & 0xFF) * 0.8 + 255 * 0.2);
		int g = (int) (((rgb >> 8) & 0xFF) * 0.8 + 255 * 0.2);
		int b = (int) ((rgb & 0xFF) * 0.8 + 255 * 0.2);
		return (0xFF << 24) | (r << 16) | (g << 8) | b;
	}

	private static int darkArgb(int rgb) {
		int r = (int) (((rgb >> 16) & 0xFF) * 0.45);
		int g = (int) (((rgb >> 8) & 0xFF) * 0.45);
		int b = (int) ((rgb & 0xFF) * 0.45);
		return (0xFF << 24) | (r << 16) | (g << 8) | b;
	}

	/**
	 * The generated gem sprites are deliberately very contrasty. Lift only their uncarved pixels a
	 * little when composing the physical engraving so their facets retain detail under world shading;
	 * carved and deep cells keep their existing, intentionally stronger colours.
	 */
	private static int liftBaseArgb(int argb) {
		int alpha = (argb >>> 24) & 0xFF;
		int r = liftChannel((argb >> 16) & 0xFF);
		int g = liftChannel((argb >> 8) & 0xFF);
		int b = liftChannel(argb & 0xFF);
		return (alpha << 24) | (r << 16) | (g << 8) | b;
	}

	/** A restrained 8% lift toward white keeps dark gem hues rich rather than making them emissive. */
	private static int liftChannel(int value) {
		return value + Math.round((255 - value) * 0.08F);
	}
}
