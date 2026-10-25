package com.coolnydude.exposureletters.item;

import com.coolnydude.exposureletters.client.ClientHooks;
import com.coolnydude.exposureletters.component.NewspaperContent;
import com.coolnydude.exposureletters.component.NewspaperMeta;
import com.coolnydude.exposureletters.registry.ModRegistry;
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

public class PrintedNewspaperItem extends Item {
    public PrintedNewspaperItem(Properties properties) {
        super(properties);
    }

    public static NewspaperMeta getMeta(ItemStack stack) {
        return stack.getOrDefault(ModRegistry.NEWSPAPER_META.get(), new NewspaperMeta("", 0));
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
            ClientHooks.openNewspaperReader(NewspaperItem.getContent(stack), null);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Component getName(ItemStack stack) {
        String title = NewspaperItem.getContent(stack).masthead().title();
        return title.isBlank()
                ? super.getName(stack)
                : Component.translatable("item.exposure_letters.printed_newspaper.named", title);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        NewspaperContent content = NewspaperItem.getContent(stack);
        NewspaperMeta meta = getMeta(stack);
        String issue = content.masthead().issue();
        String date = content.masthead().date();
        if (!issue.isBlank() || !date.isBlank()) {
            tooltip.add(Component.literal((issue.isBlank() ? "" : issue + " ") + date).withStyle(ChatFormatting.GRAY));
        }
        if (!meta.author().isBlank()) {
            tooltip.add(Component.translatable("item.exposure_letters.printed_newspaper.author", meta.author()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("book.generation." + meta.generation()).withStyle(ChatFormatting.GRAY));
    }
}
