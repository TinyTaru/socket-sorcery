package tinytaru.socketsorcery.client;

import java.util.List;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import tinytaru.socketsorcery.component.CrystalLampData;

/** Client-only snapshot of a lamp's masks and the partial-block surfaces reached by its light. */
public class CrystalLampRenderState extends BlockEntityRenderState {

	public CrystalLampData lampData = CrystalLampData.EMPTY;
	public List<LightHit> lightHits = List.of();

	public record LightHit(Direction normal, float x, float y, float z, float size,
			float minU, float maxU, float minV, float maxV) {
	}
}
