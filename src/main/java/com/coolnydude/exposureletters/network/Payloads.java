package com.coolnydude.exposureletters.network;

import com.coolnydude.exposureletters.ExposureLetters;
import com.coolnydude.exposureletters.component.NewspaperContent;
import com.coolnydude.exposureletters.component.NewspaperMeta;
import com.coolnydude.exposureletters.component.NoteContent;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class Payloads {
    public static final StreamCodec<ByteBuf, InteractionHand> HAND = ByteBufCodecs.BOOL.map(
            b -> b ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
            h -> h == InteractionHand.OFF_HAND);

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String id) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ExposureLetters.MOD_ID, id));
    }

    public record EditNote(Optional<BlockPos> lectern, InteractionHand hand, String text) implements CustomPacketPayload {
        public static final Type<EditNote> TYPE = typeOf("edit_note");
        public static final StreamCodec<ByteBuf, EditNote> CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), EditNote::lectern,
                HAND, EditNote::hand,
                ByteBufCodecs.stringUtf8(4096), EditNote::text,
                EditNote::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TakeFromLectern(BlockPos pos) implements CustomPacketPayload {
        public static final Type<TakeFromLectern> TYPE = typeOf("take_from_lectern");
        public static final StreamCodec<ByteBuf, TakeFromLectern> CODEC = BlockPos.STREAM_CODEC.map(
                TakeFromLectern::new, TakeFromLectern::pos);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestLecternSync(BlockPos pos) implements CustomPacketPayload {
        public static final Type<RequestLecternSync> TYPE = typeOf("request_lectern_sync");
        public static final StreamCodec<ByteBuf, RequestLecternSync> CODEC = BlockPos.STREAM_CODEC.map(
                RequestLecternSync::new, RequestLecternSync::pos);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UpdateEnvelope(InteractionHand hand, String from, String to, boolean seal) implements CustomPacketPayload {
        public static final Type<UpdateEnvelope> TYPE = typeOf("update_envelope");
        public static final StreamCodec<ByteBuf, UpdateEnvelope> CODEC = StreamCodec.composite(
                HAND, UpdateEnvelope::hand,
                ByteBufCodecs.stringUtf8(256), UpdateEnvelope::from,
                ByteBufCodecs.stringUtf8(256), UpdateEnvelope::to,
                ByteBufCodecs.BOOL, UpdateEnvelope::seal,
                UpdateEnvelope::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SaveNewspaper(InteractionHand hand, NewspaperContent content, List<Integer> usedSlots, boolean publish) implements CustomPacketPayload {
        public static final Type<SaveNewspaper> TYPE = typeOf("save_newspaper");
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveNewspaper> CODEC = StreamCodec.composite(
                HAND, SaveNewspaper::hand,
                NewspaperContent.STREAM_CODEC, SaveNewspaper::content,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(128)), SaveNewspaper::usedSlots,
                ByteBufCodecs.BOOL, SaveNewspaper::publish,
                SaveNewspaper::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenLecternNote(BlockPos pos, NoteContent content) implements CustomPacketPayload {
        public static final Type<OpenLecternNote> TYPE = typeOf("open_lectern_note");
        public static final StreamCodec<ByteBuf, OpenLecternNote> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenLecternNote::pos,
                NoteContent.STREAM_CODEC, OpenLecternNote::content,
                OpenLecternNote::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenLecternNewspaper(BlockPos pos, NewspaperContent content, NewspaperMeta meta) implements CustomPacketPayload {
        public static final Type<OpenLecternNewspaper> TYPE = typeOf("open_lectern_newspaper");
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenLecternNewspaper> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenLecternNewspaper::pos,
                NewspaperContent.STREAM_CODEC, OpenLecternNewspaper::content,
                NewspaperMeta.STREAM_CODEC, OpenLecternNewspaper::meta,
                OpenLecternNewspaper::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncLectern(BlockPos pos, ItemStack book) implements CustomPacketPayload {
        public static final Type<SyncLectern> TYPE = typeOf("sync_lectern");
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncLectern> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, SyncLectern::pos,
                ItemStack.OPTIONAL_STREAM_CODEC, SyncLectern::book,
                SyncLectern::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private Payloads() {}
}
