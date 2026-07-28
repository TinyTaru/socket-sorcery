package tinytaru.socketsorcery.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.pattern.Pattern;

/**
 * Sent when the player finishes chiselling at the Engraving Table. Carries two cell masks, each
 * packed one bit per cell into {@link Pattern#WORDS} 64-bit words:
 * <ul>
 *   <li>{@code carved} — cells chiselled at least once (depth ≥ 1).</li>
 *   <li>{@code deep} — cells chiselled twice (depth 2), a subset of {@code carved}; these form the
 *       modifier set.</li>
 * </ul>
 * {@code downgrades} counts how many times the player eased a cut back (right-click) during this
 * chiselling session — each one costs a matching gem dust, charged by the server alongside it
 * re-validating both masks against the scroll's pattern.
 */
public record FinishEngravingC2SPayload(long[] carved, long[] deep, int downgrades) implements CustomPacketPayload {

	public static final Type<FinishEngravingC2SPayload> ID =
			new Type<>(SocketSorcery.id("finish_engraving"));

	public static final StreamCodec<RegistryFriendlyByteBuf, FinishEngravingC2SPayload> CODEC =
			new StreamCodec<>() {
				@Override
				public FinishEngravingC2SPayload decode(RegistryFriendlyByteBuf buf) {
					long[] carved = readMask(buf);
					long[] deep = readMask(buf);
					int downgrades = buf.readVarInt();
					return new FinishEngravingC2SPayload(carved, deep, downgrades);
				}

				@Override
				public void encode(RegistryFriendlyByteBuf buf, FinishEngravingC2SPayload payload) {
					writeMask(buf, payload.carved());
					writeMask(buf, payload.deep());
					buf.writeVarInt(payload.downgrades());
				}

				private long[] readMask(RegistryFriendlyByteBuf buf) {
					long[] words = new long[Pattern.WORDS];
					for (int i = 0; i < Pattern.WORDS; i++) {
						words[i] = buf.readLong();
					}
					return words;
				}

				private void writeMask(RegistryFriendlyByteBuf buf, long[] words) {
					for (int i = 0; i < Pattern.WORDS; i++) {
						buf.writeLong(i < words.length ? words[i] : 0L);
					}
				}
			};

	@Override
	public Type<FinishEngravingC2SPayload> type() {
		return ID;
	}
}
