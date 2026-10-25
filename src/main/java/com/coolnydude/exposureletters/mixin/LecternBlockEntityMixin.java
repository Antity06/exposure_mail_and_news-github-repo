package com.coolnydude.exposureletters.mixin;

import com.coolnydude.exposureletters.client.ClientHooks;
import com.coolnydude.exposureletters.client.render.LecternItemRenderer;
import com.coolnydude.exposureletters.network.ServerHandlers;
import com.coolnydude.exposureletters.util.LecternBookAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin extends BlockEntity implements LecternBookAccess {
    @Shadow
    public abstract ItemStack getBook();

    @Shadow
    public abstract void setBook(ItemStack stack);

    @Shadow
    public abstract void clearContent();

    @Unique
    private long exposureLetters$lastRemovedGameTime = -100L;

    @Unique
    private ItemStack exposureLetters$syncedBook = ItemStack.EMPTY;

    private LecternBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "hasBook", at = @At("HEAD"), cancellable = true)
    private void exposureLetters$hasBook(CallbackInfoReturnable<Boolean> cir) {
        ItemStack current = this.getBook();
        if (LecternItemRenderer.isExposureLecternItem(current)
                || LecternItemRenderer.isExposureLecternItem(this.exposureLetters$syncedBook)) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public ItemStack exposureLetters$takeBook(@Nullable Player player) {
        ItemStack current = this.getBook();
        if (current.isEmpty() && this.exposureLetters$syncedBook != null && !this.exposureLetters$syncedBook.isEmpty()) {
            current = this.exposureLetters$syncedBook;
        }
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = current.copy();
        this.exposureLetters$syncedBook = ItemStack.EMPTY;
        this.clearContent();
        this.setBook(ItemStack.EMPTY);
        if (this.level != null) {
            this.exposureLetters$lastRemovedGameTime = this.level.getGameTime();
            if (!this.level.isClientSide()) {
                BlockState state = this.getBlockState();
                if (state.getBlock() instanceof LecternBlock) {
                    LecternBlock.resetBookState(player, this.level, this.worldPosition, state, false);
                    BlockState after = this.level.getBlockState(this.worldPosition);
                    if (after.getBlock() instanceof LecternBlock && after.getValue(LecternBlock.HAS_BOOK)) {
                        after = after.setValue(LecternBlock.POWERED, false).setValue(LecternBlock.HAS_BOOK, false);
                        this.level.setBlock(this.worldPosition, after, 3);
                    }
                    this.setChanged();
                    this.level.sendBlockUpdated(this.worldPosition, state, after, 3);
                    ServerHandlers.broadcastLecternSync(this.level, this.worldPosition, ItemStack.EMPTY);
                }
            }
        }
        return taken;
    }

    @Override
    public long exposureLetters$getLastRemovedGameTime() {
        return this.exposureLetters$lastRemovedGameTime;
    }

    @Override
    public void exposureLetters$setClientBook(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            this.exposureLetters$syncedBook = ItemStack.EMPTY;
        } else {
            this.exposureLetters$syncedBook = stack.copyWithCount(1);
        }
    }

    @Override
    public ItemStack exposureLetters$getRenderBook() {
        if (this.exposureLetters$syncedBook != null && !this.exposureLetters$syncedBook.isEmpty()) {
            return this.exposureLetters$syncedBook;
        }
        ItemStack b = this.getBook();
        return b != null ? b : ItemStack.EMPTY;
    }

    @Inject(method = "setBook(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V", at = @At("TAIL"))
    private void exposureLetters$onSetBook(ItemStack stack, Player player, CallbackInfo ci) {
        this.exposureLetters$syncedBook = (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (this.level != null && !this.level.isClientSide()) {
            this.setChanged();
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
            ServerHandlers.broadcastLecternSync(this.level, this.worldPosition, this.getBook());
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void exposureLetters$afterSave(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        ItemStack current = this.getBook();
        if ((current == null || current.isEmpty()) && this.exposureLetters$syncedBook != null) {
            current = this.exposureLetters$syncedBook;
        }
        if (current != null && !current.isEmpty()) {
            tag.put("ExposureBook", current.save(registries));
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void exposureLetters$afterLoad(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        ItemStack loaded = this.getBook();
        if ((loaded == null || loaded.isEmpty()) && tag.contains("ExposureBook", 10)) {
            loaded = ItemStack.parseOptional(registries, tag.getCompound("ExposureBook"));
        }
        if ((loaded == null || loaded.isEmpty()) && tag.contains("Book", 10)) {
            loaded = ItemStack.parseOptional(registries, tag.getCompound("Book"));
        }
        this.exposureLetters$syncedBook = (loaded == null || loaded.isEmpty()) ? ItemStack.EMPTY : loaded.copyWithCount(1);
        if (this.level != null && this.level.isClientSide() && this.worldPosition != null) {
            ClientHooks.syncLectern(this.worldPosition, this.exposureLetters$syncedBook);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        ItemStack current = this.getBook();
        if ((current == null || current.isEmpty()) && this.exposureLetters$syncedBook != null) {
            current = this.exposureLetters$syncedBook;
        }
        if (current != null && !current.isEmpty()) {
            tag.put("ExposureBook", current.save(registries));
        }
        tag.putBoolean("ExposureLettersSync", true);
        return tag;
    }
}
