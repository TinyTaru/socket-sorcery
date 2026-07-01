package tinytaru.socketsorcery;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tinytaru.socketsorcery.item.RingReactions;
import tinytaru.socketsorcery.loot.SocketArtifactFunction;
import tinytaru.socketsorcery.net.ModNetworking;
import tinytaru.socketsorcery.pattern.Patterns;
import tinytaru.socketsorcery.registry.ModBlockEntities;
import tinytaru.socketsorcery.registry.ModBlocks;
import tinytaru.socketsorcery.registry.ModComponents;
import tinytaru.socketsorcery.registry.ModItemGroup;
import tinytaru.socketsorcery.registry.ModItems;
import tinytaru.socketsorcery.registry.ModLoot;
import tinytaru.socketsorcery.registry.ModMenus;
import tinytaru.socketsorcery.registry.ModSounds;

public class SocketSorcery implements ModInitializer {
	public static final String MOD_ID = "socket-sorcery";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Builds a {@link ResourceLocation} in this mod's namespace. */
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Order matters: components and blocks before the items/block-items that reference them,
		// patterns last so it can wire up gem/scroll compatibility against the registered items.
		ModComponents.init();
		ModSounds.init();
		ModBlocks.init();
		ModBlockEntities.init();
		ModItems.init();
		RingReactions.init();
		ModMenus.init();
		ModItemGroup.init();
		ModNetworking.registerServer();
		SocketArtifactFunction.init();
		ModLoot.init();
		Patterns.init();

		LOGGER.info("Socket Sorcery initialized");
	}
}
