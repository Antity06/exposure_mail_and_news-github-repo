package com.coolnydude.exposureletters.client.screen;

import com.coolnydude.exposureletters.client.render.NewspaperPageRenderer;
import com.coolnydude.exposureletters.client.render.NewspaperPageRenderer.Layout;
import com.coolnydude.exposureletters.client.render.NewspaperPageRenderer.Placed;
import com.coolnydude.exposureletters.client.widget.PaperTextArea;
import com.coolnydude.exposureletters.component.NewspaperContent;
import com.coolnydude.exposureletters.component.NewspaperContent.Block;
import com.coolnydude.exposureletters.component.NewspaperContent.BlockType;
import com.coolnydude.exposureletters.component.NewspaperContent.Masthead;
import com.coolnydude.exposureletters.item.NewspaperItem;
import com.coolnydude.exposureletters.network.Payloads;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static com.coolnydude.exposureletters.client.render.NewspaperPageRenderer.*;

/**
 * Newspaper editor: the page preview on the left (click a block to select it) and a tool panel on the right.
 * Enforces at most 3 photographs per page, with automatic scaling and positioning.
 */
public class NewspaperEditScreen extends Screen {
    private static final int PANEL_W = 156;
    private static final int NONE = Integer.MIN_VALUE;
    private static final int PICKER_COLS = 8;

    /** Mutable block used while editing. sourceSlot = inventory slot a new photo came from (-1 = already in the draft). */
    private static final class EBlock {
        BlockType type;
        String text = "";
        ItemStack photo = ItemStack.EMPTY;
        boolean half;
        int sourceSlot = -1;

        EBlock(BlockType type) {
            this.type = type;
        }

        static EBlock of(Block b) {
            EBlock e = new EBlock(b.type());
            e.text = b.text();
            e.photo = b.photo().copy();
            e.half = b.half();
            return e;
        }

        Block toBlock() {
            return new Block(type, text, photo, half);
        }
    }

    private final InteractionHand hand;
    private final String[] masthead = new String[4]; // title, issue, date, slogan
    private final List<List<EBlock>> pages = new ArrayList<>();
    private int page;
    private int selected = NONE;
    private boolean sent;

    private float scale;
    private int pageX, pageY, panelX, panelY, panelH;
    private int pickerY = -1;
    private Button publishButton;

    public NewspaperEditScreen(InteractionHand hand, NewspaperContent content) {
        super(Component.translatable("gui.exposure_letters.newspaper.editor"));
        this.hand = hand;
        Masthead m = content.masthead();
        masthead[0] = m.title();
        masthead[1] = m.issue();
        masthead[2] = m.date();
        masthead[3] = m.slogan();
        this.selected = MASTHEAD_INDEX;
        for (int i = 0; i < NewspaperContent.PAGE_COUNT; i++) {
            List<EBlock> blocks = new ArrayList<>();
            for (Block b : content.page(i).blocks()) blocks.add(EBlock.of(b));
            pages.add(blocks);
        }
    }

    // ------------------------------------------------------------------ model helpers

    private List<EBlock> blocks() {
        return pages.get(page);
    }

    private int photoCountOnPage() {
        int count = 0;
        for (EBlock e : blocks()) {
            if (e.type == BlockType.PHOTO) count++;
        }
        return count;
    }

