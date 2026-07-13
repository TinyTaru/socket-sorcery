package tinytaru.socketsorcery.pattern.effect;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** An optional gate deciding whether a {@code PatternEffectComponent} runs for a given activation. */
public enum EffectWhen implements StringRepresentable {
	ALWAYS("always"),
	/** Only when the activation hit a living entity. */
	HIT_ENTITY("hit_entity"),
	/** Only when the activation did NOT hit a living entity. */
	NO_HIT_ENTITY("no_hit_entity"),
	/** Only when the activation hit a block. */
	HIT_BLOCK("hit_block"),
	/** Only when the activation hit anything at all (entity or block). */
	HIT_ANY("hit_any");

	public static final Codec<EffectWhen> CODEC = StringRepresentable.fromEnum(EffectWhen::values);

	private final String name;

	EffectWhen(String name) {
		this.name = name;
	}

	public boolean matches(HitResult target) {
		boolean hitLiving = target instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity;
		return switch (this) {
			case ALWAYS -> true;
			case HIT_ENTITY -> hitLiving;
			case NO_HIT_ENTITY -> !hitLiving;
			case HIT_BLOCK -> target.getType() == HitResult.Type.BLOCK;
			case HIT_ANY -> target.getType() != HitResult.Type.MISS;
		};
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
