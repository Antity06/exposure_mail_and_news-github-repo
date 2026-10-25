package com.coolnydude.exposureletters.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Text written on a note (one sheet of paper), with optional book-like sealing. */
public record NoteContent(String text, String author, boolean sealed) {
    public static final int MAX_LENGTH = 1024;
    public static final NoteContent EMPTY = new NoteContent("", "", false);

    public NoteContent(String text, String author) {
        this(text, author, false);
    }

    public static final Codec<NoteContent> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("text", "").forGetter(NoteContent::text),
            Codec.STRING.optionalFieldOf("author", "").forGetter(NoteContent::author),
            Codec.BOOL.optionalFieldOf("sealed", false).forGetter(NoteContent::sealed)
    ).apply(i, NoteContent::new));

    public static final StreamCodec<ByteBuf, NoteContent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_LENGTH * 4), NoteContent::text,
            ByteBufCodecs.stringUtf8(64), NoteContent::author,
            ByteBufCodecs.BOOL, NoteContent::sealed,
            NoteContent::new);

    public boolean isEmpty() {
        return text.isBlank() && !sealed;
    }
}
