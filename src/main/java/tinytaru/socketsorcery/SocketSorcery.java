package tinytaru.socketsorcery;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tinytaru.socketsorcery.config.SocketSorceryConfig;
import tinytaru.socketsorcery.item.RingReactions;
import tinytaru.socketsorcery.loot.SocketArtifactFunction;
import tinytaru.socketsorcery.net.ModNetworking;
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

	/** Builds a {@link Identifier} in this mod's namespace. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Order matters: config first (loot/cooldown code reads it), components and blocks before the
		// items/block-items that reference them, patterns last so it can wire up gem/scroll
		// compatibility against the registered items.
		SocketSorceryConfig.init();
		ModComponents.init();
		ModSounds.init();
		tinytaru.socketsorcery.registry.ModRegistries.init();
		ModBlocks.init();
		ModBlockEntities.init();
		ModItems.init();
		RingReactions.init();
		ModMenus.init();
		ModItemGroup.init();
		ModNetworking.registerServer();
		tinytaru.socketsorcery.advancement.ModCriteria.init();
		SocketArtifactFunction.init();
		ModLoot.init();
		tinytaru.socketsorcery.pattern.RegistryValidation.init();

		LOGGER.info("Socket Sorcery initialized");
	}
}
