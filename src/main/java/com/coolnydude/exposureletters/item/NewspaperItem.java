package com.coolnydude.exposureletters.item;

import com.coolnydude.exposureletters.client.ClientHooks;
import com.coolnydude.exposureletters.component.NewspaperContent;
import com.coolnydude.exposureletters.registry.ModRegistry;
import com.coolnydude.exposureletters.util.StackUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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

public class NewspaperItem extends Item {
    public NewspaperItem(Properties properties) {
        super(properties);
    }

    public static NewspaperContent getContent(ItemStack stack) {
        return stack.getOrDefault(ModRegistry.NEWSPAPER_CONTENT.get(), NewspaperContent.EMPTY);
    }

    public boolean isDraft() {
        return this == ModRegistry.NEWSPAPER_DRAFT.get();
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
        if (!level.isClientSide()) {
            StackUtil.keepOneInHand(player, hand);
        } else {
            ClientHooks.openNewspaperEditor(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (isDraft()) {
            String title = getContent(stack).masthead().title();
            if (!title.isBlank()) {
                tooltip.add(Component.literal("«" + title + "»").withStyle(ChatFormatting.GRAY));
            }
            tooltip.add(Component.translatable("item.exposure_letters.newspaper_draft.hint").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("item.exposure_letters.newspaper.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
