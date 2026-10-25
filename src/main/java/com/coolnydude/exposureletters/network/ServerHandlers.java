package com.coolnydude.exposureletters.network;

import com.coolnydude.exposureletters.client.render.LecternItemRenderer;
import com.coolnydude.exposureletters.component.EnvelopeAddress;
import com.coolnydude.exposureletters.component.NewspaperContent;
import com.coolnydude.exposureletters.component.NewspaperMeta;
import com.coolnydude.exposureletters.component.NoteContent;
import com.coolnydude.exposureletters.item.EnvelopeItem;
import com.coolnydude.exposureletters.item.NewspaperItem;
import com.coolnydude.exposureletters.item.NoteItem;
import com.coolnydude.exposureletters.registry.ModRegistry;
import com.coolnydude.exposureletters.util.LecternBookAccess;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class ServerHandlers {
    private static final double MAX_LECTERN_DISTANCE_SQR = 64.0D;

    public static void broadcastLecternSync(Level level, BlockPos pos, ItemStack book) {
        if (level instanceof ServerLevel serverLevel && pos != null) {
            Payloads.SyncLectern payload = new Payloads.SyncLectern(pos, book == null ? ItemStack.EMPTY : book.copy());
            for (ServerPlayer sp : serverLevel.players()) {
                PacketDistributor.sendToPlayer(sp, payload);
            }
        }
    }

    public static void requestLecternSync(Payloads.RequestLecternSync payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        BlockPos pos = payload.pos();
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos)) return;
        if (level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) {
            PacketDistributor.sendToPlayer(player, new Payloads.SyncLectern(pos, lectern.getBook().copy()));
        }
    }

    public static void editNote(Payloads.EditNote payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        String text = NewspaperContent.clamp(payload.text(), 1024);
        NoteContent content = new NoteContent(text, text.isBlank() ? "" : player.getGameProfile().getName());

        if (payload.lectern().isPresent()) {
            BlockPos pos = payload.lectern().get();
            LecternBlockEntity lectern = getLectern(player, pos);
            if (lectern == null || !lectern.getBook().is(ModRegistry.NOTE.get())) return;
            BlockState state = player.level().getBlockState(pos);
            if (!(state.getBlock() instanceof LecternBlock) || !state.getValue(LecternBlock.HAS_BOOK)) return;
            ItemStack book = lectern.getBook().copy();
            NoteItem.setContent(book, content);
            lectern.setBook(book);
            player.level().sendBlockUpdated(pos, state, state, 3);
            broadcastLecternSync(player.level(), pos, book);
            return;
        }

        ItemStack stack = player.getItemInHand(payload.hand());
        if (!stack.is(ModRegistry.NOTE.get())) return;
        if (stack.getCount() == 1) {
            NoteItem.setContent(stack, content);
        } else if (!content.isEmpty()) {
            ItemStack written = stack.copyWithCount(1);
            NoteItem.setContent(written, content);
            stack.shrink(1);
            player.getInventory().placeItemBackInInventory(written);
        }
    }

    public static void takeFromLectern(Payloads.TakeFromLectern payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        BlockPos pos = payload.pos();
        LecternBlockEntity lectern = getLectern(player, pos);
        if (lectern == null) return;
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof LecternBlock) || !state.getValue(LecternBlock.HAS_BOOK)) return;

        ItemStack book = lectern.getBook();
        if (!LecternItemRenderer.isExposureLecternItem(book)) return;

        ItemStack taken = ((LecternBookAccess) lectern).exposureLetters$takeBook(player);
        if (taken.isEmpty()) return;

        player.level().playSound(null, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.2F);

        if (player.getAbilities().instabuild) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (ItemStack.isSameItemSameComponents(player.getInventory().getItem(i), taken)) {
                    player.inventoryMenu.broadcastChanges();
                    return;
                }
            }
        }
        player.getInventory().placeItemBackInInventory(taken);
        player.inventoryMenu.broadcastChanges();
    }

    private static LecternBlockEntity getLectern(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_LECTERN_DISTANCE_SQR) return null;
        return level.getBlockEntity(pos) instanceof LecternBlockEntity lectern ? lectern : null;
    }

    public static void updateEnvelope(Payloads.UpdateEnvelope payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack envelope = player.getItemInHand(payload.hand());
        if (!EnvelopeItem.canEdit(envelope)) return;

        EnvelopeAddress address = new EnvelopeAddress(
                EnvelopeAddress.clamp(payload.from()),
                EnvelopeAddress.clamp(payload.to()));
        if (address.from().isEmpty() && address.to().isEmpty()) {
            envelope.remove(ModRegistry.ENVELOPE_ADDRESS.get());
        } else {
            envelope.set(ModRegistry.ENVELOPE_ADDRESS.get(), address);
        }

        if (payload.seal()) {
            EnvelopeItem.seal(player, envelope, address.from());
        }
    }

    public static void saveNewspaper(Payloads.SaveNewspaper payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getItemInHand(payload.hand());
        if (!(stack.getItem() instanceof NewspaperItem)) return;

        NewspaperContent content = payload.content().sanitized();
        List<ItemStack> oldPhotos = new ArrayList<>(NewspaperItem.getContent(stack).photos());
        if (stack.getCount() > 1) oldPhotos.clear();

        Map<Integer, Integer> available = new HashMap<>();
        for (int slot : new LinkedHashSet<>(payload.usedSlots())) {
            if (slot < 0 || slot >= player.getInventory().getContainerSize()) continue;
            ItemStack s = player.getInventory().getItem(slot);
            if (s.getItem() instanceof PhotographItem) {
                available.put(slot, s.getCount());
            }
        }

        Map<Integer, Integer> toTake = new HashMap<>();
        List<ItemStack> pool = new ArrayList<>(oldPhotos);

        outer:
        for (ItemStack photo : content.photos()) {
            if (!(photo.getItem() instanceof PhotographItem)) {
                reject(player);
                return;
            }
            for (Iterator<ItemStack> it = pool.iterator(); it.hasNext(); ) {
                if (ItemStack.isSameItemSameComponents(it.next(), photo)) {
                    it.remove();
                    continue outer;
                }
            }
            for (Map.Entry<Integer, Integer> e : available.entrySet()) {
                if (e.getValue() > 0 && ItemStack.isSameItemSameComponents(player.getInventory().getItem(e.getKey()), photo)) {
                    e.setValue(e.getValue() - 1);
                    toTake.merge(e.getKey(), 1, Integer::sum);
                    continue outer;
                }
            }
            reject(player);
            return;
        }

        toTake.forEach((slot, count) -> player.getInventory().getItem(slot).shrink(count));
        for (ItemStack leftover : pool) {
            player.getInventory().placeItemBackInInventory(leftover.copy());
        }

        ItemStack result;
        if (payload.publish() && !content.isEmpty()) {
            result = new ItemStack(ModRegistry.PRINTED_NEWSPAPER.get());
            result.set(ModRegistry.NEWSPAPER_CONTENT.get(), content);
            result.set(ModRegistry.NEWSPAPER_META.get(), new NewspaperMeta(player.getGameProfile().getName(), 0));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else if (content.isEmpty()) {
            result = new ItemStack(ModRegistry.NEWSPAPER.get());
        } else {
            result = new ItemStack(ModRegistry.NEWSPAPER_DRAFT.get());
            result.set(ModRegistry.NEWSPAPER_CONTENT.get(), content);
        }

        if (stack.getCount() == 1) {
            player.setItemInHand(payload.hand(), result);
        } else if (!result.is(ModRegistry.NEWSPAPER.get())) {
            stack.shrink(1);
            player.getInventory().placeItemBackInInventory(result);
        }
        player.inventoryMenu.broadcastChanges();
    }

    private static void reject(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.exposure_letters.newspaper_rejected"), true);
    }

    private ServerHandlers() {}
}
