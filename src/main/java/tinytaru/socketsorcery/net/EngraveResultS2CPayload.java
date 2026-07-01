package tinytaru.socketsorcery.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * Server → client result of an engraving attempt, identified by an {@link EngraveResult} ordinal.
 * Lets the client play the right sound and surface a reason without re-running validation.
 */
public record EngraveResultS2CPayload(int reason) implements CustomPacketPayload {

	public static final Type<EngraveResultS2CPayload> ID =
			new Type<>(SocketSorcery.id("engrave_result"));

	public static final StreamCodec<RegistryFriendlyByteBuf, EngraveResultS2CPayload> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, EngraveResultS2CPayload::reason,
					EngraveResultS2CPayload::new);

	public EngraveResult result() {
		return EngraveResult.byId(reason);
	}

	@Override
	public Type<EngraveResultS2CPayload> type() {
		return ID;
	}
}
