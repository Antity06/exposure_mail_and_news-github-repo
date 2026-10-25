package com.coolnydude.exposureletters.client.render;

import com.coolnydude.exposureletters.component.NewspaperContent;
import com.coolnydude.exposureletters.component.NewspaperContent.Block;
import com.coolnydude.exposureletters.component.NewspaperContent.BlockType;
import com.coolnydude.exposureletters.component.NewspaperContent.Masthead;
import io.github.mortuusars.exposure.ExposureClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays out and draws one newspaper page. Used by the reader, the editor (with placeholders and selection)
 * and for the overflow check before publishing. All sizes are in "page units"; the page is drawn scaled.
 *
 * Photographs (up to 3 per page) are automatically scaled and positioned based on count:
 * - 1 photo: top-center
 * - 2 photos: 1st in top-right corner, 2nd in bottom-left corner
 * - 3 photos: 1st in top-right corner, 2nd in middle-left, 3rd in bottom-right corner
 */
public final class NewspaperPageRenderer {
    public static final int PAGE_W = 168, PAGE_H = 220, MARGIN = 8, FOOTER = 10;
    public static final int CONTENT_W = PAGE_W - MARGIN * 2;
    public static final int CONTENT_H = PAGE_H - MARGIN * 2 - FOOTER;
    public static final int MASTHEAD_INDEX = -1;

    public static final float HEADLINE_SCALE = 1.25F, SUBHEADING_SCALE = 0.85F, TEXT_SCALE = 0.65F, CAPTION_SCALE = 0.55F, META_SCALE = 0.6F;
    public static final int MAX_FULL_PHOTO = 88;

    private static final int PAPER = 0xFFEFE9D8, PAPER_EDGE = 0xFFC9C0A8, INK = 0xFF161616, INK_SOFT = 0xFF3A3A3A,
            INK_FAINT = 0xFF6E6858, PLACEHOLDER = 0xFF9A927E, SELECT = 0xFF2F6BD8, OVERFLOW = 0x55E03030;

    public record Placed(int index, int x, int y, int w, int h) {
        public boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    public record Layout(List<Placed> placed, int height) {
        public boolean overflows() {
            return height > CONTENT_H;
        }
    }

    // ------------------------------------------------------------------ text helpers

    private static Component headlineText(Block b, boolean editing) {
        return text(b.text(), editing, "gui.exposure_letters.newspaper.placeholder.headline").copy().withStyle(ChatFormatting.BOLD);
    }

    private static Component text(String s, boolean editing, String placeholderKey) {
        if (!s.isBlank()) return Component.literal(s);
        return editing ? Component.translatable(placeholderKey) : Component.empty();
    }

    private static List<FormattedCharSequence> wrap(Font font, Component text, int width, float scale) {
        if (text.getString().isEmpty()) return List.of();
        return font.split(text, Math.max(1, (int) (width / scale)));
    }

