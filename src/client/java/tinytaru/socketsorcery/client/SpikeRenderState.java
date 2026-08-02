package tinytaru.socketsorcery.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Per-frame snapshot of a spike, mirroring vanilla's {@code EvokerFangsRenderState}. */
public class SpikeRenderState extends EntityRenderState {

	public float yRot;
	/** 0 while still underground, 1 once fully risen. */
	public float riseProgress;
	public float scale = 1.0F;
	/** ARGB — the alpha carries the end-of-life fade, the RGB the pattern's colour. */
	public int color = 0xFFFFFFFF;
}
