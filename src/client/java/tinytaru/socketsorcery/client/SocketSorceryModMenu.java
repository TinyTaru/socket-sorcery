package tinytaru.socketsorcery.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfigClient;
import tinytaru.socketsorcery.config.SocketSorceryConfig;

/**
 * Mod Menu hook: opens the Cloth Config AutoConfig screen for {@link SocketSorceryConfig}. Only
 * loaded when Mod Menu is present (it drives the {@code modmenu} entrypoint).
 */
public class SocketSorceryModMenu implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		// The screen factory moved to AutoConfigClient (AutoConfig itself is now common-side only).
		return parent -> AutoConfigClient.getConfigScreen(SocketSorceryConfig.class, parent).get();
	}
}