    private static int height(int lines, float scale) {
        return (int) Math.ceil(lines * font().lineHeight * scale);
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static void drawScaled(GuiGraphics g, Font font, FormattedCharSequence text, float x, float y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private static void drawCentered(GuiGraphics g, Font font, FormattedCharSequence text, float centerX, float y, float scale, int color) {
        drawScaled(g, font, text, centerX - font.width(text) * scale / 2F, y, scale, color);
    }

    // ------------------------------------------------------------------ layout

    public static int mastheadHeight(Font font, Masthead m, boolean editing) {
        int h = 2 + (int) Math.ceil(9 * titleScale(font, m, editing)) + 3 + 2 + 3 + height(1, META_SCALE) + 2;
        if (!m.slogan().isBlank() || editing) h += height(1, META_SCALE) + 1;
        return h + 4;
    }

    private static Component title(Masthead m, boolean editing) {
        return text(m.title(), editing, "gui.exposure_letters.newspaper.placeholder.title").copy().withStyle(ChatFormatting.BOLD);
    }

    private static float titleScale(Font font, Masthead m, boolean editing) {
        int w = font.width(title(m, editing));
        return w == 0 ? 2F : Math.min(2F, CONTENT_W / (float) w);
    }

    private static int captionHeight(Font font, Block b, int width) {
        List<FormattedCharSequence> lines = wrap(font, Component.literal(b.text()).withStyle(ChatFormatting.ITALIC), width, CAPTION_SCALE);
        return lines.isEmpty() ? 0 : height(lines.size(), CAPTION_SCALE) + 2;
    }

    public static int halfPhotoSize() {
        return (CONTENT_W - 4) / 2;
    }

    private static int blockHeight(Font font, Block b, int width, boolean editing) {
        return switch (b.type()) {
            case HEADLINE -> Math.max(6, height(wrap(font, headlineText(b, editing), width, HEADLINE_SCALE).size(), HEADLINE_SCALE) + 3);
            case SUBHEADING -> {
                Component t = text(b.text(), editing, "gui.exposure_letters.newspaper.placeholder.subheading").copy().withStyle(ChatFormatting.ITALIC);
                yield Math.max(6, height(wrap(font, t, width, SUBHEADING_SCALE).size(), SUBHEADING_SCALE) + 3);
            }
            case TEXT -> {
                Component t = text(b.text(), editing, "gui.exposure_letters.newspaper.placeholder.text");
                yield Math.max(6, height(wrap(font, t, width, TEXT_SCALE).size(), TEXT_SCALE) + 3);
            }
            case DIVIDER -> 6;
            case PHOTO -> 0;
        };
    }

    private static boolean overlapsY(int y0, int y1, Placed p) {
        return y0 < p.y() + p.h() && y1 > p.y();
    }

    public static Layout layout(Font font, Masthead masthead, boolean showMasthead, List<Block> blocks, boolean editing) {
        List<Placed> placed = new ArrayList<>();
        int topY = 0;

        if (showMasthead) {
            int h = mastheadHeight(font, masthead, editing);
            placed.add(new Placed(MASTHEAD_INDEX, 0, 0, CONTENT_W, h));
            topY = h;
        }

        int lastDividerIdx = -1;
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).type() == BlockType.DIVIDER) {
                lastDividerIdx = i;
            }
        }

        int maxBottom = topY;
        int photoTopY = topY;

        // If a divider is present on the page, lay out all non-photo blocks up to and including the divider first,
        // so that photographs can only be placed below the divider.
        if (lastDividerIdx >= 0) {
            int yAbove = topY;
            for (int i = 0; i <= lastDividerIdx; i++) {
                Block b = blocks.get(i);
                if (b.type() == BlockType.PHOTO) continue;
                int h = blockHeight(font, b, CONTENT_W, editing);
                placed.add(new Placed(i, 0, yAbove, CONTENT_W, h));
                yAbove += h;
                maxBottom = Math.max(maxBottom, yAbove);
            }
            photoTopY = yAbove;
        }

