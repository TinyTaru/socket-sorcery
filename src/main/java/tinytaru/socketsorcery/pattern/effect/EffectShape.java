package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * The geometry a visual component draws — the counterpart to {@link EffectTarget}, which picks entities.
 * A visual's whole job is to make an otherwise invisible radius or line legible, so these deliberately
 * mirror the shapes the targeting side already uses.
 */
public enum EffectShape implements StringRepresentable {
	/**
	 * The activation's hit location; on a miss, two blocks ahead of the wearer's eyes. That is the same
	 * rule {@code CastFeedback} uses, so a {@code point} visual lands where the cast puff already lands.
	 */
	POINT("point"),
	/** The wearer's feet, read at apply time — so a visual either side of a teleport marks both ends. */
	SELF("self"),
	/** A circle around the wearer at {@code radius} blocks, widened by Range. */
	RING("ring"),
	/** One position per cell of the aimed line — nothing without a Direction modifier. */
	LINE("line"),
	/** Scattered through a sphere of {@code radius} blocks around the {@code point} origin. */
	BURST("burst"),
	/**
	 * The circle over which Range shares a {@code self} effect with nearby allies. Empty on an engraving
	 * with no Range bonus, because no sharing happens there — see {@code EffectTargets.shareWithAllies}.
	 */
	ALLY_AURA("ally_aura");

	public static final Codec<EffectShape> CODEC = StringRepresentable.fromEnum(EffectShape::values);

	private final String name;

	EffectShape(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
