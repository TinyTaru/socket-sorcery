package tinytaru.socketsorcery.component;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Data component value stored on an engraved gem: the chiselled pattern and the set of modifiers
 * applied on top of it (empty for a plain base engraving).
 */
public record EngravingData(Identifier pattern, List<Identifier> modifiers) {

	public EngravingData {
		modifiers = List.copyOf(modifiers);
	}

	/** A base engraving with no modifiers. */
	public EngravingData(Identifier pattern) {
		this(pattern, List.of());
	}

	public static final Codec<EngravingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("pattern").forGetter(EngravingData::pattern),
			Identifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(EngravingData::modifiers)
	).apply(instance, EngravingData::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, EngravingData> STREAM_CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, EngravingData::pattern,
			Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), EngravingData::modifiers,
			EngravingData::new
	);
}
