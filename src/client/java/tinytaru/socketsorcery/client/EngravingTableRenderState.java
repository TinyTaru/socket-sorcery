package tinytaru.socketsorcery.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Client-side snapshot of the engraving table's current workpiece. */
public class EngravingTableRenderState extends BlockEntityRenderState {

	public ItemStackRenderState gem;
	public ItemStackRenderState chisel;
}
