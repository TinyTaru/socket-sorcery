package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

/** Which living entities an {@code area}-targeted component selects. Never selects the wearer. */
public enum EffectFilter implements StringRepresentable {
	/** Hostile monsters only (the default — matches every built-in area effect). */
	MONSTERS("monsters"),
	/** Any living entity except the wearer. */
	LIVING("living");

	public static final Codec<EffectFilter> CODEC = StringRepresentable.fromEnum(EffectFilter::values);

	private final String name;

	EffectFilter(String name) {
		this.name = name;
	}

	public Class<? extends LivingEntity> entityClass() {
		return this == MONSTERS ? Monster.class : LivingEntity.class;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
