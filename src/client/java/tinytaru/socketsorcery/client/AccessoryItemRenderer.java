package tinytaru.socketsorcery.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.item.AccessoryItem;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModItems;
import tinytaru.socketsorcery.registry.ModComponents;

/**
 * Renders necklaces and bangles by compositing the base accessory texture with the sprite of each
 * socketed gem, placed at that accessory's gem slots (necklace = 5 around the pendant, bangle = 3
 * across the top setting). Cached per (accessory, socketed gem types).
 */
public class AccessoryItemRenderer extends DynamicIconRenderer {

	/** The special model type id the accessory item model definitions reference. */
	public static final Identifier ID = SocketSorcery.id("accessory");

	private static final AccessoryItemRenderer INSTANCE = new AccessoryItemRenderer();

	// Top-left (row, col) where each gem's cropped sprite is drawn, in socket order.
	private static final int[][] NECKLACE_SLOTS = { { 6, 10 }, { 9, 12 }, { 10, 3 }, { 12, 6 }, { 12, 10 } };
	private static final int[][] BANGLE_SLOTS = { { 1, 6 }, { 3, 9 }, { 6, 11 } };
	private static final int[][] RING_SLOTS = { { 9, 6 } };

	private final Map<Item, Pixels> baseCache = new HashMap<>();
	private final Map<Item, Pixels> gemCache = new HashMap<>();

	private AccessoryItemRenderer() {
		super("accessory_icon");
	}

	/** As {@code GemItemRenderer.Unbaked}: no data, unit codec, bakes to the shared instance. */
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

	@Override
	protected Identifier texture(ItemStack stack) {
		Item item = stack.getItem();
		Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
		List<ItemStack> gems = AccessoryItem.getSockets(stack).gems();
		EngravingData engraving = stack.get(ModComponents.ENGRAVING);
		StringBuilder key = new StringBuilder(itemId.toString());
		if (engraving != null) {
			key.append("|ring_pattern=").append(engraving.pattern());
		}
		for (ItemStack gem : gems) {
			key.append('|').append(BuiltInRegistries.ITEM.getKey(gem.getItem()));
		}
		Identifier built = composeCached(key.toString(), () -> compose(item, itemId, gems, engraving));
		return built != null ? built : itemTexture(itemId);
	}

	private NativeImage compose(Item item, Identifier itemId, List<ItemStack> gems, EngravingData engraving) {
		Pixels base = baseCache.computeIfAbsent(item, k -> loadPixels(itemTexture(itemId)));
		if (base == null) {
			return null;
		}
		int[][] slots = item == ModItems.NECKLACE ? NECKLACE_SLOTS : (item == ModItems.RING ? RING_SLOTS : BANGLE_SLOTS);

		NativeImage image = new NativeImage(SIZE, SIZE, false);
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				int pixel = col < base.width() && row < base.height() ? base.get(col, row) : 0;
				image.setPixel(col, row, pixel);
			}
		}

		if (item instanceof tinytaru.socketsorcery.item.RingItem && engraving != null) {
			HolderLookup.Provider registries = Minecraft.getInstance().level == null ? null
					: Minecraft.getInstance().level.registryAccess();
			var holder = Patterns.get(registries, engraving.pattern());
			if (holder != null && holder.value().ringTrigger().isPresent()) {
				int color = brightArgb(holder.value().color());
				for (int row = 0; row < SIZE; row++) {
					for (int col = 0; col < SIZE; col++) {
						if (holder.value().isCellCarved(row, col) && ((image.getPixel(col, row) >>> 24) & 0xFF) > 16) {
							image.setPixel(col, row, color);
						}
					}
				}
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
							image.setPixel(x, y, gem.get(gx, gy));
						}
					}
				}
			}
		}
		return image;
	}

	private Pixels gemSprite(Item gem) {
		return gemCache.computeIfAbsent(gem, k -> {
			Identifier id = BuiltInRegistries.ITEM.getKey(gem);
			Pixels raw = loadPixels(Identifier.fromNamespaceAndPath(
					id.getNamespace(), "textures/accessory_gem/" + id.getPath() + ".png"));
			return raw == null ? null : raw.cropToOpaque();
		});
	}

	private static int brightArgb(int rgb) {
		int r = (int) (((rgb >> 16) & 0xFF) * 0.8 + 255 * 0.2);
		int g = (int) (((rgb >> 8) & 0xFF) * 0.8 + 255 * 0.2);
		int b = (int) ((rgb & 0xFF) * 0.8 + 255 * 0.2);
		return (0xFF << 24) | (r << 16) | (g << 8) | b;
	}

	@Override
	protected void onReload() {
		baseCache.clear();
		gemCache.clear();
	}
}
