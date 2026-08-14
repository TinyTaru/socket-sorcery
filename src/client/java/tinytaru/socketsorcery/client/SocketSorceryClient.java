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
		// The socketing bench's cutout render layer is derived from its sprites now — no registration.
		GemItemRenderer.register();
		ScrollItemRenderer.register();
		ScrollDrawingMode.register();
		AccessoryItemRenderer.register();
		PatternTooltipRenderer.register();
		// Worn bangles: the band as a baked model layer, the socketed gems drawn on top of it.
		BangleTrinketRenderer.register();
		// Spikes raised by pattern effects — the one entity the mod draws itself.
		SpikeRenderer.register();
		CrystalLampRenderer.register();
		// The old "engraving" model predicate is gone: item model definitions select the engraved
		// variant with vanilla's minecraft:has_component condition instead (see assets/…/items/).
		ModKeybinds.register();
		BangleCooldownHud.register();
		ClientNetworking.registerClient();
	}
}
