package tinytaru.socketsorcery.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener.SharedState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.item.AccessoryItem;
import tinytaru.socketsorcery.item.BangleItem;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * Draws an equipped bangle on the wearer's main arm: the {@link BangleModel} band, plus a flat gem
 * inlay for each socketed gem.
 *
 * <p>The band is a baked {@link ModelPart} — it wants a proper box with an inflate, and
 * {@code submitModelPart} gives that for free. The gems are hand-emitted quads through
 * {@code submitCustomGeometry} instead, because a {@code ModelPart} ties a cube's model size to its
 * texel count 1:1: a 2-pixel gem would get a 2x2 texel face, and the gem sprites are a 4x4 cut
 * shape. Emitting the quads directly decouples the two, so each gem shows its real artwork — the
 * very same {@code textures/accessory_gem/} sprite that {@link AccessoryItemRenderer} composites
 * into the inventory icon, sampled over its opaque bounds so any canvas size works. Note that such
 * vertices go into the buffer unscaled, hence {@link #UNIT}.
 *
 * <p>Slim arms need no special casing beyond picking the narrower band:
 * {@code TrinketRenderer.translateToModelPart} aims at the arm part's <em>live</em> cube bounds, so
 * the anchor already tracks the 1-pixel-narrower arm, and the gems are spaced along Z and Y, the
 * axes the slim model leaves alone (see {@link BangleModel}).
 */
public final class BangleTrinketRenderer extends SimpleReloadListener<Void> implements TrinketRenderer {

	private static final BangleTrinketRenderer INSTANCE = new BangleTrinketRenderer();

	/**
	 * Where on the arm the band sits, as a fraction of the arm part's own bounding box: centred in X
	 * and Z, and halfway from the middle towards the wrist. The arm spans y -2..10 in model space
	 * (+Y is down), so this lands at y = 7, just above the hand.
	 */
	private static final Vector3fc ARM_ANCHOR = new Vector3f(0.0F, -0.5F, 0.0F);

	/**
	 * Model units to blocks. The stud geometry below is written in model units to match
	 * {@link BangleModel}, but unlike a {@code ModelPart} — whose cubes {@code ModelPart.compile}
	 * scales by 1/16 on the way to the buffer — hand-emitted vertices go into {@code addVertex}
	 * unscaled, so they must be converted here or the studs come out sixteen times too large.
	 */
	private static final float UNIT = 1.0F / 16.0F;

	/**
	 * How far up the arm, in model pixels, the first-person band sits relative to the third-person
	 * one. Model space has +Y pointing down, so this is applied as a negative translation.
	 */
	private static final float FIRST_PERSON_RAISE = 1.0F;

	/** Half a gem's width and height on the face it sits on: the gems are 2x2 model pixels. */
	private static final float STUD_HALF = 1.0F;
	/** How far a gem floats off the band — just enough to clear it without reading as extruded. */
	private static final float STUD_RISE = 0.25F;

	/** Gem quad corners, walked as a ring: (-u,-v), (-u,+v), (+u,+v), (+u,-v). */
	private static final float[][] RING = { { -1.0F, -1.0F }, { -1.0F, 1.0F }, { 1.0F, 1.0F }, { 1.0F, -1.0F } };

	/** A gem sprite plus the UV rect its artwork occupies within that sprite. */
	private record GemArt(Identifier texture, float u0, float v0, float u1, float v1) {
	}

	private final Map<Item, Identifier> bandTextures = new HashMap<>();
	private final Map<Item, GemArt> gemArt = new HashMap<>();
	private ModelPart wideBand;
	private ModelPart slimBand;

	private BangleTrinketRenderer() {
	}

	public static void register() {
		ModelLayerRegistry.registerModelLayer(BangleModel.WIDE, () -> BangleModel.create(false));
		ModelLayerRegistry.registerModelLayer(BangleModel.SLIM, () -> BangleModel.create(true));
		for (BangleItem bangle : List.of(ModItems.COPPER_BANGLE, ModItems.BANGLE, ModItems.NETHERITE_BANGLE)) {
			TrinketRendererRegistry.registerRenderer(bangle, INSTANCE);
		}
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(INSTANCE.getFabricId(), INSTANCE);
	}

	@Override
	public void submit(ItemStack stack, TrinketSlotAccess slot, EntityModel<? extends LivingEntityRenderState> model,
			PoseStack poseStack, SubmitNodeCollector collector, int light, LivingEntityRenderState state,
			float headYaw, float headPitch) {
		// Non-players (armour stands, mobs Trinkets can equip) have no skin, so they get the wide arm.
		boolean slim = state instanceof AvatarRenderState avatar && avatar.skin.model() == PlayerModelType.SLIM;
		HumanoidArm arm = state instanceof ArmedEntityRenderState armed ? armed.mainArm : HumanoidArm.RIGHT;
		submitBangle(stack, model, poseStack, collector, light, arm, slim, 0.0F);
	}

	@Override
	public void submitFirstPersonRightArm(ItemStack stack, TrinketSlotAccess slot,
			EntityModel<? extends LivingEntityRenderState> model, ModelPart armPart, PoseStack poseStack,
			SubmitNodeCollector collector, int light, LocalPlayer player, boolean isMainArm) {
		submitFirstPerson(stack, model, poseStack, collector, light, player, isMainArm, HumanoidArm.RIGHT);
	}

	@Override
	public void submitFirstPersonLeftArm(ItemStack stack, TrinketSlotAccess slot,
			EntityModel<? extends LivingEntityRenderState> model, ModelPart armPart, PoseStack poseStack,
			SubmitNodeCollector collector, int light, LocalPlayer player, boolean isMainArm) {
		submitFirstPerson(stack, model, poseStack, collector, light, player, isMainArm, HumanoidArm.LEFT);
	}

	/**
	 * First person shares the third-person placement: {@code renderHand} leaves the pose at the model
	 * root and lets the arm part's own transform carry it into view, so the same anchor lands in the
	 * same spot — offset by {@link #FIRST_PERSON_RAISE}, which this view wants. Only the main arm
	 * draws, matching {@link #submit}.
	 *
	 * <p>Note that Trinkets gates this entire path behind its own {@code renderFirstPersonHand}
	 * setting, which ships disabled, so nothing here runs unless the player has turned it on.
	 */
	private void submitFirstPerson(ItemStack stack, EntityModel<? extends LivingEntityRenderState> model,
			PoseStack poseStack, SubmitNodeCollector collector, int light, LocalPlayer player, boolean isMainArm,
			HumanoidArm arm) {
		if (!isMainArm) {
			return;
		}
		boolean slim = player instanceof ClientAvatarEntity avatar
				&& avatar.getSkin().model() == PlayerModelType.SLIM;
		submitBangle(stack, model, poseStack, collector, light, arm, slim, FIRST_PERSON_RAISE);
	}

	/**
	 * @param upArm model pixels to shift towards the elbow, on top of {@link #ARM_ANCHOR}. The pose
	 *              is in blocks by this point — {@code ModelPart.translateAndRotate} divides its
	 *              pivot by 16 and the anchor's bounds are pre-divided — hence {@link #UNIT}.
	 */
	private void submitBangle(ItemStack stack, EntityModel<? extends LivingEntityRenderState> model,
			PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidArm arm, boolean slim,
			float upArm) {
		ModelPart band = band(slim);
		if (band == null) {
			return;
		}

		poseStack.pushPose();
		String part = arm == HumanoidArm.RIGHT ? "right_arm" : "left_arm";
		if (TrinketRenderer.translateToModelPart(poseStack, model, part, ARM_ANCHOR)) {
			if (upArm != 0.0F) {
				poseStack.translate(0.0F, -upArm * UNIT, 0.0F);
			}
			collector.submitModelPart(band, poseStack, RenderTypes.entityCutout(bandTexture(stack.getItem())),
					light, OverlayTexture.NO_OVERLAY, null);
			submitGems(stack, arm, slim, poseStack, collector, light);
		}
		poseStack.popPose();
	}

	/**
	 * Places the socketed gems around the band: socket 0 on its left, socket 1 on its face, socket 2
	 * on its right, as seen by someone looking at the outward face. Left and right therefore swap
	 * with the arm — the same way a real bangle looks the same whichever wrist it is on.
	 */
	private void submitGems(ItemStack stack, HumanoidArm arm, boolean slim, PoseStack poseStack,
			SubmitNodeCollector collector, int light) {
		List<ItemStack> gems = AccessoryItem.getSockets(stack).gems();
		// Which way is "away from the body": model +X is the wearer's left, so the right arm faces -X.
		float outward = arm == HumanoidArm.RIGHT ? -1.0F : 1.0F;
		float halfWidth = BangleModel.halfWidth(slim);
		float halfDepth = BangleModel.halfDepth();

		for (int i = 0; i < gems.size() && i < BangleItem.CAPACITY; i++) {
			ItemStack gem = gems.get(i);
			if (gem.isEmpty()) {
				continue;
			}
			GemArt art = gemArt(gem.getItem());
			if (art == null) {
				continue;
			}
			if (i == 1) {
				submitStud(collector, poseStack, light, art,
						outward * halfWidth, 0.0F, 0.0F, outward, 0.0F, 0.0F);
			} else {
				// Slot 0 sits on the viewer's left, which is +Z on a right arm and -Z on a left one.
				float side = i == 0 ? -outward : outward;
				submitStud(collector, poseStack, light, art,
						0.0F, 0.0F, side * halfDepth, 0.0F, 0.0F, side);
			}
		}
	}

	/**
	 * Emits one gem: a single flat quad carrying the sprite, floating {@link #STUD_RISE} off the band.
	 * There are deliberately no side faces — at two pixels across, an extruded edge reads as noise
	 * rather than depth, and a flat inlay suits the game's art style better.
	 */
	private static void submitStud(SubmitNodeCollector collector, PoseStack poseStack, int light, GemArt art,
			float cx, float cy, float cz, float nx, float ny, float nz) {
		// Screen-right for someone facing the gem, i.e. cross(-normal, up) with model up = (0,-1,0).
		float ux = -nz;
		float uy = 0.0F;
		float uz = nx;
		// The sprite's +V runs down the image, which is model +Y.
		float vx = 0.0F;
		float vy = 1.0F;
		float vz = 0.0F;

		float[][] corners = new float[4][];
		for (int c = 0; c < 4; c++) {
			float su = RING[c][0] * STUD_HALF;
			float sv = RING[c][1] * STUD_HALF;
			corners[c] = new float[] {
					cx + nx * STUD_RISE + su * ux + sv * vx,
					cy + ny * STUD_RISE + su * uy + sv * vy,
					cz + nz * STUD_RISE + su * uz + sv * vz,
			};
		}

		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(art.texture()), (pose, consumer) -> {
			vertex(consumer, pose, corners[0], art.u0(), art.v0(), light, nx, ny, nz);
			vertex(consumer, pose, corners[1], art.u0(), art.v1(), light, nx, ny, nz);
			vertex(consumer, pose, corners[2], art.u1(), art.v1(), light, nx, ny, nz);
			vertex(consumer, pose, corners[3], art.u1(), art.v0(), light, nx, ny, nz);
		});
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float[] position, float u, float v,
			int light, float nx, float ny, float nz) {
		consumer.addVertex(pose, position[0] * UNIT, position[1] * UNIT, position[2] * UNIT)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);
	}

	/** The baked band, fetched lazily — {@code EntityModelSet} is not ready during client init. */
	private ModelPart band(boolean slim) {
		if (slim) {
			if (slimBand == null) {
				slimBand = bake(BangleModel.SLIM);
			}
			return slimBand;
		}
		if (wideBand == null) {
			wideBand = bake(BangleModel.WIDE);
		}
		return wideBand;
	}

	private static ModelPart bake(ModelLayerLocation layer) {
		return Minecraft.getInstance().getEntityModels().bakeLayer(layer).getChild(BangleModel.BAND);
	}

	private Identifier bandTexture(Item bangle) {
		return bandTextures.computeIfAbsent(bangle, item -> {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			return Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/entity/bangle/" + id.getPath() + ".png");
		});
	}

	/**
	 * The stud artwork for a gem item, or null if it has no accessory sprite. Misses are cached too:
	 * this runs per gem per frame, and re-reading a missing resource every time would be costly.
	 */
	private GemArt gemArt(Item gem) {
		if (gemArt.containsKey(gem)) {
			return gemArt.get(gem);
		}
		GemArt art = loadGemArt(gem);
		gemArt.put(gem, art);
		return art;
	}

	private static GemArt loadGemArt(Item gem) {
		Identifier id = BuiltInRegistries.ITEM.getKey(gem);
		Identifier texture = Identifier.fromNamespaceAndPath(
				id.getNamespace(), "textures/accessory_gem/" + id.getPath() + ".png");
		DynamicIconRenderer.Pixels pixels = DynamicIconRenderer.loadPixels(texture);
		if (pixels == null) {
			return null;
		}
		int[] bounds = pixels.opaqueBounds();
		if (bounds == null) {
			return null;
		}
		// The sprites sit on differently sized canvases (6x6, 8x8, 16x16), so address the artwork by
		// its opaque bounds rather than assuming it fills the image.
		float width = pixels.width();
		float height = pixels.height();
		return new GemArt(texture, bounds[0] / width, bounds[1] / height,
				(bounds[2] + 1) / width, (bounds[3] + 1) / height);
	}

	public Identifier getFabricId() {
		return SocketSorcery.id("bangle_renderer");
	}

	@Override
	protected Void prepare(SharedState sharedState) {
		return null;
	}

	@Override
	protected void apply(Void ignored, SharedState sharedState) {
		// Both the baked models and the gem sprites come from the packs that just changed.
		wideBand = null;
		slimBand = null;
		gemArt.clear();
	}
}
