package tinytaru.socketsorcery.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.entity.SpikeEntity;

public final class ModEntities {

	/**
	 * The purely decorative spike raised by {@code socket-sorcery:spawn_spikes}. Dimensions are a culling
	 * box only — a bare {@link Entity} has no collision, so nothing is ever shoved by one.
	 *
	 * <p>{@code noSave()} is deliberate: a sub-second visual written into chunk NBT would come back frozen
	 * after a reload, because nothing re-fires the eruption event. {@code noSummon()} is deliberately
	 * <em>not</em> set — {@code /summon socket-sorcery:spike} is the cheapest way to iterate on the model,
	 * and a damageless entity that deletes itself is harmless in a player's hands.
	 */
	public static final EntityType<SpikeEntity> SPIKE = register("spike",
			EntityType.Builder.<SpikeEntity>of(SpikeEntity::new, MobCategory.MISC)
					.noLootTable()
					.noSave()
					.sized(0.5F, 1.0F)
					.clientTrackingRange(6)
					.updateInterval(2));

	/** As with blocks and items, the registry key must be threaded through construction. */
	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, SocketSorcery.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
	}

	private ModEntities() {
	}
}
