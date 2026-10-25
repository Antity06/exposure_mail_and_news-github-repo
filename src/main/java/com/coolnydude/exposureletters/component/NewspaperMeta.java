package com.coolnydude.exposureletters.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Author and copy generation of a printed newspaper (0 original, 1 copy, 2 copy of a copy). */
public record NewspaperMeta(String author, int generation) {
    public static final int MAX_GENERATION = 2;

    public static final Codec<NewspaperMeta> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("author", "").forGetter(NewspaperMeta::author),
            Codec.intRange(0, MAX_GENERATION).optionalFieldOf("generation", 0).forGetter(NewspaperMeta::generation)
    ).apply(i, NewspaperMeta::new));

    public static final StreamCodec<ByteBuf, NewspaperMeta> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), NewspaperMeta::author,
            ByteBufCodecs.VAR_INT, NewspaperMeta::generation,
            NewspaperMeta::new);

    public boolean canBeCopied() {
        return generation < MAX_GENERATION;
    }

    public NewspaperMeta copy() {
        return new NewspaperMeta(author, Math.min(generation + 1, MAX_GENERATION));
    }
}
