package com.coolnydude.exposureletters.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Whole newspaper: a masthead (shown on the first page) and 12 pages, each an ordered list of blocks.
 * Each page supports at most {@link #MAX_PHOTOS_PER_PAGE} (3) photographs, which are automatically scaled and positioned.
 */
public record NewspaperContent(Masthead masthead, List<Page> pages) {
    public static final int PAGE_COUNT = 12;
    public static final int MAX_BLOCKS_PER_PAGE = 8;
    public static final int MAX_PHOTOS_PER_PAGE = 3;

    public static final Codec<NewspaperContent> CODEC = RecordCodecBuilder.create(i -> i.group(
            Masthead.CODEC.optionalFieldOf("masthead", Masthead.EMPTY).forGetter(NewspaperContent::masthead),
            Page.CODEC.listOf().optionalFieldOf("pages", List.of()).forGetter(NewspaperContent::pages)
    ).apply(i, NewspaperContent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, NewspaperContent> STREAM_CODEC = StreamCodec.composite(
            Masthead.STREAM_CODEC, NewspaperContent::masthead,
            Page.STREAM_CODEC.apply(ByteBufCodecs.list(PAGE_COUNT)), NewspaperContent::pages,
            NewspaperContent::new);

    public static final NewspaperContent EMPTY = new NewspaperContent(Masthead.EMPTY, List.of());

    public NewspaperContent {
        pages = List.copyOf(pages);
    }

    public Page page(int index) {
        return index >= 0 && index < pages.size() ? pages.get(index) : Page.EMPTY;
    }

    public boolean isEmpty() {
        return masthead.isEmpty() && pages.stream().allMatch(Page::isEmpty);
    }

    public List<ItemStack> photos() {
        List<ItemStack> list = new ArrayList<>();
        for (Page page : pages)
            for (Block block : page.blocks())
                if (block.type() == BlockType.PHOTO && !block.photo().isEmpty())
                    list.add(block.photo());
        return list;
    }

    /** Trims texts and lists to their limits (including max 3 photos per page), so that a modified client can't store huge data. */
    public NewspaperContent sanitized() {
        List<Page> list = new ArrayList<>();
        for (int p = 0; p < Math.min(PAGE_COUNT, pages.size()); p++) {
            List<Block> blocks = new ArrayList<>();
            int photoCount = 0;
            for (Block b : pages.get(p).blocks()) {
                if (blocks.size() >= MAX_BLOCKS_PER_PAGE) break;
                if (b.type() == BlockType.PHOTO) {
                    if (photoCount >= MAX_PHOTOS_PER_PAGE) continue;
                    photoCount++;
                }
                blocks.add(new Block(b.type(), clamp(b.text(), b.type().maxLength),
                        b.type() == BlockType.PHOTO ? b.photo().copyWithCount(1) : ItemStack.EMPTY,
                        b.type() == BlockType.PHOTO && b.half()));
            }
            int lastDiv = -1;
            for (int i = 0; i < blocks.size(); i++) {
                if (blocks.get(i).type() == BlockType.DIVIDER) lastDiv = i;
            }
            if (lastDiv >= 0) {
                List<Block> reordered = new ArrayList<>();
                List<Block> movedPhotos = new ArrayList<>();
                for (int i = 0; i < blocks.size(); i++) {
                    Block b = blocks.get(i);
                    if (i < lastDiv && b.type() == BlockType.PHOTO) {
                        movedPhotos.add(b);
                    } else {
                        reordered.add(b);
                        if (i == lastDiv) {
                            reordered.addAll(movedPhotos);
                        }
                    }
                }
                blocks = reordered;
            }
            list.add(new Page(blocks));
        }
        Masthead m = new Masthead(clamp(masthead.title(), Masthead.TITLE_LENGTH), clamp(masthead.issue(), Masthead.ISSUE_LENGTH),
                clamp(masthead.date(), Masthead.DATE_LENGTH), clamp(masthead.slogan(), Masthead.SLOGAN_LENGTH));
        return new NewspaperContent(m, list);
    }

    public static String clamp(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    // ------------------------------------------------------------------------------------------------------------

    public record Masthead(String title, String issue, String date, String slogan) {
        public static final int TITLE_LENGTH = 32, ISSUE_LENGTH = 16, DATE_LENGTH = 32, SLOGAN_LENGTH = 64;
        public static final Masthead EMPTY = new Masthead("", "", "", "");

        public static final Codec<Masthead> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("title", "").forGetter(Masthead::title),
                Codec.STRING.optionalFieldOf("issue", "").forGetter(Masthead::issue),
                Codec.STRING.optionalFieldOf("date", "").forGetter(Masthead::date),
                Codec.STRING.optionalFieldOf("slogan", "").forGetter(Masthead::slogan)
        ).apply(i, Masthead::new));

        public static final StreamCodec<ByteBuf, Masthead> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(TITLE_LENGTH * 4), Masthead::title,
                ByteBufCodecs.stringUtf8(ISSUE_LENGTH * 4), Masthead::issue,
                ByteBufCodecs.stringUtf8(DATE_LENGTH * 4), Masthead::date,
                ByteBufCodecs.stringUtf8(SLOGAN_LENGTH * 4), Masthead::slogan,
                Masthead::new);

        public boolean isEmpty() {
            return title.isBlank() && issue.isBlank() && date.isBlank() && slogan.isBlank();
        }
    }

    public record Page(List<Block> blocks) {
        public static final Page EMPTY = new Page(List.of());

        public static final Codec<Page> CODEC = Block.CODEC.listOf().xmap(Page::new, Page::blocks);
        public static final StreamCodec<RegistryFriendlyByteBuf, Page> STREAM_CODEC =
                Block.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_BLOCKS_PER_PAGE)).map(Page::new, Page::blocks);

        public Page {
            blocks = List.copyOf(blocks);
        }

        public boolean isEmpty() {
            return blocks.isEmpty();
        }
    }

    public enum BlockType implements StringRepresentable {
        HEADLINE(64), SUBHEADING(96), TEXT(512), PHOTO(64), DIVIDER(0);

        public static final Codec<BlockType> CODEC = StringRepresentable.fromEnum(BlockType::values);
        public static final StreamCodec<ByteBuf, BlockType> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(i -> values()[Math.floorMod(i, values().length)], Enum::ordinal);

        /** Max characters of text (for PHOTO it is the caption). */
        public final int maxLength;

        BlockType(int maxLength) {
            this.maxLength = maxLength;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** One element of a page. ItemStack has no equals(), so the record overrides it. */
    public record Block(BlockType type, String text, ItemStack photo, boolean half) {
        public static final Codec<Block> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockType.CODEC.fieldOf("type").forGetter(Block::type),
                Codec.STRING.optionalFieldOf("text", "").forGetter(Block::text),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("photo", ItemStack.EMPTY).forGetter(Block::photo),
                Codec.BOOL.optionalFieldOf("half", false).forGetter(Block::half)
        ).apply(i, Block::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Block> STREAM_CODEC = StreamCodec.composite(
                BlockType.STREAM_CODEC, Block::type,
                ByteBufCodecs.stringUtf8(2048), Block::text,
                ItemStack.OPTIONAL_STREAM_CODEC, Block::photo,
                ByteBufCodecs.BOOL, Block::half,
                Block::new);

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Block b)) return false;
            return type == b.type && half == b.half && text.equals(b.text) && ItemStack.matches(photo, b.photo);
        }

        @Override
        public int hashCode() {
            int h = type.hashCode();
            h = 31 * h + text.hashCode();
            h = 31 * h + ItemStack.hashItemAndComponents(photo);
            h = 31 * h + Boolean.hashCode(half);
            return h;
        }
    }
}
