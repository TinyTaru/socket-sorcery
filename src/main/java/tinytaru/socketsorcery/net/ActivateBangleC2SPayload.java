package tinytaru.socketsorcery.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * Sent when the player presses the Activate Bangle keybind. Carries no data — the server finds the
 * worn bangle and ray-traces the look target itself.
 */
public record ActivateBangleC2SPayload() implements CustomPacketPayload {

	public static final ActivateBangleC2SPayload INSTANCE = new ActivateBangleC2SPayload();

	public static final Type<ActivateBangleC2SPayload> ID =
			new Type<>(SocketSorcery.id("activate_bangle"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ActivateBangleC2SPayload> CODEC =
			StreamCodec.unit(INSTANCE);

	@Override
	public Type<ActivateBangleC2SPayload> type() {
		return ID;
	}
}
