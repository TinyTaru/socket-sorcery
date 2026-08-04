package tinytaru.socketsorcery.pattern;

import net.minecraft.util.StringRepresentable;

/** Events that may be carved into a ring. */
public enum RingTrigger implements StringRepresentable {
	ON_HIT("on_hit"), ON_ATTACK("on_attack"), ON_FALLING("on_falling"), ON_KILL("on_kill"),
	HP_THRESHOLD("hp_threshold"), IN_RADIUS("in_radius");

	public static final net.minecraft.util.StringRepresentable.EnumCodec<RingTrigger> CODEC =
			StringRepresentable.fromEnum(RingTrigger::values);
	private final String name;

	RingTrigger(String name) { this.name = name; }

	@Override
	public String getSerializedName() { return name; }
}
