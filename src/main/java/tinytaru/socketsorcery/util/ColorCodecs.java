package tinytaru.socketsorcery.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Codec for RGB colours that accepts either a {@code "#RRGGBB"} hex string (the friendly datapack
 * form) or a raw integer, and always serializes back to the hex form.
 */
public final class ColorCodecs {

	private static final Codec<Integer> HEX = Codec.STRING.comapFlatMap(ColorCodecs::parseHex, ColorCodecs::formatHex);

	public static final Codec<Integer> RGB = Codec.either(HEX, Codec.INT)
			.xmap(either -> either.map(i -> i, i -> i), Either::left);

	private static DataResult<Integer> parseHex(String value) {
		String digits = value.startsWith("#") ? value.substring(1) : value;
		if (digits.length() != 6) {
			return DataResult.error(() -> "Expected #RRGGBB colour, got: " + value);
		}
		try {
			return DataResult.success(Integer.parseInt(digits, 16));
		} catch (NumberFormatException e) {
			return DataResult.error(() -> "Invalid hex colour: " + value);
		}
	}

	private static String formatHex(int rgb) {
		return String.format("#%06X", rgb & 0xFFFFFF);
	}

	private ColorCodecs() {
	}
}
