package tinytaru.socketsorcery.registry;

import java.util.function.UnaryOperator;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.component.CarvingData;
import tinytaru.socketsorcery.component.CrystalLampData;
import tinytaru.socketsorcery.component.EngravingData;
import tinytaru.socketsorcery.component.GlassPaneEngravingData;
import tinytaru.socketsorcery.component.SocketData;
import tinytaru.socketsorcery.component.ScrollDrawingData;

/**
 * Registers the mod's {@link DataComponentType}s:
 * <ul>
 *   <li>{@code engraving} — the chiselled pattern carried by an engraved gem.</li>
 *   <li>{@code carving} — the half-finished cuts on a gem still being chiselled.</li>
 *   <li>{@code sockets} — the ordered gems socketed into an accessory.</li>
 * </ul>
 */
public final class ModComponents {

	public static final DataComponentType<EngravingData> ENGRAVING = register("engraving",
			builder -> builder.persistent(EngravingData.CODEC).networkSynchronized(EngravingData.STREAM_CODEC));

	public static final DataComponentType<CarvingData> CARVING = register("carving",
			builder -> builder.persistent(CarvingData.CODEC).networkSynchronized(CarvingData.STREAM_CODEC));

	/** Freeform 16×16 etching on a glass pane, used exclusively by Crystal Lamps. */
	public static final DataComponentType<GlassPaneEngravingData> GLASS_PANE_ENGRAVING = register("glass_pane_engraving",
			builder -> builder.persistent(GlassPaneEngravingData.CODEC)
					.networkSynchronized(GlassPaneEngravingData.STREAM_CODEC));

	/** The five freeform light masks stored by a Crystal Lamp block or item. */
	public static final DataComponentType<CrystalLampData> CRYSTAL_LAMP = register("crystal_lamp",
			builder -> builder.persistent(CrystalLampData.CODEC).networkSynchronized(CrystalLampData.STREAM_CODEC));

	public static final DataComponentType<SocketData> SOCKETS = register("sockets",
			builder -> builder.persistent(SocketData.CODEC).networkSynchronized(SocketData.STREAM_CODEC));

	/** Painted 16x16 cells on a Blank Scroll. */
	public static final DataComponentType<ScrollDrawingData> SCROLL_DRAWING = register("scroll_drawing",
			builder -> builder.persistent(ScrollDrawingData.CODEC).networkSynchronized(ScrollDrawingData.STREAM_CODEC));

	/** Marks a completed pattern scroll as player-transcribed, selecting the blank-paper base art. */
	public static final DataComponentType<Boolean> TRANSCRIBED_SCROLL = register("transcribed_scroll",
			builder -> builder.persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

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
