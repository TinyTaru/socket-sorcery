package tinytaru.socketsorcery.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * The worn bangle's metal band — a single box that wraps the forearm, baked in two widths because
 * that is the <em>only</em> thing the slim player model changes about an arm.
 *
 * <p>26.1's {@code PlayerModel.createMesh} builds the arm as {@code addBox(-3,-2,-2, 4,12,4)} when
 * wide and {@code addBox(-2,-2,-2, 3,12,4)} when slim (mirrored for the left arm), both pivoted at
 * {@code (±5, 2, 0)}. So the height (12) and the depth (4) are identical, the pivot is identical —
 * only the X extent drops from 4 to 3, and it drops off the <em>outer</em> face; the face against
 * the body stays put. That is why one flag is enough here, and why the socketed gems (drawn by
 * {@link BangleTrinketRenderer}) are spaced along Z, the axis that never moves.
 *
 * <p>The cubes are centred on the origin rather than placed at the arm's real coordinates, because
 * {@code TrinketRenderer.translateToModelPart} aims at a part's live cube bounds: it lands us at the
 * arm's X/Z centre, which is {@code x = -1} on a wide right arm and {@code x = -0.5} on a slim one.
 * Positioning is therefore already slim-correct by the time this model is drawn.
 */
public final class BangleModel {

	public static final ModelLayerLocation WIDE = new ModelLayerLocation(SocketSorcery.id("bangle"), "wide");
	public static final ModelLayerLocation SLIM = new ModelLayerLocation(SocketSorcery.id("bangle"), "slim");

	/** The single part in each layer. */
	public static final String BAND = "band";

	/**
	 * How far the band stands off the arm skin. The player's own sleeve (second skin layer) is
	 * inflated by 0.25, so anything at or below that would z-fight with a skin that uses one.
	 */
	public static final float INFLATE = 0.4F;

	public static final float WIDE_WIDTH = 4.0F;
	public static final float SLIM_WIDTH = 3.0F;
	/** Half the band's height, before {@link #INFLATE}. The band is 2 model pixels tall. */
	public static final float HALF_HEIGHT = 1.0F;
	/** Half the band's depth, before {@link #INFLATE}. Matches the arm's 4-deep Z on both models. */
	public static final float HALF_DEPTH = 2.0F;

	private BangleModel() {
	}

	/** Half the band's outer X extent — the surface a gem on the outward face sits on. */
	public static float halfWidth(boolean slim) {
		return (slim ? SLIM_WIDTH : WIDE_WIDTH) / 2.0F + INFLATE;
	}

	/** Half the band's outer Z extent — the surface a gem on either side face sits on. */
	public static float halfDepth() {
		return HALF_DEPTH + INFLATE;
	}

	/**
	 * Builds the band. The 16x16 texture holds the wide unwrap at {@code (0,0)} (16x6, since a
	 * 4x2x4 box unwraps to {@code 2*(depth+width)} by {@code depth+height}) and the slim one at
	 * {@code (0,6)} (14x6) — separate regions, because a 3-wide box and a 4-wide box do not share
	 * a layout and sampling one from the other would seam.
	 */
	public static LayerDefinition create(boolean slim) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		float width = slim ? SLIM_WIDTH : WIDE_WIDTH;
		root.addOrReplaceChild(BAND, CubeListBuilder.create()
				.texOffs(0, slim ? 6 : 0)
				.addBox(-width / 2.0F, -HALF_HEIGHT, -HALF_DEPTH,
						width, HALF_HEIGHT * 2.0F, HALF_DEPTH * 2.0F, new CubeDeformation(INFLATE)),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 16, 16);
	}
}
