package tinytaru.socketsorcery.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
 *
 * <p>Pattern data comes from the synced dynamic registry via the client level (null-guarded: with no
 * level, the plain gem texture renders). Other mods can opt their own items into this renderer via
 * {@code SocketSorceryClientApi#registerEngravableGem}.
 */
public class GemItemRenderer extends DynamicIconRenderer {

	private static final GemItemRenderer INSTANCE = new GemItemRenderer();

	private final Map<Item, Pixels> baseCache = new HashMap<>();

	private GemItemRenderer() {
		super("gem_icon");
	}

	public static void register() {
		for (Item gem : ModItems.GEMS) {
			registerFor(gem);
		}
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(INSTANCE);
	}

	/** Routes the given item's {@code builtin/entity} model through this renderer. */
	public static void registerFor(Item item) {
		BuiltinItemRendererRegistry.INSTANCE.register(item, INSTANCE);
	}

	private static HolderLookup.Provider clientRegistries() {
		return Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.registryAccess();
	}

	@Override
	protected Identifier texture(ItemStack stack) {
		Item item = stack.getItem();
		Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
		EngravingData data = stack.get(ModComponents.ENGRAVING);
		Holder.Reference<Pattern> pattern = data == null ? null : Patterns.get(clientRegistries(), data.pattern());
		if (pattern == null) {
			return itemTexture(itemId); // not engraved / unknown pattern / no level: just the plain gem
		}
		String key = itemId + "|" + data.pattern() + "|" + Modifiers.ordered(data.modifiers());
		Identifier built = composeCached(key, () -> compose(item, itemId, pattern.value(), data));
		return built != null ? built : itemTexture(itemId);
	}

	private NativeImage compose(Item item, Identifier itemId, Pattern pattern, EngravingData data) {
		Pixels base = baseCache.computeIfAbsent(item, k -> loadPixels(itemTexture(itemId)));
		if (base == null || base.width() != SIZE || base.height() != SIZE) {
			return null;
		}
		boolean[][] symbol = pattern.mask();
		long[] deep = Modifiers.cellsFor(clientRegistries(), pattern, data.modifiers());
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
