package tinytaru.socketsorcery.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fires when a player completes an engraving. Conditions can require a specific {@code pattern} id
 * (so "master engraver" can list one criterion per pattern) and/or {@code require_modified} (the
 * engraving carried at least one modifier).
 */
public class EngravePatternTrigger extends SimpleCriterionTrigger<EngravePatternTrigger.Instance> {

	@Override
	public Codec<Instance> codec() {
		return Instance.CODEC;
	}

	public void trigger(ServerPlayer player, Identifier pattern, boolean modified) {
		this.trigger(player, instance -> instance.matches(pattern, modified));
	}

	public record Instance(Optional<ContextAwarePredicate> player, Optional<Identifier> pattern,
			boolean requireModified) implements SimpleCriterionTrigger.SimpleInstance {

		public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
				Identifier.CODEC.optionalFieldOf("pattern").forGetter(Instance::pattern),
				Codec.BOOL.optionalFieldOf("require_modified", false).forGetter(Instance::requireModified)
		).apply(i, Instance::new));

		public boolean matches(Identifier engravedPattern, boolean modified) {
			if (pattern.isPresent() && !pattern.get().equals(engravedPattern)) {
				return false;
			}
			return !requireModified || modified;
		}
	}
}
