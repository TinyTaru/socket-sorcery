package tinytaru.socketsorcery.pattern;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tinytaru.socketsorcery.registry.ModRegistries;

/**
 * Lookup helpers over the synced {@code socket-sorcery:pattern} dynamic registry. Pattern
 * definitions themselves live in datapack JSON (see {@code ModRegistries}); these helpers are
 * null-tolerant so callers degrade gracefully when a registry isn't available (e.g. tooltip
 * contexts without registries) or an id references a removed datapack.
 */
public final class Patterns {

	/** The pattern holder for an id, or null if unavailable. */
	public static Holder.Reference<Pattern> get(HolderLookup.Provider registries, ResourceLocation id) {
		if (registries == null || id == null) {
			return null;
		}
		return registries.lookup(ModRegistries.PATTERN)
				.flatMap(lookup -> lookup.get(ResourceKey.create(ModRegistries.PATTERN, id)))
				.orElse(null);
	}

	/** Every registered pattern (empty when registries are unavailable). */
	public static Stream<Holder.Reference<Pattern>> all(HolderLookup.Provider registries) {
		if (registries == null) {
			return Stream.empty();
		}
		return registries.lookup(ModRegistries.PATTERN)
				.map(HolderLookup::listElements)
				.orElse(Stream.empty());
	}

	/** Patterns the given gem item is able to receive. */
	public static Set<ResourceLocation> patternsFor(HolderLookup.Provider registries, Item gem) {
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(gem);
		Set<ResourceLocation> ids = new LinkedHashSet<>();
		all(registries).forEach(holder -> {
			if (holder.value().gems().contains(itemId)) {
				ids.add(holder.key().location());
			}
		});
		return ids;
	}

	public static boolean canEngrave(HolderLookup.Provider registries, Item gem, ResourceLocation patternId) {
		Holder.Reference<Pattern> pattern = get(registries, patternId);
		return pattern != null && pattern.value().gems().contains(BuiltInRegistries.ITEM.getKey(gem));
	}

	/** True if the item appears in ANY pattern's gems list — the "can go in the gem slot" gate. */
	public static boolean isEngravableGem(HolderLookup.Provider registries, Item item) {
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
		return all(registries).anyMatch(holder -> holder.value().gems().contains(itemId));
	}

	/** The pattern the given scroll item teaches, or null if no pattern claims it. */
	public static Holder.Reference<Pattern> forScroll(HolderLookup.Provider registries, Item scroll) {
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(scroll);
		return all(registries)
				.filter(holder -> holder.value().scroll().map(itemId::equals).orElse(false))
				.findFirst()
				.orElse(null);
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

	private Patterns() {
	}
}
