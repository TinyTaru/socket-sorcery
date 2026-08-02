package tinytaru.socketsorcery.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import tinytaru.socketsorcery.registry.ModEntities;

/**
 * A spike that erupts from the ground, holds, and sinks away — the visible body of an effect that would
 * otherwise be an invisible radius or line. Spawned by {@code socket-sorcery:spawn_spikes}.
 *
 * <p><strong>This entity deals no damage and must never be given any.</strong> Damage stays with the
 * pattern's own {@code socket-sorcery:damage} component, which is what respects the Power modifier and
 * the {@code monsters} filter. Vanilla's {@link net.minecraft.world.entity.projectile.EvokerFangs} —
 * the obvious thing to reuse here, and the model for this class's lifecycle — was rejected precisely
 * because its 6 damage is hardcoded, hits anything not allied to the caster, and honours neither knob.
 * Wiring damage in here would reintroduce exactly that problem.
 *
 * <p>Colour, scale and lifetime are synced because the pattern JSON chooses them and the renderer needs
 * them; a single greyscale texture is tinted per entity, so ice shards and stone spikes are one asset.
 * The eruption itself is announced with an entity event rather than derived from {@code tickCount},
 * because a client's tick count starts when its tracker picks the entity up — a tick or more after the
 * spawn, varying with latency — which would visibly scramble the stagger across a ring.
 */
public class SpikeEntity extends Entity {

	/** Ticks the spike takes to rise clear of the ground. */
	public static final int RISE_TICKS = 5;
	/** Ticks of sink-and-fade at the end of life. */
	public static final int FADE_TICKS = 5;
	public static final int DEFAULT_LIFE_TICKS = 22;

	/** Broadcast the tick the spike breaks the surface; starts the client's rise. */
	private static final byte EVENT_ERUPT = 4;

	private static final EntityDataAccessor<Integer> DATA_COLOR =
			SynchedEntityData.defineId(SpikeEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_SCALE =
			SynchedEntityData.defineId(SpikeEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_LIFE =
			SynchedEntityData.defineId(SpikeEntity.class, EntityDataSerializers.INT);

	private int warmupDelayTicks;
	private int lifeTicks = DEFAULT_LIFE_TICKS;
	private boolean erupted;

	/** The client tick the erupt event landed on, or -1 while the spike is still underground. */
	private int clientEruptTick = -1;

	public SpikeEntity(EntityType<? extends SpikeEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public SpikeEntity(Level level, double x, double y, double z, float yRot,
			int warmupDelayTicks, int lifeTicks, int color, float scale) {
		this(ModEntities.SPIKE, level);
		this.warmupDelayTicks = Math.max(0, warmupDelayTicks);
		this.lifeTicks = Math.max(1, lifeTicks);
		this.setYRot(yRot);
		this.setPos(x, y, z);
		this.entityData.set(DATA_COLOR, 0xFF000000 | (color & 0x00FFFFFF));
		this.entityData.set(DATA_SCALE, scale);
		this.entityData.set(DATA_LIFE, this.lifeTicks);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_COLOR, 0xFFFFFFFF);
		builder.define(DATA_SCALE, 1.0F);
		builder.define(DATA_LIFE, DEFAULT_LIFE_TICKS);
	}

	/** Never persisted — the type is built {@code noSave()}, so there is nothing to read or write. */
	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	/** Immune to everything: it is scenery with a timer, not a combatant. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public void tick() {
		if (level().isClientSide()) {
			return; // the client's whole animation is derived from the erupt event; nothing to tick
		}
		if (!erupted) {
			if (warmupDelayTicks-- > 0) {
				return;
			}
			erupted = true;
			level().broadcastEntityEvent(this, EVENT_ERUPT);
		}
		if (--lifeTicks <= 0) {
			discard();
		}
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == EVENT_ERUPT) {
			clientEruptTick = tickCount;
		} else {
			super.handleEntityEvent(id);
		}
	}

	/** Ticks since the spike broke the surface, or -1 while it is still underground. */
	private float sinceErupt(float partialTick) {
		return clientEruptTick < 0 ? -1.0F : tickCount - clientEruptTick + partialTick;
	}

	/** How far the spike stands out of the ground: 0 before eruption, ramping to 1 over {@link #RISE_TICKS}. */
	public float riseProgress(float partialTick) {
		float age = sinceErupt(partialTick);
		return age < 0.0F ? 0.0F : Mth.clamp(age / RISE_TICKS, 0.0F, 1.0F);
	}

	/** Full opacity until the last {@link #FADE_TICKS} of life, then out. */
	public float alpha(float partialTick) {
		float age = sinceErupt(partialTick);
		if (age < 0.0F) {
			return 0.0F;
		}
		float remaining = entityData.get(DATA_LIFE) - age;
		return remaining >= FADE_TICKS ? 1.0F : Mth.clamp(remaining / FADE_TICKS, 0.0F, 1.0F);
	}

	/** The spike's ARGB tint, applied to the shared greyscale texture. */
	public int color() {
		return entityData.get(DATA_COLOR);
	}

	public float scale() {
		return entityData.get(DATA_SCALE);
	}
}
