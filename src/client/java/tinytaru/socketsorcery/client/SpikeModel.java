package tinytaru.socketsorcery.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * The spike raised by {@code socket-sorcery:spawn_spikes} — four stacked boxes narrowing to a point,
 * because {@link CubeListBuilder} has no way to describe a cone and a stepped taper reads correctly at
 * Minecraft's scale anyway.
 *
 * <p>The two middle boxes are nudged half a pixel off-axis in opposite directions. A ring of eight
 * identical spikes looks manufactured; a slight lean is the whole difference between rock and traffic
 * cones, and it costs nothing.
 *
 * <p>Drawn translucent so the alpha the renderer passes can fade the spike out at the end of its life.
 * The texture is greyscale — colour arrives as a per-entity tint, so one asset serves stone spikes and
 * ice shards alike.
 */
public final class SpikeModel extends EntityModel<SpikeRenderState> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(SocketSorcery.id("spike"), "main");

	public static final String SPIKE = "spike";

	/** Height of the built spike in model pixels — how far it sinks to be fully hidden. */
	public static final float HEIGHT = 13.0F;

	private final ModelPart spike;

	public SpikeModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
		this.spike = root.getChild(SPIKE);
	}

	public static LayerDefinition create() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Model space runs +Y down, so each successive box sits at a more negative Y: further out of the ground.
		root.addOrReplaceChild(SPIKE, CubeListBuilder.create()
						.texOffs(0, 0).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 4.0F, 6.0F)
						.texOffs(0, 10).addBox(-1.5F, -8.0F, -2.5F, 4.0F, 4.0F, 4.0F)
						.texOffs(0, 18).addBox(-1.5F, -11.0F, -0.5F, 2.0F, 3.0F, 2.0F)
						.texOffs(0, 24).addBox(-0.5F, -13.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(SpikeRenderState state) {
		super.setupAnim(state);
		// Fully sunk at progress 0, fully clear at 1 — the ground hides whatever is still below it.
		spike.y = (1.0F - state.riseProgress) * HEIGHT;
	}
}
