package com.coolnydude.exposureletters.item;

import com.coolnydude.exposureletters.component.NoteContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class PaperBoatItem extends PlaceOnWaterBlockItem {
    public PaperBoatItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public static NoteContent getContent(ItemStack stack) {
        return NoteItem.getContent(stack);
    }

    public static void setContent(ItemStack stack, NoteContent content) {
        NoteItem.setContent(stack, content);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        NoteContent content = getContent(stack);
        if (!content.isEmpty()) {
            String firstLine = content.text().strip().split("\n", 2)[0];
            if (firstLine.length() > 32) {
                firstLine = firstLine.substring(0, 31) + "…";
            }
            tooltip.add(Component.literal("«" + firstLine + "»").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            if (!content.author().isBlank()) {
                tooltip.add(Component.translatable("item.exposure_letters.note.author", content.author()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
