package tinytaru.socketsorcery.pattern;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.registry.ModItems;

/**
 * The registry of engravable patterns plus the gem/scroll compatibility tables.
 *
 * <p>Each pattern bundles a 5x5 symbol (what must be chiselled) and a {@link PatternEffect} with a
 * necklace (passive) and bangle (active) behaviour. Adding a pattern is a localised change here.
 */
public final class Patterns {

	/** How far the bangle's targeting ray reaches. */
	public static final double BANGLE_REACH = 6.0;

	private static final Map<ResourceLocation, Pattern> BY_ID = new LinkedHashMap<>();
	private static final Map<ResourceLocation, Integer> MODEL_INDEX = new LinkedHashMap<>();
	private static final Map<Item, Set<ResourceLocation>> GEM_PATTERNS = new LinkedHashMap<>();
	private static final Map<Item, ResourceLocation> SCROLL_PATTERN = new LinkedHashMap<>();

	public static final Pattern FIRE = register(new Pattern(
			SocketSorcery.id("fire"), "pattern.socket-sorcery.fire", 0xFF5722,
			Pattern.mask(
					"................",
					"................",
					"................",
					".......##.......",
					"......#..#......",
					"......#..#......",
					".....#....#.....",
					".....#....#.....",
					"....#......#....",
					"....#......#....",
					"...#........#...",
					"...#........#...",
					"...##########...",
					"................",
					"................",
					"................"),
			effect(
					(player, mods, index) -> player.addEffect(buff(MobEffects.FIRE_RESISTANCE, mods)),
					(player, target, mods, index) -> {
						if (target instanceof EntityHitResult hit) {
							hit.getEntity().igniteForSeconds((float) mods.magnitude(mods.duration(5)));
						} else if (target instanceof BlockHitResult hit && target.getType() == HitResult.Type.BLOCK) {
							Level level = player.level();
							BlockPos firePos = hit.getBlockPos().relative(hit.getDirection());
							if (level.getBlockState(firePos).isAir()) {
								level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
							}
						}
					})));

