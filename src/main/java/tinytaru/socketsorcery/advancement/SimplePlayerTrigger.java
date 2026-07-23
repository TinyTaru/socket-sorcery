package tinytaru.socketsorcery.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * A criterion that simply fires for a player when some event happens, with no extra conditions beyond
 * the optional player predicate — the same shape as vanilla's {@code PlayerTrigger}. Reused for the
 * "equipped an accessory" and "activated an ability" advancements.
 */
public class SimplePlayerTrigger extends SimpleCriterionTrigger<SimplePlayerTrigger.Instance> {

	@Override
	public Codec<Instance> codec() {
		return Instance.CODEC;
	}

	public void trigger(ServerPlayer player) {
		this.trigger(player, instance -> true);
	}

	public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player)
		).apply(i, Instance::new));
	}
}
