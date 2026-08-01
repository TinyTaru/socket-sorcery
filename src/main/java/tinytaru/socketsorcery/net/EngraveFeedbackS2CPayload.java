package tinytaru.socketsorcery.net;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * Sent when a chisel stroke finished something, so the Engraving Table screen can celebrate it. The
 * carve itself already reaches the client as the gem's own components — this only carries the
 * <em>event</em>, which the item can't express.
 *
 * @param patternComplete true when that stroke turned the carve into a finished engraving; the
 *                        screen pulses every cut cell
 * @param modifiers       the modifiers that stroke completed (empty if none); with
 *                        {@code patternComplete} false the screen pulses only their cells
 */
public record EngraveFeedbackS2CPayload(boolean patternComplete, List<Identifier> modifiers)
		implements CustomPacketPayload {

	public static final Type<EngraveFeedbackS2CPayload> ID =
			new Type<>(SocketSorcery.id("engrave_feedback"));

	public static final StreamCodec<ByteBuf, EngraveFeedbackS2CPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, EngraveFeedbackS2CPayload::patternComplete,
			Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), EngraveFeedbackS2CPayload::modifiers,
			EngraveFeedbackS2CPayload::new);

	@Override
	public Type<EngraveFeedbackS2CPayload> type() {
		return ID;
	}
}
