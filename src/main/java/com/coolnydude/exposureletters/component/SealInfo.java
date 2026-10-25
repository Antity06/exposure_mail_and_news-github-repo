package com.coolnydude.exposureletters.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Marks an envelope as sealed and remembers whose seal pressed the wax. */
public record SealInfo(String sealName) {
    public static final Codec<SealInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("seal", "").forGetter(SealInfo::sealName)
    ).apply(i, SealInfo::new));

    public static final StreamCodec<ByteBuf, SealInfo> STREAM_CODEC = ByteBufCodecs.stringUtf8(256)
            .map(SealInfo::new, SealInfo::sealName);
}
