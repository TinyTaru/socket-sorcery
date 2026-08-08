package tinytaru.socketsorcery.item;

import java.util.Arrays;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.registry.ModItems;

/** The five pigments used to transcribe different families of pattern scrolls. */
public enum ScrollInkColor {
	RED("red", 0xF44336),
	BLUE("blue", 0x42A5F5),
	GREEN("green", 0x4CAF50),
	YELLOW("yellow", 0xFFEB3B),
	PURPLE("purple", 0x9C27B0);

	public static final Codec<ScrollInkColor> CODEC = Codec.STRING.comapFlatMap(
			ScrollInkColor::fromName, ScrollInkColor::id);
	public static final StreamCodec<ByteBuf, ScrollInkColor> STREAM_CODEC = StreamCodec.of(
			(buf, color) -> buf.writeByte(color.ordinal()),
			buf -> values()[buf.readUnsignedByte()]);

	private final String name;
	private final int rgb;

	ScrollInkColor(String name, int rgb) {
		this.name = name;
		this.rgb = rgb;
	}

	public String id() {
		return name;
	}

	public int rgb() {
		return rgb;
	}

	public Item item() {
		return ModItems.scrollInk(this);
	}

	public boolean matches(ItemStack stack) {
		return !stack.isEmpty() && (stack.is(item())
				|| this == RED && stack.is(ModItems.SCROLL_INK));
	}

	public static ScrollInkColor fromItem(ItemStack stack) {
		return Arrays.stream(values()).filter(color -> color.matches(stack)).findFirst().orElse(null);
	}

	private static DataResult<ScrollInkColor> fromName(String name) {
		for (ScrollInkColor color : values()) {
			if (color.name.equals(name)) return DataResult.success(color);
		}
		return DataResult.error(() -> "Unknown scroll ink color: " + name);
	}
}
