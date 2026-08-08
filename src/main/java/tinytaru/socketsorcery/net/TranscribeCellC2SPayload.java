package tinytaru.socketsorcery.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.item.ScrollInkColor;

import java.util.Optional;

/** One requested ink mark on the held Blank Scroll. The server validates both hand and ink. */
public record TranscribeCellC2SPayload(InteractionHand hand, int cell, Optional<ScrollInkColor> ink)
		implements CustomPacketPayload {
	public static final Type<TranscribeCellC2SPayload> ID = new Type<>(SocketSorcery.id("transcribe_cell"));
	public static final StreamCodec<ByteBuf, TranscribeCellC2SPayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				InteractionHand.STREAM_CODEC.encode(buf, payload.hand);
				ByteBufCodecs.VAR_INT.encode(buf, payload.cell);
				buf.writeBoolean(payload.ink.isPresent());
				payload.ink.ifPresent(color -> ScrollInkColor.STREAM_CODEC.encode(buf, color));
			},
			buf -> {
				InteractionHand hand = InteractionHand.STREAM_CODEC.decode(buf);
				int cell = ByteBufCodecs.VAR_INT.decode(buf);
				Optional<ScrollInkColor> ink = buf.readBoolean()
						? Optional.of(ScrollInkColor.STREAM_CODEC.decode(buf)) : Optional.empty();
				return new TranscribeCellC2SPayload(hand, cell, ink);
			});

	public TranscribeCellC2SPayload(InteractionHand hand, int cell) {
		this(hand, cell, Optional.empty());
	}

	@Override
	public Type<TranscribeCellC2SPayload> type() { return ID; }
}
