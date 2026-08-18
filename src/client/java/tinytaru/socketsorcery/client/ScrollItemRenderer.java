package tinytaru.socketsorcery.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.component.ScrollDrawingData;
import tinytaru.socketsorcery.item.BlankScrollItem;
import tinytaru.socketsorcery.item.ScrollItem;
import tinytaru.socketsorcery.item.ScrollInkColor;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModComponents;

/** Composites ancient, transcribed, and in-progress scrolls from the authored base sprites. */
public final class ScrollItemRenderer extends DynamicIconRenderer {
	public static final Identifier ID = SocketSorcery.id("scroll");
	private static final ScrollItemRenderer INSTANCE = new ScrollItemRenderer();
	private static final int TRIGGER_PATTERN_RAISE = 3;
	private final Map<Identifier, Pixels> bases = new HashMap<>();

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
		INSTANCE.submitCloseUp(INSTANCE.extractArgument(stack), poseStack, collector, light, stack.hasFoil());
	}

	@Override
	protected Identifier texture(ItemStack stack) {
		return baseTexture(stack);
	}

	@Override
	protected Identifier backTexture(ItemStack stack) {
		if (stack.getItem() instanceof BlankScrollItem) {
			ScrollDrawingData drawing = stack.getOrDefault(ModComponents.SCROLL_DRAWING, ScrollDrawingData.EMPTY);
			return composeCached("blank|" + drawing.ink() + "|" + java.util.Arrays.toString(drawing.painted()),
					() -> compose(baseTexture(stack), drawing.painted(), 0xFF000000 | drawing.ink()
							.map(ScrollInkColor::rgb).orElse(0x111111)));
		}
		if (!(stack.getItem() instanceof ScrollItem scroll)) return baseTexture(stack);
		HolderLookup.Provider registries = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.registryAccess();
		var holder = Patterns.forScroll(registries, scroll);
		if (holder == null) return baseTexture(stack);
		boolean transcribed = stack.has(ModComponents.TRANSCRIBED_SCROLL);
		Pattern pattern = holder.value();
		String key = (transcribed ? "transcribed|" : "ancient|") + holder.key().identifier() + "|" + pattern.color();
		return composeCached(key, () -> compose(baseTexture(stack), pattern.maskBits(), 0xFF000000 | pattern.color()));
	}

	private Identifier baseTexture(ItemStack stack) {
		boolean blank = stack.getItem() instanceof BlankScrollItem
				|| stack.has(ModComponents.TRANSCRIBED_SCROLL);
		boolean ringTrigger = false;
		if (stack.getItem() instanceof ScrollItem scroll) {
			HolderLookup.Provider registries = Minecraft.getInstance().level == null ? null
					: Minecraft.getInstance().level.registryAccess();
			Holder.Reference<Pattern> holder = Patterns.forScroll(registries, scroll);
			ringTrigger = holder != null && holder.value().ringTrigger().isPresent();
		}
		String base = blank ? "scroll_blank" : "scroll_ancient";
		if (ringTrigger) base += "_trigger";
		return itemTexture(SocketSorcery.id(base));
	}

	private NativeImage compose(Identifier baseTexture, long[] marks, int color) {
		Pixels base = bases.computeIfAbsent(baseTexture, this::loadBase);
		if (base == null || base.width() != SIZE || base.height() != SIZE) return null;
		int markRowOffset = baseTexture.getPath().endsWith("_trigger.png") ? TRIGGER_PATTERN_RAISE : 0;
		NativeImage image = new NativeImage(SIZE, SIZE, false);
		for (int row = 0; row < SIZE; row++) for (int col = 0; col < SIZE; col++) {
			int pixel = base.get(col, row);
			image.setPixel(col, row, ((pixel >>> 24) & 0xFF) <= 16 ? 0
					: row + markRowOffset < SIZE
							&& tinytaru.socketsorcery.pattern.GridBits.get(marks, row + markRowOffset, col)
									? color : pixel);
		}
		return image;
	}

	private Pixels loadBase(Identifier baseTexture) {
		return loadPixels(baseTexture);
	}

	@Override protected void onReload() { bases.clear(); }
}
