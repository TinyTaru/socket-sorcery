package tinytaru.socketsorcery.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import tinytaru.socketsorcery.client.screen.EngravingTableScreen;
import tinytaru.socketsorcery.client.screen.SocketingBenchScreen;
import tinytaru.socketsorcery.registry.ModBlocks;
import tinytaru.socketsorcery.registry.ModMenus;

public class SocketSorceryClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenus.ENGRAVING_TABLE, EngravingTableScreen::new);
		MenuScreens.register(ModMenus.SOCKETING_BENCH, SocketingBenchScreen::new);
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SOCKETING_BENCH, RenderType.cutout());
		GemItemRenderer.register();
		AccessoryItemRenderer.register();
		PatternTooltipRenderer.register();
		ModModelPredicates.register();
		ModKeybinds.register();
		BangleCooldownHud.register();
		ClientNetworking.registerClient();
	}
}
