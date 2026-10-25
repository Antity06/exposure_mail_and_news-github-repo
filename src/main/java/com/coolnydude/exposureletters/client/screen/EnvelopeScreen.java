package com.coolnydude.exposureletters.client.screen;

import com.coolnydude.exposureletters.ExposureLetters;
import com.coolnydude.exposureletters.client.widget.PaperTextArea;
import com.coolnydude.exposureletters.component.EnvelopeAddress;
import com.coolnydude.exposureletters.item.EnvelopeItem;
import com.coolnydude.exposureletters.item.StampItem;
import com.coolnydude.exposureletters.network.Payloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class EnvelopeScreen extends Screen {
    private static final ResourceLocation TEXTURE = ExposureLetters.id("textures/gui/envelope.png");
    private static final ResourceLocation STAMPS = ExposureLetters.id("textures/gui/stamps.png");
    public static final int W = 220;
    public static final int H = 122;
    public static final int STAMP_X = 12;
    public static final int STAMP_Y = 88;
    public static final int STAMP_W = 24;
    public static final int STAMP_H = 28;
    private static final int INK = 0xFF3B352D;
    private static final int LABEL = 0xFF7A6D5E;

    private final InteractionHand hand;
    private String from;
    private String to;
    private int left;
    private int top;
    private boolean signing;
    private boolean sent;

    private PaperTextArea fromArea;
    private PaperTextArea toArea;
    private EditBox sealImprintBox;
    private Button sealButton;
    private Button finalizeButton;
    private Button cancelButton;

    public EnvelopeScreen(InteractionHand hand) {
        super(Component.translatable("item.exposure_letters.envelope"));
        this.hand = hand;
    }

    private ItemStack envelope() {
        return this.minecraft.player.getItemInHand(this.hand);
    }

    @Override
    protected void init() {
        if (this.from == null) {
            EnvelopeAddress address = EnvelopeItem.getAddress(envelope());
            this.from = address.from().isBlank() ? this.minecraft.player.getGameProfile().getName() : address.from();
            this.to = address.to();
        }
        this.left = (this.width - W) / 2;
        this.top = (this.height - H - 26) / 2;

        int by = this.top + H + 6;

        if (this.signing) {
            this.sealImprintBox = new EditBox(this.font, this.left + 20, this.top + 48, W - 40, 16,
                    Component.translatable("gui.exposure_letters.envelope.from"));
            this.sealImprintBox.setMaxLength(EnvelopeAddress.MAX_LENGTH);
            if (this.from.isBlank()) {
                this.from = this.minecraft.player.getGameProfile().getName();
            }
            this.sealImprintBox.setValue(this.from);
            this.sealImprintBox.setResponder(s -> {
                this.from = s;
                updateSealButton();
            });
            addRenderableWidget(this.sealImprintBox);
            setInitialFocus(this.sealImprintBox);

            this.finalizeButton = addRenderableWidget(Button.builder(Component.translatable("gui.exposure_letters.envelope.seal"), b -> {
                send(true);
                super.onClose();
            }).bounds(this.width / 2 - 102, by, 100, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.exposure_letters.envelope.seal.tooltip")))
                    .build());

            this.cancelButton = addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> {
                this.signing = false;
                rebuildWidgets();
            }).bounds(this.width / 2 + 2, by, 100, 20).build());
        } else {
            Component fromLabel = Component.translatable("gui.exposure_letters.envelope.from");
            Component toLabel = Component.translatable("gui.exposure_letters.envelope.to");

            int fx = this.left + 14 + this.font.width(fromLabel) + 3;
            this.fromArea = new PaperTextArea(this.font, fx, this.top + 68, this.left + 150 - fx, 13, fromLabel)
                    .setMultiline(false)
                    .setMaxLength(EnvelopeAddress.MAX_LENGTH)
                    .setTextColor(INK)
                    .setPlaceholder(Component.translatable("gui.exposure_letters.envelope.from.placeholder"))
                    .setResponder(s -> this.from = s);
            this.fromArea.setValue(this.from);

            int tx = this.left + 70 + this.font.width(toLabel) + 3;
            this.toArea = new PaperTextArea(this.font, tx, this.top + 92, this.left + 208 - tx, 13, toLabel)
                    .setMultiline(false)
                    .setMaxLength(EnvelopeAddress.MAX_LENGTH)
                    .setTextColor(INK)
                    .setPlaceholder(Component.translatable("gui.exposure_letters.envelope.to.placeholder"))
                    .setResponder(s -> this.to = s);
            this.toArea.setValue(this.to);

            addRenderableWidget(this.fromArea);
            addRenderableWidget(this.toArea);
            setInitialFocus(this.toArea);

            this.sealButton = addRenderableWidget(Button.builder(Component.translatable("gui.exposure_letters.envelope.seal"), b -> {
                if (EnvelopeItem.countContents(envelope()) > 0 && EnvelopeItem.canEdit(envelope())) {
                    this.signing = true;
                    rebuildWidgets();
                }
            }).bounds(this.width / 2 - 102, by, 100, 20).build());

            addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                    .bounds(this.width / 2 + 2, by, 100, 20).build());
        }

        updateSealButton();
    }

    private void updateSealButton() {
        ItemStack env = envelope();
        boolean hasContents = EnvelopeItem.countContents(env) > 0;
        boolean editable = EnvelopeItem.canEdit(env);
        if (this.sealButton != null) {
            this.sealButton.active = hasContents && editable;
            this.sealButton.setTooltip(Tooltip.create(Component.translatable(
                    !hasContents ? "gui.exposure_letters.envelope.seal.empty" : "gui.exposure_letters.envelope.seal.tooltip")));
        }
        if (this.finalizeButton != null) {
            this.finalizeButton.active = hasContents && editable && !this.from.strip().isEmpty();
        }
    }

    private void send(boolean seal) {
        if (this.sent) return;
        this.sent = true;
        PacketDistributor.sendToServer(new Payloads.UpdateEnvelope(this.hand, this.from, this.to, seal));
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (this.signing && key == GLFW.GLFW_KEY_ESCAPE) {
            this.signing = false;
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        send(false);
        super.onClose();
    }

    @Override
    public void tick() {
        if (!(envelope().getItem() instanceof EnvelopeItem) || EnvelopeItem.isSealed(envelope())) {
            super.onClose();
            return;
        }
        if (this.signing && EnvelopeItem.countContents(envelope()) == 0) {
            this.signing = false;
            rebuildWidgets();
            return;
        }
        updateSealButton();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.blit(TEXTURE, this.left, this.top, 0, 0, W, H);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        if (this.signing) {
            g.drawString(this.font, Component.translatable("gui.exposure_letters.envelope.from"), this.left + 20, this.top + 35, LABEL, false);
            List<FormattedCharSequence> tipLines = this.font.split(Component.translatable("gui.exposure_letters.envelope.seal.tooltip"), W - 40);
            for (int i = 0; i < tipLines.size(); i++) {
                g.drawString(this.font, tipLines.get(i), this.left + 20, this.top + 72 + i * 10, LABEL, false);
            }
            return;
        }

        g.drawString(this.font, Component.translatable("gui.exposure_letters.envelope.from"), this.left + 14, this.top + 70, LABEL, false);
        g.drawString(this.font, Component.translatable("gui.exposure_letters.envelope.to"), this.left + 70, this.top + 94, LABEL, false);

        Component count = Component.translatable("gui.exposure_letters.envelope.count",
                EnvelopeItem.countContents(envelope()), EnvelopeItem.SLOTS);
        g.drawString(this.font, count, this.left + W - 10 - this.font.width(count), this.top + H - 12, LABEL, false);

        int sx = this.left + STAMP_X;
        int sy = this.top + STAMP_Y;
        String stamp = EnvelopeItem.getStamp(envelope());
        if (stamp != null) {
            int index = Math.max(0, StampItem.DESIGNS.indexOf(stamp));
            g.blit(STAMPS, sx, sy, index * STAMP_W, 0.0F, STAMP_W, STAMP_H, 256, 32);
        }
        if (mouseX >= sx && mouseX < sx + STAMP_W && mouseY >= sy && mouseY < sy + STAMP_H) {
            Component tip = stamp != null
                    ? StampItem.designName(stamp)
                    : Component.translatable("gui.exposure_letters.envelope.stamp.empty");
            g.renderTooltip(this.font, this.font.split(tip, 180), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
