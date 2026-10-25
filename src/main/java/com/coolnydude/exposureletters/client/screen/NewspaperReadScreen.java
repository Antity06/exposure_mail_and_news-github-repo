package com.coolnydude.exposureletters.client.screen;

import com.coolnydude.exposureletters.client.render.LecternItemRenderer;
import com.coolnydude.exposureletters.client.render.NewspaperPageRenderer;
import com.coolnydude.exposureletters.component.NewspaperContent;
import com.coolnydude.exposureletters.network.Payloads;
import com.coolnydude.exposureletters.util.LecternBookAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import static com.coolnydude.exposureletters.client.render.NewspaperPageRenderer.PAGE_H;
import static com.coolnydude.exposureletters.client.render.NewspaperPageRenderer.PAGE_W;

public class NewspaperReadScreen extends Screen {
    private static final int SPREADS = 6;
    private static final int GAP = 2;

    private final NewspaperContent content;
    @Nullable
    private final BlockPos lectern;
    private int spread;
    private float scale;
    private int pagesX, pagesY;
    private PageButton back, forward;
    private boolean taking;

    public NewspaperReadScreen(NewspaperContent content, @Nullable BlockPos lectern) {
        super(Component.translatable("item.exposure_letters.printed_newspaper"));
        this.content = content;
        this.lectern = lectern;
    }

    @Override
    protected void init() {
        this.scale = Math.min(1.0F, Math.min((this.height - 40.0F) / PAGE_H, (this.width - 20.0F) / (PAGE_W * 2 + GAP)));
        int spreadW = (int) ((PAGE_W * 2 + GAP) * this.scale);
        this.pagesX = (this.width - spreadW) / 2;
        this.pagesY = 8;
        int bottom = this.pagesY + (int) (PAGE_H * this.scale) + 6;

        this.back = addRenderableWidget(new PageButton(this.pagesX, bottom, false, b -> turn(-1), true));
        this.forward = addRenderableWidget(new PageButton(this.pagesX + spreadW - 23, bottom, true, b -> turn(1), true));

        if (this.lectern != null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.exposure_letters.take"), b -> {
                if (this.taking) return;
                this.taking = true;
                LecternItemRenderer.setClientBook(this.lectern, ItemStack.EMPTY);
                if (this.minecraft != null && this.minecraft.level != null
                        && this.minecraft.level.getBlockEntity(this.lectern) instanceof LecternBookAccess access) {
                    access.exposureLetters$setClientBook(ItemStack.EMPTY);
                }
                PacketDistributor.sendToServer(new Payloads.TakeFromLectern(this.lectern));
                onClose();
            }).bounds(this.width / 2 - 100, bottom, 98, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                    .bounds(this.width / 2 + 2, bottom, 98, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                    .bounds(this.width / 2 - 50, bottom, 100, 20).build());
        }
        updateButtons();
    }

    private void turn(int delta) {
        this.spread = Math.max(0, Math.min(SPREADS - 1, this.spread + delta));
        updateButtons();
    }

    private void updateButtons() {
        this.back.visible = this.spread > 0;
        this.forward.visible = this.spread < SPREADS - 1;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 266 || key == 263) {
            this.back.onPress();
            return true;
        }
        if (key == 267 || key == 262) {
            this.forward.onPress();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            turn(scrollY > 0 ? -1 : 1);
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        for (int side = 0; side < 2; side++) {
            int index = this.spread * 2 + side;
            int x = this.pagesX + (int) (side * (PAGE_W + GAP) * this.scale);
            NewspaperPageRenderer.render(g, this.font, this.content.masthead(), index == 0,
                    this.content.page(index).blocks(), x, this.pagesY, this.scale, index + 1, false, Integer.MIN_VALUE);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
