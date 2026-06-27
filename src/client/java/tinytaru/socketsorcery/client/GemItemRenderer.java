package tinytaru.socketsorcery.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
 * Unengraved gems keep their normal {@code item/generated} model; only engraved stacks (whose model
 * is {@code builtin/entity}) reach this renderer.
 */
public class GemItemRenderer extends DynamicIconRenderer {

	private static final GemItemRenderer INSTANCE = new GemItemRenderer();

	private final Map<Item, Pixels> baseCache = new HashMap<>();

	private GemItemRenderer() {
		super("gem_icon");
	}

	public static void register() {
		for (Item gem : ModItems.GEMS) {
			BuiltinItemRendererRegistry.INSTANCE.register(gem, INSTANCE);
		}
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(INSTANCE);
	}

	@Override
	protected ResourceLocation texture(ItemStack stack) {
		Item item = stack.getItem();
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
		EngravingData data = stack.get(ModComponents.ENGRAVING);
		Pattern pattern = data == null ? null : Patterns.get(data.pattern());
		if (pattern == null) {
			return itemTexture(itemId); // not engraved / unknown: just the plain gem
		}
		String key = itemId + "|" + data.pattern() + "|" + Modifiers.ordered(data.modifiers());
		ResourceLocation built = composeCached(key, () -> compose(item, itemId, pattern, data));
		return built != null ? built : itemTexture(itemId);
	}

	private NativeImage compose(Item item, ResourceLocation itemId, Pattern pattern, EngravingData data) {
		Pixels base = baseCache.computeIfAbsent(item, k -> loadPixels(itemTexture(itemId)));
		if (base == null || base.width() != SIZE || base.height() != SIZE) {
			return null;
		}
		boolean[][] symbol = pattern.mask();
		long[] deep = Modifiers.cellsFor(pattern, data.modifiers());
		int bright = brightAbgr(pattern.color());
		int dark = darkAbgr(pattern.color());

		NativeImage image = new NativeImage(SIZE, SIZE, false);
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				int pixel = base.get(col, row);
				if (((pixel >>> 24) & 0xFF) <= 16) {
					image.setPixelRGBA(col, row, 0);
				} else if (GridBits.get(deep, row, col)) {
					image.setPixelRGBA(col, row, dark);
				} else if (symbol[row][col]) {
					image.setPixelRGBA(col, row, bright);
				} else {
					image.setPixelRGBA(col, row, pixel);
				}
			}
		}
		return image;
	}

	@Override
	protected void onReload() {
		baseCache.clear();
	}

	private static int brightAbgr(int rgb) {
		int r = (int) (((rgb >> 16) & 0xFF) * 0.8 + 255 * 0.2);
		int g = (int) (((rgb >> 8) & 0xFF) * 0.8 + 255 * 0.2);
		int b = (int) ((rgb & 0xFF) * 0.8 + 255 * 0.2);
		return (0xFF << 24) | (b << 16) | (g << 8) | r;
	}

	private static int darkAbgr(int rgb) {
		int r = (int) (((rgb >> 16) & 0xFF) * 0.45);
		int g = (int) (((rgb >> 8) & 0xFF) * 0.45);
		int b = (int) ((rgb & 0xFF) * 0.45);
		return (0xFF << 24) | (b << 16) | (g << 8) | r;
	}
}
