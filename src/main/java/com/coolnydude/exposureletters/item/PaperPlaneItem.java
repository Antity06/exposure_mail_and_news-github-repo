package com.coolnydude.exposureletters.item;

import com.coolnydude.exposureletters.component.NoteContent;
import com.coolnydude.exposureletters.entity.PaperPlane;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public class PaperPlaneItem extends Item {
    public static final int FULL_CHARGE_TICKS = 20;

    public PaperPlaneItem(Properties properties) {
        super(properties);
    }

    public static NoteContent getContent(ItemStack stack) {
        return NoteItem.getContent(stack);
    }

    public static void setContent(ItemStack stack, NoteContent content) {
        NoteItem.setContent(stack, content);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        int charged = getUseDuration(stack, entity) - timeLeft;
        float power = Math.min(charged / (float) FULL_CHARGE_TICKS, 1.0F);
        float velocity = 0.45F + power * 1.05F;

        if (!level.isClientSide()) {
            PaperPlane plane = new PaperPlane(level, player, stack.copyWithCount(1));
            plane.shootFromRotation(player, player.getXRot() - 4.0F, player.getYRot(), 0.0F, velocity, 0.5F);
            if (player.getAbilities().instabuild) {
                plane.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            level.addFreshEntity(plane);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
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
