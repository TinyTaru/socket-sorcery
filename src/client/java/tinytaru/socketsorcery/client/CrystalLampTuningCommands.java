package tinytaru.socketsorcery.client;

import java.util.Locale;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import tinytaru.socketsorcery.config.SocketSorceryConfig;

/**
 * Client-side live controls for tuning Crystal Lamp projection lighting while looking at it.
 * Every change is applied immediately and persisted to config/socket-sorcery.json.
 */
public final class CrystalLampTuningCommands {

	private CrystalLampTuningCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal("lamplight")
					.executes(context -> show(context.getSource()));

			root.then(ClientCommands.literal("show")
					.executes(context -> show(context.getSource())));
			root.then(ClientCommands.literal("help")
					.executes(context -> help(context.getSource())));
			root.then(ClientCommands.literal("copy")
					.executes(context -> copy(context.getSource())));
			root.then(ClientCommands.literal("save")
					.executes(context -> save(context.getSource(), "Crystal Lamp values saved.")));
			root.then(ClientCommands.literal("reset")
					.executes(context -> reset(context.getSource())));

			// Friendly high-level controls. Blur 0 is crisp; blur 1 restores the reference soft edge.
			root.then(valueCommand("blur", 0.0, 8.0, (config, scale) -> {
				config.lampLightBlurBase = SocketSorceryConfig.LAMP_LIGHT_BLUR_REFERENCE_BASE * scale;
				config.lampLightBlurPerBlock = SocketSorceryConfig.LAMP_LIGHT_BLUR_REFERENCE_PER_BLOCK * scale;
			}));
			root.then(valueCommand("brightness", 0.0, 2.0,
					(config, value) -> config.lampLightBrightness = value));
			root.then(valueCommand("falloff", 0.0, 8.0, (config, scale) -> {
				config.lampLightLinearFalloff = SocketSorceryConfig.DEFAULT_LAMP_LIGHT_LINEAR_FALLOFF * scale;
				config.lampLightQuadraticFalloff = SocketSorceryConfig.DEFAULT_LAMP_LIGHT_QUADRATIC_FALLOFF * scale;
			}));

			// Raw controls for exact fine tuning.
			root.then(valueCommand("blurbase", 0.0, 0.05,
					(config, value) -> config.lampLightBlurBase = value));
			root.then(valueCommand("blurdistance", 0.0, 0.02,
					(config, value) -> config.lampLightBlurPerBlock = value));
			root.then(valueCommand("wideblur", 0.0, 1.0,
					(config, value) -> config.lampLightWideBlurWeight = value));
			root.then(valueCommand("edgefill", 0.0, 1.0,
					(config, value) -> config.lampLightCenterFloor = value));
			root.then(valueCommand("samples", 0.25, 8.0,
					(config, value) -> config.lampLightSamplesPerPatternCell = value));
			root.then(valueCommand("cutoff", 0.0, 0.1,
					(config, value) -> config.lampLightMinOpacity = value));
			root.then(valueCommand("cullmargin", 0.0, 0.5,
					(config, value) -> config.lampLightCullMarginFactor = value));
			root.then(valueCommand("cullbase", 0.0, 1.0,
					(config, value) -> config.lampLightCullMarginBase = value));
			root.then(valueCommand("linearfalloff", 0.0, 1.0,
					(config, value) -> config.lampLightLinearFalloff = value));
			root.then(valueCommand("quadraticfalloff", 0.0, 1.0,
					(config, value) -> config.lampLightQuadraticFalloff = value));
			root.then(valueCommand("rangefade", 0.0, 0.99,
					(config, value) -> config.lampLightRangeFadeStart = value));
			root.then(valueCommand("shadowsoftness", 0.0, 0.25,
					(config, value) -> config.lampLightShadowSoftness = value));
			root.then(integerCommand("shadowsamples", 1, 3,
					(config, value) -> config.lampLightShadowSamples = value));