        List<Integer> photoIndices = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).type() == BlockType.PHOTO && photoIndices.size() < NewspaperContent.MAX_PHOTOS_PER_PAGE) {
                photoIndices.add(i);
            }
        }

        int photoCount = photoIndices.size();
        int availH = Math.max(36, CONTENT_H - photoTopY);
        List<Placed> photoPlaced = new ArrayList<>();

        if (photoCount == 1) {
            // 1 photo: top-center (below the divider if a divider is present)
            int idx0 = photoIndices.get(0);
            Block b0 = blocks.get(idx0);
            int size = Math.min(MAX_FULL_PHOTO, Math.max(36, availH - 20));
            int h0 = size + captionHeight(font, b0, size) + 3;
            Placed p0 = new Placed(idx0, (CONTENT_W - size) / 2, photoTopY, size, h0);
            photoPlaced.add(p0);
            placed.add(p0);
            maxBottom = Math.max(maxBottom, p0.y() + p0.h());
        } else if (photoCount == 2) {
            // 2 photos: 1st in top-right corner (below divider), 2nd in bottom-left corner
            int idx0 = photoIndices.get(0);
            int idx1 = photoIndices.get(1);
            Block b0 = blocks.get(idx0);
            Block b1 = blocks.get(idx1);
            int size = Math.min(halfPhotoSize(), Math.max(36, (availH - 16) / 2));
            int h0 = size + captionHeight(font, b0, size) + 3;
            int h1 = size + captionHeight(font, b1, size) + 3;
            int y0 = photoTopY;
            int y1 = Math.max(y0 + h0 + 2, CONTENT_H - h1);

            Placed p0 = new Placed(idx0, CONTENT_W - size, y0, size, h0);
            Placed p1 = new Placed(idx1, 0, y1, size, h1);
            photoPlaced.add(p0);
            photoPlaced.add(p1);
            placed.add(p0);
            placed.add(p1);
            maxBottom = Math.max(maxBottom, Math.max(p0.y() + p0.h(), p1.y() + p1.h()));
        } else if (photoCount >= 3) {
            // 3 photos: 1st in top-right corner (below divider), 2nd in middle-left, 3rd in bottom-right corner
            int idx0 = photoIndices.get(0);
            int idx1 = photoIndices.get(1);
            int idx2 = photoIndices.get(2);
            Block b0 = blocks.get(idx0);
            Block b1 = blocks.get(idx1);
            Block b2 = blocks.get(idx2);
            int size = Math.min(54, Math.max(30, (availH - 18) / 3));
            int h0 = size + captionHeight(font, b0, size) + 3;
            int h1 = size + captionHeight(font, b1, size) + 3;
            int h2 = size + captionHeight(font, b2, size) + 3;
            int y0 = photoTopY;
            int y1 = Math.max(y0 + h0 + 2, photoTopY + (availH - h1) / 2);
            int y2 = Math.max(y1 + h1 + 2, CONTENT_H - h2);

            Placed p0 = new Placed(idx0, CONTENT_W - size, y0, size, h0);
            Placed p1 = new Placed(idx1, 0, y1, size, h1);
            Placed p2 = new Placed(idx2, CONTENT_W - size, y2, size, h2);
            photoPlaced.add(p0);
            photoPlaced.add(p1);
            photoPlaced.add(p2);
            placed.add(p0);
            placed.add(p1);
            placed.add(p2);
            maxBottom = Math.max(maxBottom, Math.max(p0.y() + p0.h(), Math.max(p1.y() + p1.h(), p2.y() + p2.h())));
        }

        int y = (photoCount == 1) ? (photoPlaced.get(0).y() + photoPlaced.get(0).h() + 2) : photoTopY;

        for (int i = lastDividerIdx + 1; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            if (b.type() == BlockType.PHOTO) continue;

            if (photoCount <= 1) {
                int h = blockHeight(font, b, CONTENT_W, editing);
                placed.add(new Placed(i, 0, y, CONTENT_W, h));
                y += h;
                maxBottom = Math.max(maxBottom, y);
            } else {
                // Flow text blocks in the free horizontal column beside any overlapping photo
                int guard = 0;
                while (guard++ < 8) {
                    int leftBound = 0;
                    int rightBound = CONTENT_W;
                    int estH = blockHeight(font, b, CONTENT_W / 2, editing);
                    Placed blocking = null;
                    for (Placed p : photoPlaced) {
                        if (overlapsY(y, y + estH, p)) {
                            if (p.x() == 0) {
                                leftBound = Math.max(leftBound, p.w() + 4);
                            } else {
                                rightBound = Math.min(rightBound, p.x() - 4);
                            }
                            if (blocking == null || p.y() < blocking.y()) {
                                blocking = p;
                            }
                        }
                    }
                    int bw = rightBound - leftBound;
                    if (bw < 36 && blocking != null) {
                        y = blocking.y() + blocking.h();
                        continue;
                    }
                    bw = Math.max(36, bw);
                    int actualH = blockHeight(font, b, bw, editing);
                    // Re-verify if actualH now collides with a photo on the opposite side
                    int checkLeft = 0;
                    int checkRight = CONTENT_W;
                    for (Placed p : photoPlaced) {
                        if (overlapsY(y, y + actualH, p)) {
                            if (p.x() == 0) checkLeft = Math.max(checkLeft, p.w() + 4);
                            else checkRight = Math.min(checkRight, p.x() - 4);
                        }
                    }
                    if (checkRight - checkLeft < bw && checkRight - checkLeft >= 36) {
                        leftBound = checkLeft;
                        bw = checkRight - checkLeft;
                        actualH = blockHeight(font, b, bw, editing);
                    }
                    placed.add(new Placed(i, leftBound, y, bw, actualH));
                    y += actualH;
                    maxBottom = Math.max(maxBottom, y);
                    break;
                }
            }
        }

        return new Layout(placed, maxBottom);
    }

    // ------------------------------------------------------------------ render

    /**
     * @param screenX,screenY top-left corner of the page on screen
     * @param scale           page scale
     * @param selected        index of the selected block (editor) or Integer.MIN_VALUE
     */
    public static Layout render(GuiGraphics g, Font font, Masthead masthead, boolean showMasthead, List<Block> blocks,
                                int screenX, int screenY, float scale, int pageNumber, boolean editing, int selected) {
        Layout layout = layout(font, masthead, showMasthead, blocks, editing);

        g.pose().pushPose();
        g.pose().translate(screenX, screenY, 0);
        g.pose().scale(scale, scale, 1);

        g.fill(1, 1, PAGE_W + 1, PAGE_H + 1, 0x40000000);
        g.fill(0, 0, PAGE_W, PAGE_H, PAPER_EDGE);
        g.fill(1, 1, PAGE_W - 1, PAGE_H - 1, PAPER);

        g.enableScissor(screenX, screenY, screenX + (int) Math.ceil(PAGE_W * scale), screenY + (int) Math.ceil(PAGE_H * scale));
        g.pose().pushPose();
        g.pose().translate(MARGIN, MARGIN, 0);

        for (Placed p : layout.placed()) {
            if (p.index() == MASTHEAD_INDEX) renderMasthead(g, font, masthead, p, editing);
            else renderBlock(g, font, blocks.get(p.index()), p, editing);
        }

        if (editing) {
            for (Placed p : layout.placed()) {
                if (p.index() == selected) {
                    int x0 = p.x() - 2, y0 = p.y() - 1, x1 = p.x() + p.w() + 2, y1 = p.y() + p.h();
                    g.renderOutline(x0, y0, x1 - x0, y1 - y0, SELECT);
                }
            }
            if (layout.overflows()) {
                g.fill(-MARGIN + 1, CONTENT_H, CONTENT_W + MARGIN - 1, PAGE_H - MARGIN, OVERFLOW);
                g.fill(-MARGIN + 1, CONTENT_H, CONTENT_W + MARGIN - 1, CONTENT_H + 1, 0xFFE03030);
            }
        }
        g.pose().popPose();
        g.disableScissor();

        if (pageNumber > 0) {
            FormattedCharSequence num = Component.literal("— " + pageNumber + " —").getVisualOrderText();
            drawCentered(g, font, num, PAGE_W / 2F, PAGE_H - MARGIN - 4, META_SCALE, INK_FAINT);
        }
        g.pose().popPose();
        return layout;
    }

    private static void renderMasthead(GuiGraphics g, Font font, Masthead m, Placed p, boolean editing) {
        float y = p.y() + 2;
        float ts = titleScale(font, m, editing);
        Component title = title(m, editing);
        drawCentered(g, font, title.getVisualOrderText(), CONTENT_W / 2F, y, ts, m.title().isBlank() ? PLACEHOLDER : INK);
        y += 9 * ts + 3;
        g.fill(0, (int) y, CONTENT_W, (int) y + 2, INK);
        y += 2 + 3;

        Component issue = m.issue().isBlank()
                ? (editing ? Component.translatable("gui.exposure_letters.newspaper.placeholder.issue") : Component.empty())
                : Component.literal("№ " + m.issue());
        Component date = text(m.date(), editing, "gui.exposure_letters.newspaper.placeholder.date");
        drawScaled(g, font, issue.getVisualOrderText(), 0, y, META_SCALE, m.issue().isBlank() ? PLACEHOLDER : INK_SOFT);
        drawScaled(g, font, date.getVisualOrderText(), CONTENT_W - font.width(date) * META_SCALE, y, META_SCALE,
                m.date().isBlank() ? PLACEHOLDER : INK_SOFT);
        y += height(1, META_SCALE) + 2;

        if (!m.slogan().isBlank() || editing) {
            MutableComponent slogan = text(m.slogan(), editing, "gui.exposure_letters.newspaper.placeholder.slogan").copy().withStyle(ChatFormatting.ITALIC);
            drawCentered(g, font, slogan.getVisualOrderText(), CONTENT_W / 2F, y, META_SCALE, m.slogan().isBlank() ? PLACEHOLDER : INK_SOFT);
            y += height(1, META_SCALE) + 1;
        }
        g.fill(0, (int) y, CONTENT_W, (int) y + 1, INK);
    }

    private static void renderBlock(GuiGraphics g, Font font, Block b, Placed p, boolean editing) {
        boolean empty = b.text().isBlank();
        switch (b.type()) {
            case HEADLINE -> lines(g, font, headlineText(b, editing), p, HEADLINE_SCALE, empty ? PLACEHOLDER : INK);
            case SUBHEADING -> lines(g, font, text(b.text(), editing, "gui.exposure_letters.newspaper.placeholder.subheading")
                    .copy().withStyle(ChatFormatting.ITALIC), p, SUBHEADING_SCALE, empty ? PLACEHOLDER : INK_SOFT);
            case TEXT -> lines(g, font, text(b.text(), editing, "gui.exposure_letters.newspaper.placeholder.text"), p, TEXT_SCALE,
                    empty ? PLACEHOLDER : INK);
            case DIVIDER -> {
                int y = p.y() + 2;
                g.fill(p.x(), y, p.x() + p.w(), y + 1, INK_SOFT);
                g.fill(p.x(), y + 2, p.x() + p.w(), y + 3, INK_SOFT);
            }
            case PHOTO -> {
                int size = p.w();
                if (b.photo().isEmpty()) {
                    g.fill(p.x(), p.y(), p.x() + size, p.y() + size, 0xFFD8D1BE);
                    g.renderOutline(p.x(), p.y(), size, size, PAPER_EDGE);
                    if (editing) {
                        Component t = Component.translatable("gui.exposure_letters.newspaper.placeholder.photo");
                        drawCentered(g, font, t.getVisualOrderText(), p.x() + size / 2F, p.y() + size / 2F - 3, 0.7F, PLACEHOLDER);
                    }
                } else {
                    renderPhoto(g, b.photo(), p.x(), p.y(), size);
                }
                if (!b.text().isBlank()) {
                    List<FormattedCharSequence> cap = wrap(font, Component.literal(b.text()).withStyle(ChatFormatting.ITALIC), size, CAPTION_SCALE);
                    float y = p.y() + size + 2;
                    for (FormattedCharSequence line : cap) {
                        drawCentered(g, font, line, p.x() + size / 2F, y, CAPTION_SCALE, INK_FAINT);
                        y += 9 * CAPTION_SCALE;
                    }
                }
            }
        }
    }

    private static void lines(GuiGraphics g, Font font, Component text, Placed p, float scale, int color) {
        float y = p.y();
        for (FormattedCharSequence line : wrap(font, text, p.w(), scale)) {
            drawScaled(g, font, line, p.x(), y, scale, color);
            y += 9 * scale;
        }
    }

    public static void renderPhoto(GuiGraphics g, ItemStack photo, float x, float y, float size) {
        g.flush();
        g.pose().pushPose();
        g.pose().translate(x, y, 1);
        g.pose().scale(size, size, size);
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        try {
            ExposureClient.photographRenderer().render(photo, false, false, g.pose(), buffer, 0xF000F0);
        } catch (Throwable ignored) {
            // Photo data not available yet (or incompatible Exposure version): leave the frame empty.
        }
        buffer.endBatch();
        g.pose().popPose();
    }

    private NewspaperPageRenderer() {}
}
