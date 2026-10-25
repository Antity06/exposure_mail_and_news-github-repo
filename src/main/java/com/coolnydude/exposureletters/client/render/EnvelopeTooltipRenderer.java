package com.coolnydude.exposureletters.client.render;

import com.coolnydude.exposureletters.ExposureLetters;
import com.coolnydude.exposureletters.item.EnvelopeItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class EnvelopeTooltipRenderer implements ClientTooltipComponent {
    private static final ResourceLocation TEXTURE = ExposureLetters.id("textures/gui/envelope.png");
    private final EnvelopeItem.EnvelopeTooltip data;

    public EnvelopeTooltipRenderer(EnvelopeItem.EnvelopeTooltip data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return 24;
    }

    @Override
    public int getWidth(Font font) {
        return 94;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        g.blit(TEXTURE, x, y, 0, 180, 94, 22);
        for (int i = 0; i < 5; i++) {
            int sx = x + 2 + i * 18;
            int sy = y + 2;
            g.blit(TEXTURE, sx, sy, 232, 0, 18, 18);
            ItemStack s = i < this.data.items().size() ? this.data.items().get(i) : ItemStack.EMPTY;
            if (!s.isEmpty()) {
                g.renderItem(s, sx + 1, sy + 1);
                g.renderItemDecorations(font, s, sx + 1, sy + 1);
            }
        }
    }
}