			dispatcher.register(root);
		});
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> valueCommand(String name, double min,
			double max, ObjDoubleConsumer<SocketSorceryConfig> setter) {
		return ClientCommands.literal(name)
				.then(ClientCommands.argument("value", DoubleArgumentType.doubleArg(min, max))
						.executes(context -> {
							double value = DoubleArgumentType.getDouble(context, "value");
							SocketSorceryConfig config = SocketSorceryConfig.get();
							setter.accept(config, value);
							config.validatePostLoad();
							return save(context.getSource(), name + " = " + format(value));
						}));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> integerCommand(String name, int min, int max,
			ObjIntConsumer<SocketSorceryConfig> setter) {
		return ClientCommands.literal(name)
				.then(ClientCommands.argument("value", IntegerArgumentType.integer(min, max))
						.executes(context -> {
							int value = IntegerArgumentType.getInteger(context, "value");
							SocketSorceryConfig config = SocketSorceryConfig.get();
							setter.accept(config, value);
							config.validatePostLoad();
							return save(context.getSource(), name + " = " + value);
						}));
	}

	private static int save(FabricClientCommandSource source, String feedback) {
		AutoConfig.getConfigHolder(SocketSorceryConfig.class).save();
		CrystalLampRenderer.invalidateTuning();
		source.sendFeedback(Component.literal(feedback + "  [applied + saved]"));
		return 1;
	}

	private static int reset(FabricClientCommandSource source) {
		SocketSorceryConfig config = SocketSorceryConfig.get();
		config.resetLampLighting();
		config.validatePostLoad();
		return save(source, "Crystal Lamp lighting reset to defaults.");
	}

	private static int show(FabricClientCommandSource source) {
		SocketSorceryConfig config = SocketSorceryConfig.get();
		double blurScale = averageRatio(config.lampLightBlurBase,
				SocketSorceryConfig.LAMP_LIGHT_BLUR_REFERENCE_BASE,
				config.lampLightBlurPerBlock,
				SocketSorceryConfig.LAMP_LIGHT_BLUR_REFERENCE_PER_BLOCK);
		double falloffScale = averageRatio(config.lampLightLinearFalloff,
				SocketSorceryConfig.DEFAULT_LAMP_LIGHT_LINEAR_FALLOFF,
				config.lampLightQuadraticFalloff,
				SocketSorceryConfig.DEFAULT_LAMP_LIGHT_QUADRATIC_FALLOFF);

		String message = "Crystal Lamp lighting (live + saved)\n"
				+ "brightness=" + format(config.lampLightBrightness)
				+ "  blur≈" + format(blurScale)
				+ "  falloff≈" + format(falloffScale) + "\n"
				+ "blurbase=" + format(config.lampLightBlurBase)
				+ "  blurdistance=" + format(config.lampLightBlurPerBlock)
				+ "  wideblur=" + format(config.lampLightWideBlurWeight) + "\n"
				+ "edgefill=" + format(config.lampLightCenterFloor)
				+ "  samples=" + format(config.lampLightSamplesPerPatternCell) + "\n"
				+ "cutoff=" + format(config.lampLightMinOpacity)
				+ "  cullmargin=" + format(config.lampLightCullMarginFactor)
				+ "  cullbase=" + format(config.lampLightCullMarginBase) + "\n"
				+ "linearfalloff=" + format(config.lampLightLinearFalloff)
				+ "  quadraticfalloff=" + format(config.lampLightQuadraticFalloff)
				+ "  rangefade=" + format(config.lampLightRangeFadeStart) + "\n"
				+ "shadowsoftness=" + format(config.lampLightShadowSoftness)
				+ "  shadowsamples=" + config.lampLightShadowSamples
				+ "\nUse /lamplight help for commands.";
		source.sendFeedback(Component.literal(message));
		return 1;
	}

	private static int help(FabricClientCommandSource source) {
		source.sendFeedback(Component.literal(
				"Crystal Lamp live tuning\n"
				+ "/lamplight blur <0-8> - overall blur (0 = crisp default, 1 = soft reference)\n"
				+ "/lamplight brightness <0-2> - light strength\n"
				+ "/lamplight falloff <0-8> - distance dimming (1 = default)\n"
				+ "Fine controls: blurbase, blurdistance, wideblur, edgefill, samples, cutoff, cullmargin, cullbase, "
				+ "linearfalloff, quadraticfalloff, rangefade, shadowsoftness, shadowsamples\n"
				+ "/lamplight show | copy | reset\n"
				+ "Every changed value is saved immediately to config/socket-sorcery.json."));
		return 1;
	}

	private static int copy(FabricClientCommandSource source) {
		SocketSorceryConfig config = SocketSorceryConfig.get();
		String values = "Crystal Lamp tuning:\n"
				+ "lampLightBrightness = " + format(config.lampLightBrightness) + ";\n"
				+ "lampLightBlurBase = " + format(config.lampLightBlurBase) + ";\n"
				+ "lampLightBlurPerBlock = " + format(config.lampLightBlurPerBlock) + ";\n"
				+ "lampLightWideBlurWeight = " + format(config.lampLightWideBlurWeight) + ";\n"
				+ "lampLightCenterFloor = " + format(config.lampLightCenterFloor) + ";\n"
				+ "lampLightSamplesPerPatternCell = " + format(config.lampLightSamplesPerPatternCell) + ";\n"
				+ "lampLightMinOpacity = " + format(config.lampLightMinOpacity) + ";\n"
				+ "lampLightCullMarginFactor = " + format(config.lampLightCullMarginFactor) + ";\n"
				+ "lampLightCullMarginBase = " + format(config.lampLightCullMarginBase) + ";\n"
				+ "lampLightLinearFalloff = " + format(config.lampLightLinearFalloff) + ";\n"
				+ "lampLightQuadraticFalloff = " + format(config.lampLightQuadraticFalloff) + ";\n"
				+ "lampLightRangeFadeStart = " + format(config.lampLightRangeFadeStart) + ";\n"
				+ "lampLightShadowSoftness = " + format(config.lampLightShadowSoftness) + ";\n"
				+ "lampLightShadowSamples = " + config.lampLightShadowSamples + ";";
		source.getClient().keyboardHandler.setClipboard(values);
		source.sendFeedback(Component.literal("Copied current Crystal Lamp tuning values to the clipboard."));
		return 1;
	}

	private static double averageRatio(double a, double aDefault, double b, double bDefault) {
		return ((aDefault == 0.0 ? 0.0 : a / aDefault) + (bDefault == 0.0 ? 0.0 : b / bDefault)) * 0.5;
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", ".0");
	}
}