    private int lastDividerIndex() {
        int idx = -1;
        List<EBlock> list = blocks();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).type == BlockType.DIVIDER) idx = i;
        }
        return idx;
    }

    private static boolean validPhotoDividerOrder(List<EBlock> list) {
        int lastDiv = -1;
        int firstPhoto = Integer.MAX_VALUE;
        for (int i = 0; i < list.size(); i++) {
            BlockType t = list.get(i).type;
            if (t == BlockType.DIVIDER) lastDiv = i;
            else if (t == BlockType.PHOTO && i < firstPhoto) firstPhoto = i;
        }
        return lastDiv < 0 || firstPhoto > lastDiv;
    }

    private boolean canMove(int delta) {
        int target = selected + delta;
        if (selected < 0 || target < 0 || target >= blocks().size()) return false;
        List<EBlock> copy = new ArrayList<>(blocks());
        Collections.swap(copy, selected, target);
        return validPhotoDividerOrder(copy);
    }

    private void enforcePhotosBelowDivider(EBlock keepSelected) {
        List<EBlock> list = blocks();
        int lastDiv = lastDividerIndex();
        if (lastDiv < 0) return;
        List<EBlock> reordered = new ArrayList<>();
        List<EBlock> movedPhotos = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            EBlock b = list.get(i);
            if (i < lastDiv && b.type == BlockType.PHOTO) {
                movedPhotos.add(b);
            } else {
                reordered.add(b);
                if (i == lastDiv) {
                    reordered.addAll(movedPhotos);
                }
            }
        }
        list.clear();
        list.addAll(reordered);
        if (keepSelected != null) {
            selected = list.indexOf(keepSelected);
        }
    }

    private List<Block> records(int pageIndex) {
        List<Block> list = new ArrayList<>();
        for (EBlock e : pages.get(pageIndex)) list.add(e.toBlock());
        return list;
    }

    private Masthead mastheadRecord() {
        return new Masthead(masthead[0], masthead[1], masthead[2], masthead[3]);
    }

    private NewspaperContent toContent() {
        List<NewspaperContent.Page> list = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) list.add(new NewspaperContent.Page(records(i)));
        return new NewspaperContent(mastheadRecord(), list);
    }

    private Layout layout(int pageIndex) {
        return NewspaperPageRenderer.layout(font, mastheadRecord(), pageIndex == 0, records(pageIndex), true);
    }

    private boolean anyOverflow() {
        for (int i = 0; i < pages.size(); i++)
            if (layout(i).overflows()) return true;
        return false;
    }

    private EBlock selectedBlock() {
        return selected >= 0 && selected < blocks().size() ? blocks().get(selected) : null;
    }

    private int usedFromSlot(int slot) {
        int n = 0;
        for (List<EBlock> p : pages)
            for (EBlock e : p)
                if (e.sourceSlot == slot && !e.photo.isEmpty()) n++;
        return n;
    }

    private record PickerEntry(int slot, ItemStack stack) {}

    private List<PickerEntry> availablePhotos() {
        List<PickerEntry> list = new ArrayList<>();
        Inventory inv = minecraft.player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack s = inv.getItem(slot);
            if (s.getItem() instanceof PhotographItem && s.getCount() > usedFromSlot(slot))
                list.add(new PickerEntry(slot, s));
        }
        return list;
    }

    // ------------------------------------------------------------------ widgets

    @Override
    protected void init() {
        scale = Math.min(1F, (height - 16F) / PAGE_H);
        int pageW = (int) (PAGE_W * scale);
        pageX = Math.max(4, (width - (pageW + 10 + PANEL_W)) / 2);
        pageY = (height - (int) (PAGE_H * scale)) / 2;
        panelX = pageX + pageW + 10;
        panelY = pageY;
        panelH = (int) (PAGE_H * scale);
        pickerY = -1;

        // Row 0: page navigation
        addRenderableWidget(new PageButton(panelX, panelY, false, b -> changePage(-1), true));
        addRenderableWidget(new PageButton(panelX + PANEL_W - 23, panelY, true, b -> changePage(1), true));

        // Row 1: add blocks
        int y = panelY + 18;
        BlockType[] types = {BlockType.HEADLINE, BlockType.SUBHEADING, BlockType.TEXT, BlockType.PHOTO, BlockType.DIVIDER};
        int bw = PANEL_W / types.length;
        int currentPhotos = photoCountOnPage();
        for (int i = 0; i < types.length; i++) {
            BlockType type = types[i];
            boolean photoLimitReached = type == BlockType.PHOTO && currentPhotos >= NewspaperContent.MAX_PHOTOS_PER_PAGE;
            String tooltipKey = photoLimitReached
                    ? "gui.exposure_letters.newspaper.add.photo.limit"
                    : "gui.exposure_letters.newspaper.add." + type.getSerializedName() + ".tooltip";
            Button b = Button.builder(Component.translatable("gui.exposure_letters.newspaper.add." + type.getSerializedName()), btn -> addBlock(type))
                    .bounds(panelX + i * bw, y, bw - 2, 18)
                    .tooltip(Tooltip.create(Component.translatable(tooltipKey)))
                    .build();
            b.active = blocks().size() < NewspaperContent.MAX_BLOCKS_PER_PAGE && !photoLimitReached;
            addRenderableWidget(b);
        }

        // Row 2: selected block controls
        y = panelY + 40;
        EBlock sel = selectedBlock();
        if (sel != null) {
            addRenderableWidget(smallButton(panelX, y, "▲", "move_up", b -> move(-1))).active = canMove(-1);
            addRenderableWidget(smallButton(panelX + 22, y, "▼", "move_down", b -> move(1))).active = canMove(1);
            addRenderableWidget(smallButton(panelX + 44, y, "✕", "delete", b -> deleteSelected()));
        }

        // Row 3: editing area
        y = panelY + 62;
        int areaBottom = panelY + panelH - 24;
        if (selected == MASTHEAD_INDEX && page == 0) {
            String[] hints = {"title", "issue", "date", "slogan"};
            int[] limits = {Masthead.TITLE_LENGTH, Masthead.ISSUE_LENGTH, Masthead.DATE_LENGTH, Masthead.SLOGAN_LENGTH};
            for (int i = 0; i < 4; i++) {
                final int index = i;
                EditBox box = new EditBox(font, panelX, y + i * 20, PANEL_W, 16, Component.translatable("gui.exposure_letters.newspaper.placeholder." + hints[i]));
                box.setMaxLength(limits[i]);
                box.setValue(masthead[i]);
                box.setHint(Component.translatable("gui.exposure_letters.newspaper.placeholder." + hints[i]));
                box.setResponder(s -> masthead[index] = s);
                box.setTooltip(Tooltip.create(Component.translatable("gui.exposure_letters.newspaper.placeholder." + hints[i])));
                addRenderableWidget(box);
                if (i == 0) setInitialFocus(box);
            }
            if (masthead[2].isBlank()) {
                addRenderableWidget(Button.builder(Component.translatable("gui.exposure_letters.newspaper.today"), b -> {
                    long day = minecraft.level.getDayTime() / 24000L + 1;
                    masthead[2] = Component.translatable("gui.exposure_letters.newspaper.day", day).getString();
                    rebuildWidgets();
                }).bounds(panelX, y + 82, PANEL_W, 18).build());
            }
        } else if (sel != null) {
            switch (sel.type) {
                case HEADLINE, SUBHEADING, TEXT -> {
                    int h = sel.type == BlockType.TEXT ? Math.min(areaBottom - y, 128) : 40;
                    PaperTextArea area = new PaperTextArea(font, panelX, y, PANEL_W, h, Component.empty())
                            .setMaxLength(sel.type.maxLength)
                            .setMultiline(sel.type == BlockType.TEXT)
                            .setBackgroundColor(0xFFF7F3E8)
                            .setPlaceholder(Component.translatable("gui.exposure_letters.newspaper.placeholder." + sel.type.getSerializedName()))
                            .setResponder(s -> sel.text = s);
                    area.setValue(sel.text);
                    addRenderableWidget(area);
                    setInitialFocus(area);
                }
                case PHOTO -> {
                    EditBox caption = new EditBox(font, panelX, y, PANEL_W, 16, Component.translatable("gui.exposure_letters.newspaper.placeholder.caption"));
                    caption.setMaxLength(BlockType.PHOTO.maxLength);
                    caption.setValue(sel.text);
                    caption.setHint(Component.translatable("gui.exposure_letters.newspaper.placeholder.caption"));
                    caption.setResponder(s -> sel.text = s);
                    addRenderableWidget(caption);
                    pickerY = y + 32;
                    if (!sel.photo.isEmpty()) {
                        addRenderableWidget(Button.builder(Component.translatable("gui.exposure_letters.newspaper.remove_photo"), b -> {
                            sel.photo = ItemStack.EMPTY;
                            sel.sourceSlot = -1;
                            rebuildWidgets();
                        }).bounds(panelX, areaBottom - 20, PANEL_W, 18).build());
                    }
                }
                case DIVIDER -> {}
            }
        }

        // Bottom: done / publish
        int by = panelY + panelH - 20;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(panelX, by, PANEL_W / 2 - 1, 20).build());
        publishButton = addRenderableWidget(Button.builder(Component.translatable("gui.exposure_letters.newspaper.publish"), b -> save(true))
                .bounds(panelX + PANEL_W / 2 + 1, by, PANEL_W / 2 - 1, 20).build());
        updatePublishButton();
    }

    private Button smallButton(int x, int y, String label, String tooltipKey, Button.OnPress onPress) {
        return Button.builder(Component.literal(label), onPress).bounds(x, y, 20, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.exposure_letters.newspaper." + tooltipKey))).build();
    }

    private void updatePublishButton() {
        boolean overflow = anyOverflow();
        boolean titled = !masthead[0].isBlank();
        publishButton.active = !overflow && titled;
        publishButton.setTooltip(Tooltip.create(Component.translatable(overflow ? "gui.exposure_letters.newspaper.publish.overflow"
                : titled ? "gui.exposure_letters.newspaper.publish.tooltip" : "gui.exposure_letters.newspaper.publish.no_title")));
    }

    @Override
    public void tick() {
        super.tick();
        if (publishButton != null) updatePublishButton();
    }

    // ------------------------------------------------------------------ actions

    private void changePage(int delta) {
        page = Math.max(0, Math.min(NewspaperContent.PAGE_COUNT - 1, page + delta));
        selected = page == 0 ? MASTHEAD_INDEX : NONE;
        rebuildWidgets();
    }

    private void addBlock(BlockType type) {
        if (blocks().size() >= NewspaperContent.MAX_BLOCKS_PER_PAGE) return;
        if (type == BlockType.PHOTO && photoCountOnPage() >= NewspaperContent.MAX_PHOTOS_PER_PAGE) return;
        int at = selected >= 0 ? selected + 1 : blocks().size();
        int lastDiv = lastDividerIndex();
        if (type == BlockType.PHOTO && lastDiv >= 0 && at <= lastDiv) {
            at = lastDiv + 1;
        }
        EBlock created = new EBlock(type);
        blocks().add(at, created);
        selected = at;
        if (type == BlockType.DIVIDER || type == BlockType.PHOTO) {
            enforcePhotosBelowDivider(created);
        }
        rebuildWidgets();
    }

    private void move(int delta) {
        if (!canMove(delta)) return;
        int target = selected + delta;
        Collections.swap(blocks(), selected, target);
        selected = target;
        rebuildWidgets();
    }

    private void deleteSelected() {
        if (selected < 0 || selected >= blocks().size()) return;
        blocks().remove(selected);
        selected = NONE;
        rebuildWidgets();
    }

    private void save(boolean publish) {
        if (sent) return;
        sent = true;
        Set<Integer> slots = new LinkedHashSet<>();
        for (List<EBlock> p : pages)
            for (EBlock e : p)
                if (e.sourceSlot >= 0 && !e.photo.isEmpty()) slots.add(e.sourceSlot);
        PacketDistributor.sendToServer(new Payloads.SaveNewspaper(hand, toContent(), new ArrayList<>(slots), publish));
        if (publish) super.onClose();
    }

    @Override
    public void onClose() {
        save(false);
        super.onClose();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        // Click on the page selects a block.
        double px = (mouseX - pageX) / scale - MARGIN, py = (mouseY - pageY) / scale - MARGIN;
        if (mouseX >= pageX && mouseX < pageX + PAGE_W * scale && mouseY >= pageY && mouseY < pageY + PAGE_H * scale) {
            int hit = NONE;
            for (Placed p : layout(page).placed())
                if (p.contains(px, py)) hit = p.index();
            if (hit != selected) {
                selected = hit;
                rebuildWidgets();
            }
            return true;
        }

        // Click in the photo picker assigns a photograph to the selected photo block.
        EBlock sel = selectedBlock();
        if (pickerY >= 0 && sel != null && sel.type == BlockType.PHOTO) {
            List<PickerEntry> entries = availablePhotos();
            for (int i = 0; i < entries.size(); i++) {
                int x = panelX + (i % PICKER_COLS) * 18 + 1, y = pickerY + (i / PICKER_COLS) * 18 + 1;
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    sel.photo = entries.get(i).stack().copyWithCount(1);
                    sel.sourceSlot = entries.get(i).slot();
                    rebuildWidgets();
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        NewspaperPageRenderer.render(g, font, mastheadRecord(), page == 0, records(page), pageX, pageY, scale, page + 1, true, selected);

        Component pageLabel = Component.translatable("gui.exposure_letters.newspaper.page", page + 1, NewspaperContent.PAGE_COUNT);
        g.drawString(font, pageLabel, panelX + (PANEL_W - font.width(pageLabel)) / 2, panelY + 3, 0xFFFFFFFF);

        EBlock sel = selectedBlock();
        if (selected == NONE) {
            List<net.minecraft.util.FormattedCharSequence> hint = font.split(Component.translatable("gui.exposure_letters.newspaper.hint"), PANEL_W);
            for (int i = 0; i < hint.size(); i++)
                g.drawString(font, hint.get(i), panelX, panelY + 66 + i * 10, 0xFFAAAAAA);
        } else if (sel != null && sel.type == BlockType.DIVIDER) {
            g.drawString(font, Component.translatable("gui.exposure_letters.newspaper.divider"), panelX, panelY + 66, 0xFFAAAAAA);
        }

        if (pickerY >= 0 && sel != null && sel.type == BlockType.PHOTO) {
            g.drawString(font, Component.translatable("gui.exposure_letters.newspaper.pick_photo"), panelX, pickerY - 11, 0xFFDDDDDD);
            List<PickerEntry> entries = availablePhotos();
            if (entries.isEmpty())
                g.drawString(font, Component.translatable("gui.exposure_letters.newspaper.no_photos"), panelX, pickerY + 4, 0xFFAAAAAA);
            ItemStack hovered = ItemStack.EMPTY;
            for (int i = 0; i < entries.size(); i++) {
                int x = panelX + (i % PICKER_COLS) * 18, y = pickerY + (i / PICKER_COLS) * 18;
                if (y + 18 > panelY + panelH - 46) break;
                g.fill(x, y, x + 18, y + 18, 0xFF373737);
                g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
                ItemStack stack = entries.get(i).stack();
                g.renderItem(stack, x + 1, y + 1);
                int left = stack.getCount() - usedFromSlot(entries.get(i).slot());
                if (left > 1) g.renderItemDecorations(font, stack, x + 1, y + 1, String.valueOf(left));
                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    g.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
                    hovered = stack;
                }
            }
            if (!hovered.isEmpty()) g.renderTooltip(font, hovered, mouseX, mouseY);
        }

        if (layout(page).overflows()) {
            Component warn = Component.translatable("gui.exposure_letters.newspaper.overflow");
            int pw = (int) (PAGE_W * scale);
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.drawString(font, warn, pageX + (pw - font.width(warn)) / 2, pageY + (int) (PAGE_H * scale) - 12, 0xFFFF3333);
            g.pose().popPose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