	public static final Pattern FROST = register(new Pattern(
			SocketSorcery.id("frost"), "pattern.socket-sorcery.frost", 0x4FC3F7,
			Pattern.mask(
					"................",
					"................",
					"................",
					"...##########...",
					"...#........#...",
					"....#......#....",
					"....#......#....",
					".....#....#.....",
					".....#....#.....",
					"......#..#......",
					"......#..#......",
					".......##.......",
					".......##.......",
					"................",
					"................",
					"................"),
			effect(
					(player, mods, index) -> {
						AABB area = player.getBoundingBox().inflate(mods.radius(5.0));
						for (Monster mob : player.level().getEntitiesOfClass(Monster.class, area, LivingEntity::isAlive)) {
							mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, mods.duration(60), mods.amp(1), false, true, true));
							mob.setTicksFrozen(Math.min(mob.getTicksFrozen() + 6, 140));
						}
					},
					(player, target, mods, index) -> {
						if (target instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) {
							living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, mods.duration(120), mods.amp(3), false, true, true));
							living.setTicksFrozen(mods.duration(220));
						}
					})));

	public static final Pattern HEALING = register(new Pattern(
			SocketSorcery.id("healing"), "pattern.socket-sorcery.healing", 0x66BB6A,
			Pattern.mask(
					"................",
					"................",
					"................",
					".......##.......",
					".......##.......",
					".......##.......",
					".......##.......",
					"...##########...",
					"...##########...",
					".......##.......",
					".......##.......",
					".......##.......",
					".......##.......",
					"................",
					"................",
					"................"),
			effect(
					(player, mods, index) -> player.addEffect(buff(MobEffects.REGENERATION, mods)),
					(player, target, mods, index) -> {
						LivingEntity healed = player;
						if (target instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) {
							healed = living;
						}
						healed.heal((float) mods.magnitude(6.0));
						healed.addEffect(new MobEffectInstance(MobEffects.REGENERATION, mods.duration(100), mods.amp(1), false, true, true));
					})));

	public static final Pattern LIGHTNING = register(new Pattern(
			SocketSorcery.id("lightning"), "pattern.socket-sorcery.lightning", 0xFFEB3B,
			Pattern.mask(
					"................",
					"................",
					"................",
					".........##.....",
					"........##......",
					".......##.......",
					"......##........",
					"....######......",
					"......##........",
					".....##.........",
					"....##..........",
					"...##...........",
					"...##...........",
					"................",
					"................",
					"................"),
			effect(
					(player, mods, index) -> player.addEffect(buff(MobEffects.MOVEMENT_SPEED, mods)),
					(player, target, mods, index) -> {
						if (target.getType() == HitResult.Type.MISS) {
							return;
						}
						Vec3 at = target.getLocation();
						LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(player.level());
						if (bolt != null) {
							bolt.moveTo(at.x, at.y, at.z);
							bolt.setCause(player);
							player.level().addFreshEntity(bolt);
						}
					})));

	public static final Pattern LEAPING = register(new Pattern(
			SocketSorcery.id("leaping"), "pattern.socket-sorcery.leaping", 0xBA68C8,
			Pattern.mask(
					"................",
					"................",
					"................",
					".......##.......",
					"......####......",
					".....######.....",
					"....########....",
					"...####..####...",
					"...##......##...",
					".......##.......",
					".......##.......",
					".......##.......",
					".......##.......",
					"................",
					"................",
					"................"),
			effect(
					(player, mods, index) -> player.addEffect(
							new MobEffectInstance(MobEffects.JUMP, mods.duration(60), mods.amp(1), true, false, true)),
					(player, target, mods, index) -> {
						Vec3 dash = player.getViewVector(1.0F).scale(mods.magnitude(1.6)).add(0.0, 0.35, 0.0);
						if (mods.hasAim()) {
							dash = dash.add(mods.worldAim(player).scale(1.2));
						}
						player.setDeltaMovement(dash);
						player.hurtMarked = true; // forces a velocity packet to the client
						player.fallDistance = 0.0F;
					})));

	/** Wires up the gem/scroll compatibility tables. Call after {@link ModItems} has registered. */
	public static void init() {
		gem(ModItems.RUBY, FIRE, LIGHTNING);
		gem(ModItems.SAPPHIRE, FROST, HEALING);
		gem(ModItems.PERIDOT, HEALING, LEAPING);
		gem(ModItems.AMETHYST, LIGHTNING, FROST);
		gem(ModItems.TOPAZ, LEAPING, FIRE);

		gem(ModItems.ENGRAVABLE_DIAMOND, FROST, HEALING);
		gem(ModItems.ENGRAVABLE_REDSTONE, FIRE, LIGHTNING);
		gem(ModItems.ENGRAVABLE_LAPIS, FROST, LEAPING);
		gem(ModItems.ENGRAVABLE_EMERALD, HEALING, LEAPING);
		gem(ModItems.ENGRAVABLE_QUARTZ, FIRE, FROST);

		scroll(ModItems.SCROLL_FIRE, FIRE);
		scroll(ModItems.SCROLL_FROST, FROST);
		scroll(ModItems.SCROLL_HEALING, HEALING);
		scroll(ModItems.SCROLL_LIGHTNING, LIGHTNING);
		scroll(ModItems.SCROLL_LEAPING, LEAPING);
	}

	public static Pattern get(ResourceLocation id) {
		return id == null ? null : BY_ID.get(id);
	}

	public static Collection<Pattern> all() {
		return BY_ID.values();
	}

	/** Patterns the given gem item is able to receive. */
	public static Set<ResourceLocation> patternsFor(Item gem) {
		return GEM_PATTERNS.getOrDefault(gem, Set.of());
	}

	public static boolean canEngrave(Item gem, ResourceLocation patternId) {
		return patternsFor(gem).contains(patternId);
	}

	/** The pattern a given scroll item teaches, or null if the item is not a scroll. */
	public static ResourceLocation patternForScroll(Item scroll) {
		return SCROLL_PATTERN.get(scroll);
	}

	/**
	 * Server-authoritative ray-trace from the player's eyes, preferring an entity hit over a block hit
	 * within reach. Returns a MISS {@link HitResult} when nothing is in range.
	 */
	public static HitResult raycast(ServerPlayer player, double reach) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = eye.add(look.x * reach, look.y * reach, look.z * reach);
		Level level = player.level();

		BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		Vec3 limit = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
		double maxDistSq = eye.distanceToSqr(limit);

		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, end, searchBox,
				e -> !e.isSpectator() && e.isPickable(), maxDistSq);
		return entityHit != null ? entityHit : blockHit;
	}

	private static Pattern register(Pattern pattern) {
		BY_ID.put(pattern.id(), pattern);
		MODEL_INDEX.put(pattern.id(), MODEL_INDEX.size() + 1);
		return pattern;
	}

	/**
	 * A stable 1-based index per pattern (in declaration order), used by the client item-model
	 * override predicate to pick the engraved gem texture. 0 means "no/unknown pattern".
	 */
	public static int modelIndex(ResourceLocation patternId) {
		return patternId == null ? 0 : MODEL_INDEX.getOrDefault(patternId, 0);
	}

	private static void gem(Item gem, Pattern... patterns) {
		Set<ResourceLocation> ids = new java.util.LinkedHashSet<>();
		for (Pattern pattern : patterns) {
			ids.add(pattern.id());
		}
		GEM_PATTERNS.put(gem, Set.copyOf(ids));
	}

	private static void scroll(Item scroll, Pattern pattern) {
		SCROLL_PATTERN.put(scroll, pattern.id());
	}

	/** A standard self-buff used by the passive necklace behaviours: short, refreshed, quiet. */
	private static MobEffectInstance buff(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
			EngraveMods mods) {
		return new MobEffectInstance(effect, mods.duration(60), mods.amp(0), true, false, true);
	}

	private static PatternEffect effect(NecklaceBehavior necklace, BangleBehavior bangle) {
		return new PatternEffect() {
			@Override
			public void onNecklaceTick(ServerPlayer player, EngraveMods mods, int socketIndex) {
				necklace.run(player, mods, socketIndex);
			}

			@Override
			public void onBangleActivate(ServerPlayer player, HitResult target, EngraveMods mods, int socketIndex) {
				bangle.run(player, target, mods, socketIndex);
			}
		};
	}

	@FunctionalInterface
	private interface NecklaceBehavior {
		void run(ServerPlayer player, EngraveMods mods, int socketIndex);
	}

	@FunctionalInterface
	private interface BangleBehavior {
		void run(ServerPlayer player, HitResult target, EngraveMods mods, int socketIndex);
	}

	private Patterns() {
	}
}
