package tinytaru.socketsorcery.advancement;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * The mod's custom advancement criteria, registered into the vanilla {@code TRIGGER_TYPES} registry
 * (no access widener needed in 1.21). Fired from gameplay code:
 * <ul>
 *   <li>{@link #ENGRAVE} — {@code EngravingTableMenu.chisel} whenever a stroke completes the pattern
 *       or adds a modifier to it.</li>
 *   <li>{@link #EQUIP_ACCESSORY} — {@code AccessoryItem.onEquip}.</li>
 *   <li>{@link #ACTIVATE_ABILITY} — {@code AccessoryItem.runBangle} when a socketed gem fires.</li>
 * </ul>
 */
public final class ModCriteria {

	public static final EngravePatternTrigger ENGRAVE = register("engrave_pattern", new EngravePatternTrigger());
	public static final SimplePlayerTrigger EQUIP_ACCESSORY = register("equip_accessory", new SimplePlayerTrigger());
	public static final SimplePlayerTrigger ACTIVATE_ABILITY = register("activate_ability", new SimplePlayerTrigger());

	private static <T extends CriterionTrigger<?>> T register(String path, T trigger) {
		return Registry.register(BuiltInRegistries.TRIGGER_TYPES, SocketSorcery.id(path), trigger);
	}

	/** Forces class load so the static triggers register. Call once during mod init. */
	public static void init() {
	}

	private ModCriteria() {
	}
}
