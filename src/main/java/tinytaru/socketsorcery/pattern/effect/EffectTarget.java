package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** Who a {@code PatternEffectComponent} applies to. */
public enum EffectTarget implements StringRepresentable {
	/** The wearer. */
	SELF("self"),
	/** The living entity the activation hit; nothing on a block hit or miss. */
	HIT_ENTITY("hit_entity"),
	/** The living entity hit, or the wearer when there is none. */
	HIT_ENTITY_OR_SELF("hit_entity_or_self"),
	/** All matching living entities within {@code radius} blocks of the wearer. */
	AREA("area");

	public static final Codec<EffectTarget> CODEC = StringRepresentable.fromEnum(EffectTarget::values);

	private final String name;

	EffectTarget(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
