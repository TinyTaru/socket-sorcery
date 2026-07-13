package tinytaru.socketsorcery.pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import tinytaru.socketsorcery.registry.ModRegistries;

/**
 * One data-driven building block of a pattern's behaviour, dispatched by type id from the
 * {@code socket-sorcery:pattern_effect_type} registry (which other mods may extend at init).
 *
 * <p>The same component type serves both roles: a pattern's {@code necklace_effects} run on the
 * worn-necklace tick with a synthetic MISS target, and its {@code bangle_effects} run on bangle
 * activation / ring retaliation against the real look/attacker target. {@code mods} carries the
 * engraving's modifier adjustments ({@link EngraveMods#NONE} for a base engraving).
 */
public interface PatternEffectComponent {

	Codec<PatternEffectComponent> CODEC = ModRegistries.EFFECT_TYPE.byNameCodec()
			.dispatch(PatternEffectComponent::codec, mapCodec -> mapCodec);

	/** This component's serializer, as registered in {@link ModRegistries#EFFECT_TYPE}. */
	MapCodec<? extends PatternEffectComponent> codec();

	/** Runs the behaviour. Server-side only. */
	void apply(ServerPlayer player, HitResult target, EngraveMods mods);
}
