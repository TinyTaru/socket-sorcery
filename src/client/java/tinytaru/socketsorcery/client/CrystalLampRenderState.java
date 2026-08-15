package tinytaru.socketsorcery.client;

import java.util.List;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import tinytaru.socketsorcery.component.CrystalLampData;

/** Client snapshot of the lamp face masks and physically-inspired projector geometry. */
public class CrystalLampRenderState extends BlockEntityRenderState {

	public CrystalLampData lampData = CrystalLampData.EMPTY;
	public List<ProjectionBatch> projections = List.of();

	public record ProjectionBatch(Direction projectorFace, List<SurfaceQuad> quads) {
	}

	/**
	 * One small patch of receiving geometry. Alpha is stored per vertex so the projected engraving can
	 * soften with distance and fade naturally across angled surfaces.
	 */
	public record SurfaceQuad(Direction normal,
			float x0, float y0, float z0, float a0,
			float x1, float y1, float z1, float a1,
			float x2, float y2, float z2, float a2,
			float x3, float y3, float z3, float a3) {
	}
}
