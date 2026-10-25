package com.coolnydude.exposureletters.mixin;

import com.coolnydude.exposureletters.client.render.LecternItemRenderer;
import com.coolnydude.exposureletters.item.NewspaperItem;
import com.coolnydude.exposureletters.item.NoteItem;
import com.coolnydude.exposureletters.item.PrintedNewspaperItem;
import com.coolnydude.exposureletters.network.Payloads;
import com.coolnydude.exposureletters.network.ServerHandlers;
import com.coolnydude.exposureletters.registry.ModRegistry;
import com.coolnydude.exposureletters.util.LecternBookAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternBlock.class)
public abstract class LecternBlockMixin {
    @Inject(method = "tryPlaceBook", at = @At("HEAD"), cancellable = true)
    private static void exposureLetters$beforeTryPlaceBook(LivingEntity entity, Level level, BlockPos pos, BlockState state, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (level.getBlockEntity(pos) instanceof LecternBookAccess access) {
            if (level.getGameTime() - access.exposureLetters$getLastRemovedGameTime() < 5L) {
                cir.setReturnValue(false);
                return;
            }
            if (!state.getValue(LecternBlock.HAS_BOOK)
                    && LecternItemRenderer.isExposureLecternItem(stack)) {
                ItemStack copy = stack.copyWithCount(1);
                access.exposureLetters$setClientBook(copy);
                if (level.isClientSide()) {
                    LecternItemRenderer.setClientBook(pos, copy);
                }
            }
        }
    }

    @Inject(method = "tryPlaceBook", at = @At("TAIL"))
    private static void exposureLetters$afterTryPlaceBook(LivingEntity entity, Level level, BlockPos pos, BlockState state, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!level.isClientSide() && level.getBlockState(pos).getValue(LecternBlock.HAS_BOOK)
                && level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) {
            BlockState current = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, current, 3);
            ItemStack book = lectern.getBook();
            if ((book == null || book.isEmpty()) && lectern instanceof LecternBookAccess access) {
                book = access.exposureLetters$getRenderBook();
            }
            ServerHandlers.broadcastLecternSync(level, pos, book);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void exposureLetters$onUseItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (state.getValue(LecternBlock.HAS_BOOK) && player.isSecondaryUseActive()
                && level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) {
            ItemStack book = lectern.getBook();
            if (book.isEmpty() && lectern instanceof LecternBookAccess access) {
                book = access.exposureLetters$getRenderBook();
            }
            if (LecternItemRenderer.isExposureLecternItem(book)) {
                if (level.isClientSide() && lectern instanceof LecternBookAccess clientAccess) {
                    clientAccess.exposureLetters$setClientBook(ItemStack.EMPTY);
                    LecternItemRenderer.setClientBook(pos, ItemStack.EMPTY);
                } else if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && lectern instanceof LecternBookAccess access) {
                    ItemStack taken = access.exposureLetters$takeBook(serverPlayer);
                    if (!taken.isEmpty()) {
                        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.2F);
                        giveTakenToPlayer(serverPlayer, taken);
                    }
                }
                cir.setReturnValue(ItemInteractionResult.sidedSuccess(level.isClientSide()));
            }
        }
    }

    @Inject(method = "resetBookState", at = @At("TAIL"))
    private static void exposureLetters$afterResetBookState(Entity entity, Level level, BlockPos pos, BlockState state, boolean hasBook, CallbackInfo ci) {
        if (!level.isClientSide()) {
            BlockState current = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, current, 3);
            if (level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) {
                ItemStack book = lectern.getBook();
                if ((book == null || book.isEmpty()) && lectern instanceof LecternBookAccess access) {
                    book = access.exposureLetters$getRenderBook();
                }
                ServerHandlers.broadcastLecternSync(level, pos, hasBook ? book : ItemStack.EMPTY);
            }
        }
    }

    @Inject(method = "openScreen", at = @At("HEAD"), cancellable = true)
    private void exposureLetters$openScreen(Level level, BlockPos pos, Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern))
            return;
        ItemStack book = lectern.getBook();
        if ((book == null || book.isEmpty()) && lectern instanceof LecternBookAccess access) {
            book = access.exposureLetters$getRenderBook();
        }
        if (LecternItemRenderer.isExposureLecternItem(book)) {
            ServerHandlers.broadcastLecternSync(level, pos, book);
            if (serverPlayer.isSecondaryUseActive() && lectern instanceof LecternBookAccess access) {
                ItemStack taken = access.exposureLetters$takeBook(serverPlayer);
                if (!taken.isEmpty()) {
                    level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.2F);
                    giveTakenToPlayer(serverPlayer, taken);
                }
                ci.cancel();
                return;
            }
            if (book.is(ModRegistry.NOTE.get())) {
                PacketDistributor.sendToPlayer(serverPlayer, new Payloads.OpenLecternNote(pos, NoteItem.getContent(book)));
                ci.cancel();
            } else if (LecternItemRenderer.isNewspaperItem(book)) {
                PacketDistributor.sendToPlayer(serverPlayer, new Payloads.OpenLecternNewspaper(pos,
                        NewspaperItem.getContent(book), PrintedNewspaperItem.getMeta(book)));
                ci.cancel();
            }
        }
    }

    private static void giveTakenToPlayer(ServerPlayer player, ItemStack taken) {
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
}
