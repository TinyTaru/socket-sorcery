package tinytaru.socketsorcery.net;

import java.util.function.IntFunction;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * One chisel stroke at the Engraving Table. The minigame is server-authoritative and saved as it is
 * played: every click sends one of these, the server cuts the gem in the table and writes the result
 * straight back onto the item, so a carve is never lost by closing the screen or pulling the gem out.
 *
 * @param cell   the grid cell struck ({@code row * GRID + col}); ignored by {@link Action#CLEAR}
 * @param action how the stroke changes that cell
 */
public record ChiselC2SPayload(int cell, Action action) implements CustomPacketPayload {

	/** What a stroke does to the struck cell. */
	public enum Action {
		/** Cut the cell one step deeper (bare → carved → deep), spending chisel durability. */
		DEEPEN,
		/** Ease the cut back one step, spending a gem dust. */
		EASE,
		/** Ease every cut on the gem back to bare, spending one dust per step. */
		CLEAR;

		private static final IntFunction<Action> BY_ID =
				ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

		public static final StreamCodec<ByteBuf, Action> STREAM_CODEC =
				ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
	}

	public static final Type<ChiselC2SPayload> ID = new Type<>(SocketSorcery.id("chisel"));

	public static final StreamCodec<ByteBuf, ChiselC2SPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ChiselC2SPayload::cell,
			Action.STREAM_CODEC, ChiselC2SPayload::action,
			ChiselC2SPayload::new);

	@Override
	public Type<ChiselC2SPayload> type() {
		return ID;
	}
}
