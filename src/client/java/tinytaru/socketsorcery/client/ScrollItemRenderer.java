package tinytaru.socketsorcery.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.component.ScrollDrawingData;
import tinytaru.socketsorcery.item.BlankScrollItem;
import tinytaru.socketsorcery.item.ScrollItem;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;

/** Composites ancient, transcribed, and in-progress scrolls from the authored base sprites. */
public final class ScrollItemRenderer extends DynamicIconRenderer {
	public static final Identifier ID = SocketSorcery.id("scroll");
	private static final ScrollItemRenderer INSTANCE = new ScrollItemRenderer();
	private final Map<Boolean, Pixels> bases = new HashMap<>();

	private ScrollItemRenderer() { super("scroll_icon"); }

	public record Unbaked() implements SpecialModelRenderer.Unbaked<Icon> {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);
		@Override public SpecialModelRenderer<Icon> bake(SpecialModelRenderer.BakingContext context) { return INSTANCE; }
		@Override public MapCodec<Unbaked> type() { return MAP_CODEC; }
	}

	public static void register() {
		SpecialModelRenderers.ID_MAPPER.put(ID, Unbaked.MAP_CODEC);
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(INSTANCE.getFabricId(), INSTANCE);
	}

	/** The composed texture used by the close-up transcription presentation. */
	public static Identifier drawingTexture(ItemStack stack) {
		return INSTANCE.texture(stack);
	}

	/** Draws the dynamic icon directly into the known camera plane used while transcribing. */
	public static void submitCloseUp(ItemStack stack, com.mojang.blaze3d.vertex.PoseStack poseStack,
			net.minecraft.client.renderer.SubmitNodeCollector collector, int light) {
		INSTANCE.submitCloseUp(INSTANCE.extractArgument(stack), poseStack, collector, light);
	}

	@Override
	protected Identifier texture(ItemStack stack) {
		return baseTexture(stack);
	}

	@Override
	protected Identifier backTexture(ItemStack stack) {
		if (stack.getItem() instanceof BlankScrollItem) {
			ScrollDrawingData drawing = stack.getOrDefault(ModComponents.SCROLL_DRAWING, ScrollDrawingData.EMPTY);
			return composeCached("blank|" + java.util.Arrays.toString(drawing.painted()),
					() -> compose(true, drawing.painted(), 0xFF111111));
		}
		if (!(stack.getItem() instanceof ScrollItem scroll)) return baseTexture(stack);
		HolderLookup.Provider registries = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.registryAccess();
		var holder = Patterns.forScroll(registries, scroll);
		if (holder == null) return baseTexture(stack);
		boolean transcribed = stack.has(ModComponents.TRANSCRIBED_SCROLL);
		Pattern pattern = holder.value();
		String key = (transcribed ? "transcribed|" : "ancient|") + holder.key().identifier() + "|" + pattern.color();
		return composeCached(key, () -> compose(transcribed, pattern.maskBits(), 0xFF000000 | pattern.color()));
	}

	private Identifier baseTexture(ItemStack stack) {
		boolean blank = stack.getItem() instanceof BlankScrollItem
				|| stack.has(ModComponents.TRANSCRIBED_SCROLL);
		return itemTexture(SocketSorcery.id(blank ? "scroll_blank" : "scroll_ancient"));
	}

	private NativeImage compose(boolean blankBase, long[] marks, int color) {
		Pixels base = bases.computeIfAbsent(blankBase, this::loadBase);
		if (base == null || base.width() != SIZE || base.height() != SIZE) return null;
		NativeImage image = new NativeImage(SIZE, SIZE, false);
		for (int row = 0; row < SIZE; row++) for (int col = 0; col < SIZE; col++) {
			int pixel = base.get(col, row);
			image.setPixel(col, row, ((pixel >>> 24) & 0xFF) <= 16 ? 0
					: tinytaru.socketsorcery.pattern.GridBits.get(marks, row, col) ? color : pixel);
		}
		return image;
	}

	private Pixels loadBase(boolean blank) {
		return loadPixels(itemTexture(SocketSorcery.id(blank ? "scroll_blank" : "scroll_ancient")));
	}

	@Override protected void onReload() { bases.clear(); }
}
