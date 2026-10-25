package com.coolnydude.exposureletters.item;

import com.coolnydude.exposureletters.client.ClientHooks;
import com.coolnydude.exposureletters.component.NoteContent;
import com.coolnydude.exposureletters.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class NoteItem extends Item {
    public NoteItem(Properties properties) {
        super(properties);
    }

    public static NoteContent getContent(ItemStack stack) {
        return stack.getOrDefault(ModRegistry.NOTE_CONTENT.get(), NoteContent.EMPTY);
    }

    public static void setContent(ItemStack stack, NoteContent content) {
        if (content.isEmpty()) {
            stack.remove(ModRegistry.NOTE_CONTENT.get());
            stack.remove(DataComponents.MAX_STACK_SIZE);
        } else {
            stack.set(ModRegistry.NOTE_CONTENT.get(), content);
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.LECTERN) && !state.getValue(LecternBlock.HAS_BOOK)) {
            return LecternBlock.tryPlaceBook(context.getPlayer(), level, pos, state, context.getItemInHand())
                    ? InteractionResult.sidedSuccess(level.isClientSide())
                    : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            ClientHooks.openNoteInHand(hand, getContent(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        NoteContent content = getContent(stack);
        if (content.isEmpty()) {
            tooltip.add(Component.translatable("item.exposure_letters.note.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
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
