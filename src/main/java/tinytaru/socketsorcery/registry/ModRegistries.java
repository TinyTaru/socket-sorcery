package tinytaru.socketsorcery.registry;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.pattern.Modifier;
import tinytaru.socketsorcery.pattern.ModifierCellRule;
import tinytaru.socketsorcery.pattern.Pattern;
import tinytaru.socketsorcery.pattern.PatternEffectComponent;
import tinytaru.socketsorcery.pattern.effect.DamageComponent;
import tinytaru.socketsorcery.pattern.effect.FlameLineComponent;
import tinytaru.socketsorcery.pattern.effect.FreezeComponent;
import tinytaru.socketsorcery.pattern.effect.HealComponent;
import tinytaru.socketsorcery.pattern.effect.IgniteComponent;
import tinytaru.socketsorcery.pattern.effect.LaunchComponent;
import tinytaru.socketsorcery.pattern.effect.MobEffectComponent;
import tinytaru.socketsorcery.pattern.effect.SummonLightningComponent;
import tinytaru.socketsorcery.pattern.effect.TeleportComponent;
import tinytaru.socketsorcery.pattern.rule.CenterBlockRule;
import tinytaru.socketsorcery.pattern.rule.ExtensionRule;
import tinytaru.socketsorcery.pattern.rule.LeftRightRule;
import tinytaru.socketsorcery.pattern.rule.TopBottomRule;

/**
 * The mod's registries for data-driven content.
 *
 * <ul>
 *   <li>{@link #PATTERN} / {@link #MODIFIER} — <b>dynamic, datapack-loaded, synced</b>: definitions
 *       load from {@code data/&lt;pack&gt;/socket-sorcery/pattern/*.json} (and {@code .../modifier/})
 *       at server start and sync to clients automatically, exactly like vanilla enchantments.</li>
 *   <li>{@link #EFFECT_TYPE} / {@link #CELL_RULE_TYPE} — <b>static, Java-extensible</b>: the codec
 *       vocabularies the JSON dispatches on. Other mods may register additional types during their
 *       own initializer to extend what pattern/modifier JSON can express.</li>
 * </ul>
 */
public final class ModRegistries {

	// --- dynamic registry keys (datapack content) ---

	public static final ResourceKey<Registry<Pattern>> PATTERN =
			ResourceKey.createRegistryKey(SocketSorcery.id("pattern"));
	public static final ResourceKey<Registry<Modifier>> MODIFIER =
			ResourceKey.createRegistryKey(SocketSorcery.id("modifier"));

	// --- static type registries (Java-extensible codec vocabularies) ---

	public static final ResourceKey<Registry<MapCodec<? extends PatternEffectComponent>>> EFFECT_TYPE_KEY =
			ResourceKey.createRegistryKey(SocketSorcery.id("pattern_effect_type"));
	public static final Registry<MapCodec<? extends PatternEffectComponent>> EFFECT_TYPE =
			FabricRegistryBuilder.create(EFFECT_TYPE_KEY).buildAndRegister();

	public static final ResourceKey<Registry<MapCodec<? extends ModifierCellRule>>> CELL_RULE_TYPE_KEY =
			ResourceKey.createRegistryKey(SocketSorcery.id("modifier_cell_rule_type"));
	public static final Registry<MapCodec<? extends ModifierCellRule>> CELL_RULE_TYPE =
			FabricRegistryBuilder.create(CELL_RULE_TYPE_KEY).buildAndRegister();

	public static void init() {
		Registry.register(EFFECT_TYPE, SocketSorcery.id("mob_effect"), MobEffectComponent.CODEC);
		Registry.register(EFFECT_TYPE, SocketSorcery.id("damage"), DamageComponent.CODEC);
		Registry.register(EFFECT_TYPE, SocketSorcery.id("heal"), HealComponent.CODEC);
		Registry.register(EFFECT_TYPE, SocketSorcery.id("freeze"), FreezeComponent.CODEC);
		Registry.register(EFFECT_TYPE, SocketSorcery.id("ignite"), IgniteComponent.CODEC);
		Registry.register(EFFECT_TYPE, SocketSorcery.id("flame_line"), FlameLineComponent.CODEC);
		Registry.register(EFFECT_TYPE, SocketSorcery.id("launch"), LaunchComponent.CODEC);
		Registry.register(EFFECT_TYPE, SocketSorcery.id("teleport"), TeleportComponent.CODEC);
		Registry.register(EFFECT_TYPE, SocketSorcery.id("summon_lightning"), SummonLightningComponent.CODEC);

		Registry.register(CELL_RULE_TYPE, SocketSorcery.id("center_block"), CenterBlockRule.CODEC);
		Registry.register(CELL_RULE_TYPE, SocketSorcery.id("top_bottom"), TopBottomRule.CODEC);
		Registry.register(CELL_RULE_TYPE, SocketSorcery.id("left_right"), LeftRightRule.CODEC);
		Registry.register(CELL_RULE_TYPE, SocketSorcery.id("extension"), ExtensionRule.CODEC);

		// Datapack-loaded, synced to clients on join — the enchantment model. Loads at server start;
		// /reload does NOT re-read these (re-enter the world after editing JSON).
		DynamicRegistries.registerSynced(PATTERN, Pattern.CODEC);
		DynamicRegistries.registerSynced(MODIFIER, Modifier.CODEC);
	}

	private ModRegistries() {
	}
}
