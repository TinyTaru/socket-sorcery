package tinytaru.socketsorcery.net;

/**
 * Outcome of a server-side engraving attempt, sent back to the client so the Engraving Table screen
 * can give immediate feedback (success chime, or a reason the carve was rejected).
 */
public enum EngraveResult {
	OK,
	NOT_ENGRAVABLE,
	NO_CHISEL,
	NO_DUST,
	BAD_MODIFIERS,
	BAD_SYMBOL,
	/** A well-formed modifier the target pattern lists as incompatible. Appended last: the ordinal is the wire id. */
	INCOMPATIBLE_MODIFIER;

	private static final EngraveResult[] VALUES = values();

	public boolean success() {
		return this == OK;
	}

	public static EngraveResult byId(int id) {
		return id >= 0 && id < VALUES.length ? VALUES[id] : NOT_ENGRAVABLE;
	}
}
