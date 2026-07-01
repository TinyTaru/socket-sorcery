package tinytaru.socketsorcery.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import tinytaru.socketsorcery.SocketSorcery;

/**
 * Custom sound events for the four ability casts that have no clean single vanilla match
 * (fire / frost / heal / leap). Everything else in the mod plays a vanilla {@code SoundEvents.*}
 * constant directly, so those need no registration here.
 *
 * <p>These events are given stable ids now; their actual audio is aliased to vanilla sound events
 * in {@code sounds.json}, so no {@code .ogg} assets are required yet. Dropping bespoke {@code .ogg}
 * files in later only changes that JSON, never the code that plays these ids.
 */
public final class ModSounds {

	public static final SoundEvent CAST_FIRE = register("cast_fire");
	public static final SoundEvent CAST_FROST = register("cast_frost");
	public static final SoundEvent CAST_HEAL = register("cast_heal");
	public static final SoundEvent CAST_LEAP = register("cast_leap");

	/** Forces class load so the static events register. Call once during mod init. */
	public static void init() {
	}

	private static SoundEvent register(String path) {
		ResourceLocation id = SocketSorcery.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	private ModSounds() {
	}
}
