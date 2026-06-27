package tinytaru.socketsorcery.registry;

import java.util.function.UnaryOperator;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.component.SocketData;

/**
 * Registers the mod's {@link DataComponentType}s:
 * <ul>
 *   <li>{@code engraving} — the chiselled pattern carried by an engraved gem.</li>
 *   <li>{@code sockets} — the ordered gems socketed into an accessory.</li>
 * </ul>
 */
public final class ModComponents {

	public static final DataComponentType<EngravingData> ENGRAVING = register("engraving",
			builder -> builder.persistent(EngravingData.CODEC).networkSynchronized(EngravingData.STREAM_CODEC));

	public static final DataComponentType<SocketData> SOCKETS = register("sockets",
			builder -> builder.persistent(SocketData.CODEC).networkSynchronized(SocketData.STREAM_CODEC));

	private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> op) {
		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SocketSorcery.id(name),
				op.apply(DataComponentType.builder()).build());
	}

	/** Forces class load so the static fields register. */
	public static void init() {
	}

	private ModComponents() {
	}
}
