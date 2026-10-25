package com.coolnydude.exposureletters.item;

import com.coolnydude.exposureletters.client.ClientHooks;
import com.coolnydude.exposureletters.component.EnvelopeAddress;
import com.coolnydude.exposureletters.component.SealInfo;
import com.coolnydude.exposureletters.registry.ModRegistry;
import com.coolnydude.exposureletters.util.StackUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnvelopeItem extends Item {
    public static final int SLOTS = 5;

    public EnvelopeItem(Properties properties) {
        super(properties);
    }

    public static boolean isSealed(ItemStack stack) {
        return stack.has(ModRegistry.SEALED.get());
    }

    public static boolean canEdit(ItemStack stack) {
        return stack.getItem() instanceof EnvelopeItem && stack.getCount() == 1 && !isSealed(stack);
    }

    public static EnvelopeAddress getAddress(ItemStack stack) {
        return stack.getOrDefault(ModRegistry.ENVELOPE_ADDRESS.get(), EnvelopeAddress.EMPTY);
    }

    public static List<ItemStack> getContents(ItemStack stack) {
        ItemContainerContents contents = stack.getOrDefault(ModRegistry.ENVELOPE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        contents.copyInto(list);
        return list;
    }

    public static int countContents(ItemStack stack) {
        return (int) getContents(stack).stream().filter(s -> !s.isEmpty()).count();
    }

    public static void setContents(ItemStack stack, List<ItemStack> items) {
        List<ItemStack> packed = new ArrayList<>();
        for (ItemStack s : items) {
            if (!s.isEmpty()) packed.add(s);
        }
        if (packed.isEmpty()) {
            stack.remove(ModRegistry.ENVELOPE_CONTENTS.get());
            stack.remove(DataComponents.MAX_STACK_SIZE);
        } else {
            stack.set(ModRegistry.ENVELOPE_CONTENTS.get(), ItemContainerContents.fromItems(packed));
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        }
    }

    public static boolean canInsert(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModRegistry.ENVELOPE_INSERTABLE) && !(stack.getItem() instanceof EnvelopeItem);
    }

    private static boolean insertOne(ItemStack envelope, ItemStack item) {
        if (!canEdit(envelope) || !canInsert(item)) return false;
        List<ItemStack> contents = new ArrayList<>(getContents(envelope));
        contents.removeIf(ItemStack::isEmpty);
        if (contents.size() >= SLOTS) return false;
        contents.add(item.split(1));
        setContents(envelope, contents);
        return true;
    }

    private static ItemStack removeLast(ItemStack envelope) {
        if (!canEdit(envelope)) return ItemStack.EMPTY;
        List<ItemStack> contents = new ArrayList<>(getContents(envelope));
        contents.removeIf(ItemStack::isEmpty);
        if (contents.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = contents.remove(contents.size() - 1);
        setContents(envelope, contents);
        return out;
    }

    public static String getStamp(ItemStack envelope) {
        return envelope.get(ModRegistry.STAMP.get());
    }

    public static boolean hasStamp(ItemStack envelope) {
        return getStamp(envelope) != null;
    }

    private static boolean applyStamp(ItemStack envelope, ItemStack stamp, Player player) {
        if (!(envelope.getItem() instanceof EnvelopeItem) || envelope.getCount() != 1 || hasStamp(envelope)
                || !(stamp.getItem() instanceof StampItem)) {
            return false;
        }
        envelope.set(ModRegistry.STAMP.get(), StampItem.getDesign(stamp));
        envelope.set(DataComponents.MAX_STACK_SIZE, 1);
        stamp.shrink(1);
        player.playSound(SoundEvents.BOOK_PUT, 0.7F, 1.6F);
        return true;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack envelope, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack target = slot.getItem();
        if (target.getItem() instanceof StampItem) {
            if (!slot.allowModification(player)) return false;
            ItemStack taken = slot.safeTake(target.getCount(), 1, player);
            if (!applyStamp(envelope, taken, player) && !taken.isEmpty()) {
                slot.safeInsert(taken);
            }
            return true;
        }
        if (!canEdit(envelope)) return false;
        if (target.isEmpty()) {
            ItemStack out = removeLast(envelope);
            if (!out.isEmpty()) {
                ItemStack rest = slot.safeInsert(out);
                if (!rest.isEmpty()) insertOne(envelope, rest);
                playRemoveSound(player);
            }
        } else if (canInsert(target) && countContents(envelope) < SLOTS) {
            ItemStack taken = slot.safeTake(target.getCount(), 1, player);
            if (insertOne(envelope, taken)) {
                playInsertSound(player);
            } else if (!taken.isEmpty()) {
                slot.safeInsert(taken);
            }
        }
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack envelope, ItemStack cursor, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        if (cursor.getItem() instanceof StampItem) {
            applyStamp(envelope, cursor, player);
            return true;
        }
        if (!canEdit(envelope)) return false;
        if (cursor.isEmpty()) {
            ItemStack out = removeLast(envelope);
            if (!out.isEmpty()) {
                access.set(out);
                playRemoveSound(player);
            }
        } else if (canInsert(cursor)) {
            if (insertOne(envelope, cursor)) {
                playInsertSound(player);
            }
        } else {
            return false;
        }
        return true;
    }

    private static void playInsertSound(Player player) {
        player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.2F + player.level().getRandom().nextFloat() * 0.2F);
    }

    private static void playRemoveSound(Player player) {
        player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 0.9F + player.level().getRandom().nextFloat() * 0.2F);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (isSealed(stack)) return Optional.empty();
        return Optional.of(new EnvelopeTooltip(getContents(stack)));
    }

    public record EnvelopeTooltip(List<ItemStack> items) implements TooltipComponent {}

    public static boolean seal(Player player, ItemStack envelope) {
        return seal(player, envelope, "");
    }

    public static boolean seal(Player player, ItemStack envelope, String customSealName) {
        if (!canEdit(envelope)) return false;
        if (countContents(envelope) == 0) {
            player.displayClientMessage(Component.translatable("message.exposure_letters.envelope_empty"), true);
            return false;
        }
        if (!player.level().isClientSide()) {
            EnvelopeAddress address = getAddress(envelope);
            String sender = address.from().isBlank() ? player.getGameProfile().getName() : address.from();
            if (address.from().isBlank()) {
                envelope.set(ModRegistry.ENVELOPE_ADDRESS.get(), new EnvelopeAddress(sender, address.to()));
            }
            String imprint = (customSealName == null || customSealName.isBlank())
                    ? sender
                    : EnvelopeAddress.clamp(customSealName);
            envelope.set(ModRegistry.SEALED.get(), new SealInfo(imprint));
            envelope.set(DataComponents.MAX_STACK_SIZE, 1);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.HONEYCOMB_WAX_ON, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isSealed(stack)) {
            if (!level.isClientSide()) {
                List<ItemStack> contents = getContents(stack);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER)),
                            player.getX(), player.getEyeY() - 0.3D, player.getZ(), 12, 0.2D, 0.2D, 0.2D, 0.05D);
                }
                stack.shrink(1);
                for (ItemStack item : contents) {
                    if (!item.isEmpty()) {
                        player.getInventory().placeItemBackInInventory(item.copy());
                    }
                }
            }
            return InteractionResultHolder.sidedSuccess(stack.isEmpty() ? ItemStack.EMPTY : stack, level.isClientSide());
        }

        if (!level.isClientSide()) {
            StackUtil.keepOneInHand(player, hand);
        } else {
            ClientHooks.openEnvelope(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EnvelopeAddress address = getAddress(stack);
        if (!address.from().isBlank()) {
            tooltip.add(Component.translatable("item.exposure_letters.envelope.from", address.from()).withStyle(ChatFormatting.GRAY));
        }
        if (!address.to().isBlank()) {
            tooltip.add(Component.translatable("item.exposure_letters.envelope.to", address.to()).withStyle(ChatFormatting.GRAY));
        }
        if (hasStamp(stack)) {
            tooltip.add(Component.translatable("item.exposure_letters.envelope.stamp",
                    StampItem.designName(StampItem.getDesign(stack))).withStyle(ChatFormatting.GRAY));
        }
        SealInfo seal = stack.get(ModRegistry.SEALED.get());
        if (seal != null) {
            tooltip.add(Component.translatable("item.exposure_letters.envelope.sealed_by", seal.sealName()).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.exposure_letters.envelope.count", countContents(stack)).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("item.exposure_letters.envelope.open_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.translatable("item.exposure_letters.envelope.insert_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
