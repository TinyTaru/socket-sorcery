package tinytaru.socketsorcery.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.item.AccessoryItem;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * Renders necklaces and bangles by compositing the base accessory texture with the sprite of each
 * socketed gem, placed at that accessory's gem slots (necklace = 5 around the pendant, bangle = 3
 * across the top setting). Cached per (accessory, socketed gem types).
 */
public class AccessoryItemRenderer extends DynamicIconRenderer {

	private static final AccessoryItemRenderer INSTANCE = new AccessoryItemRenderer();

	// Top-left (row, col) where each gem's cropped sprite is drawn, in socket order.
	private static final int[][] NECKLACE_SLOTS = { { 6, 10 }, { 9, 12 }, { 10, 3 }, { 12, 6 }, { 12, 10 } };
	private static final int[][] BANGLE_SLOTS = { { 1, 6 }, { 3, 9 }, { 6, 11 } };

	private final Map<Item, Pixels> baseCache = new HashMap<>();
	private final Map<Item, Pixels> gemCache = new HashMap<>();

	private AccessoryItemRenderer() {
		super("accessory_icon");
	}

	public static void register() {
		BuiltinItemRendererRegistry.INSTANCE.register(ModItems.NECKLACE, INSTANCE);
		BuiltinItemRendererRegistry.INSTANCE.register(ModItems.BANGLE, INSTANCE);
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(INSTANCE);
	}

	@Override
	protected ResourceLocation texture(ItemStack stack) {
		Item item = stack.getItem();
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
		List<ItemStack> gems = AccessoryItem.getSockets(stack).gems();
		StringBuilder key = new StringBuilder(itemId.toString());
		for (ItemStack gem : gems) {
			key.append('|').append(BuiltInRegistries.ITEM.getKey(gem.getItem()));
		}
		ResourceLocation built = composeCached(key.toString(), () -> compose(item, itemId, gems));
		return built != null ? built : itemTexture(itemId);
	}

	private NativeImage compose(Item item, ResourceLocation itemId, List<ItemStack> gems) {
		Pixels base = baseCache.computeIfAbsent(item, k -> loadPixels(itemTexture(itemId)));
		if (base == null) {
			return null;
		}
		int[][] slots = item == ModItems.NECKLACE ? NECKLACE_SLOTS : BANGLE_SLOTS;

		NativeImage image = new NativeImage(SIZE, SIZE, false);
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				int pixel = col < base.width() && row < base.height() ? base.get(col, row) : 0;
				image.setPixelRGBA(col, row, pixel);
			}
		}

		for (int i = 0; i < gems.size() && i < slots.length; i++) {
			Pixels gem = gemSprite(gems.get(i).getItem());
			if (gem == null) {
				continue;
			}
			int slotRow = slots[i][0];
			int slotCol = slots[i][1];
			for (int gy = 0; gy < gem.height(); gy++) {
				for (int gx = 0; gx < gem.width(); gx++) {
					if (gem.alpha(gx, gy) > 16) {
						int x = slotCol + gx;
						int y = slotRow + gy;
						if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
							image.setPixelRGBA(x, y, gem.get(gx, gy));
						}
					}
				}
			}
		}
		return image;
	}

	private Pixels gemSprite(Item gem) {
		return gemCache.computeIfAbsent(gem, k -> {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(gem);
			Pixels raw = loadPixels(ResourceLocation.fromNamespaceAndPath(
					id.getNamespace(), "textures/accessory_gem/" + id.getPath() + ".png"));
			return raw == null ? null : raw.cropToOpaque();
		});
	}

	@Override
	protected void onReload() {
		baseCache.clear();
		gemCache.clear();
	}
}
