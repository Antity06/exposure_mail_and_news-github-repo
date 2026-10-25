package com.coolnydude.exposureletters.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface LecternBookAccess {
    ItemStack exposureLetters$takeBook(@Nullable Player player);
    long exposureLetters$getLastRemovedGameTime();
    void exposureLetters$setClientBook(ItemStack stack);
    ItemStack exposureLetters$getRenderBook();
}
