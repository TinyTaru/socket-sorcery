package tinytaru.socketsorcery.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import tinytaru.socketsorcery.client.screen.EngravingTableScreen;
import tinytaru.socketsorcery.client.screen.SocketingBenchScreen;
import tinytaru.socketsorcery.registry.ModMenus;

public class SocketSorceryClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenus.ENGRAVING_TABLE, EngravingTableScreen::new);
		MenuScreens.register(ModMenus.SOCKETING_BENCH, SocketingBenchScreen::new);
		GemItemRenderer.register();
		AccessoryItemRenderer.register();
		ModModelPredicates.register();
		ModKeybinds.register();
		BangleCooldownHud.register();
		ClientNetworking.registerClient();
	}
}
