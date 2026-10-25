package com.coolnydude.exposureletters.client.screen;

import com.coolnydude.exposureletters.ExposureLetters;
import com.coolnydude.exposureletters.client.render.LecternItemRenderer;
import com.coolnydude.exposureletters.client.widget.PaperTextArea;
import com.coolnydude.exposureletters.component.NoteContent;
import com.coolnydude.exposureletters.network.Payloads;
import com.coolnydude.exposureletters.util.LecternBookAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class NoteScreen extends Screen {
    private static final ResourceLocation PAPER_TEXTURE = ExposureLetters.id("textures/gui/note.png");
    public static final int PAPER_W = 156;
    public static final int PAPER_H = 192;

    private final InteractionHand hand;
    @Nullable
    private final BlockPos lectern;
    private final String original;
    private String text;
    private PaperTextArea area;
    private boolean saved;
    private boolean taking;
    private int paperLeft;
    private int paperTop;

    public NoteScreen(NoteContent content, InteractionHand hand, @Nullable BlockPos lectern) {
        super(Component.translatable("item.exposure_letters.note"));
        this.hand = hand;
        this.lectern = lectern;
        this.original = content.text();
        this.text = content.text();
    }

    @Override
    protected void init() {
        this.paperLeft = (this.width - PAPER_W) / 2;
        this.paperTop = Math.max(4, (this.height - PAPER_H - 28) / 2);

        this.area = new PaperTextArea(this.font, this.paperLeft + 14, this.paperTop + 20, PAPER_W - 28, PAPER_H - 44, Component.empty())
                .setMaxLength(1024)
                .setTextColor(0xFF221E18)
                .setPlaceholder(Component.translatable("gui.exposure_letters.note.placeholder"))
                .setResponder(s -> this.text = s);
        this.area.setValue(this.text);
        addRenderableWidget(this.area);
        setInitialFocus(this.area);

        int buttonsY = this.paperTop + PAPER_H + 6;
        if (this.lectern != null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.exposure_letters.take"), b -> {
                if (this.taking) return;
                this.taking = true;
                save();
                this.saved = true;
                LecternItemRenderer.setClientBook(this.lectern, ItemStack.EMPTY);
                if (this.minecraft != null && this.minecraft.level != null
                        && this.minecraft.level.getBlockEntity(this.lectern) instanceof LecternBookAccess access) {
                    access.exposureLetters$setClientBook(ItemStack.EMPTY);
                }
                PacketDistributor.sendToServer(new Payloads.TakeFromLectern(this.lectern));
                super.onClose();
            }).bounds(this.width / 2 - 100, buttonsY, 98, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                    .bounds(this.width / 2 + 2, buttonsY, 98, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                    .bounds(this.width / 2 - 50, buttonsY, 100, 20).build());
        }
    }

    private void save() {
        if (this.saved || this.text.equals(this.original)) return;
        this.saved = true;
        PacketDistributor.sendToServer(new Payloads.EditNote(Optional.ofNullable(this.lectern), this.hand, this.text));
    }

    @Override
    public void onClose() {
        save();
        super.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.blit(PAPER_TEXTURE, this.paperLeft, this.paperTop, 0, 0, PAPER_W, PAPER_H);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        Component counter = Component.literal(this.text.length() + "/1024");
        g.drawString(this.font, counter,
                this.paperLeft + PAPER_W - 14 - this.font.width(counter),
                this.paperTop + PAPER_H - 17, 0xFF8A8270, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
