package com.coolnydude.exposureletters.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** "From" and "To" written on an envelope. */
public record EnvelopeAddress(String from, String to) {
    public static final int MAX_LENGTH = 32;
    public static final EnvelopeAddress EMPTY = new EnvelopeAddress("", "");

    public static final Codec<EnvelopeAddress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("from", "").forGetter(EnvelopeAddress::from),
            Codec.STRING.optionalFieldOf("to", "").forGetter(EnvelopeAddress::to)
    ).apply(i, EnvelopeAddress::new));

    public static final StreamCodec<ByteBuf, EnvelopeAddress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_LENGTH * 4), EnvelopeAddress::from,
            ByteBufCodecs.stringUtf8(MAX_LENGTH * 4), EnvelopeAddress::to,
            EnvelopeAddress::new);

    public static String clamp(String s) {
        s = s == null ? "" : s.strip();
        return s.length() > MAX_LENGTH ? s.substring(0, MAX_LENGTH) : s;
    }
}
