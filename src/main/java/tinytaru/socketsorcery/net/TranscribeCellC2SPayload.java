package tinytaru.socketsorcery.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import tinytaru.socketsorcery.SocketSorcery;

/** One requested mark on the held Blank Scroll. The server reads the ink from the offhand. */
public record TranscribeCellC2SPayload(InteractionHand hand, int cell)
		implements CustomPacketPayload {
	public static final Type<TranscribeCellC2SPayload> ID = new Type<>(SocketSorcery.id("transcribe_cell"));
	public static final StreamCodec<ByteBuf, TranscribeCellC2SPayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				InteractionHand.STREAM_CODEC.encode(buf, payload.hand);
				ByteBufCodecs.VAR_INT.encode(buf, payload.cell);
			},
			buf -> {
				InteractionHand hand = InteractionHand.STREAM_CODEC.decode(buf);
				int cell = ByteBufCodecs.VAR_INT.decode(buf);
				return new TranscribeCellC2SPayload(hand, cell);
			});

	@Override
	public Type<TranscribeCellC2SPayload> type() { return ID; }
}
