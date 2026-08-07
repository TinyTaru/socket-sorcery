package tinytaru.socketsorcery.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import tinytaru.socketsorcery.SocketSorcery;

/** One requested ink mark on the held Blank Scroll. The server validates both hand and ink. */
public record TranscribeCellC2SPayload(InteractionHand hand, int cell) implements CustomPacketPayload {
	public static final Type<TranscribeCellC2SPayload> ID = new Type<>(SocketSorcery.id("transcribe_cell"));
	public static final StreamCodec<ByteBuf, TranscribeCellC2SPayload> CODEC = StreamCodec.composite(
			InteractionHand.STREAM_CODEC, TranscribeCellC2SPayload::hand,
			ByteBufCodecs.VAR_INT, TranscribeCellC2SPayload::cell,
			TranscribeCellC2SPayload::new);

	@Override
	public Type<TranscribeCellC2SPayload> type() { return ID; }
}
