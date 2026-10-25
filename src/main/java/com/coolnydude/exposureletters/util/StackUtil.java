package com.coolnydude.exposureletters.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class StackUtil {
    /**
     * Leaves exactly one item in the hand and moves the rest of the stack to an empty inventory slot (or drops it).
     * The rest must NOT be merged back into the held stack, so placeItemBackInInventory/add can't be used here.
     */
    public static ItemStack keepOneInHand(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getCount() <= 1) return stack;
        ItemStack rest = stack.split(stack.getCount() - 1);
        Inventory inv = player.getInventory();
        int heldSlot = hand == InteractionHand.MAIN_HAND ? inv.selected : Inventory.SLOT_OFFHAND;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i != heldSlot && inv.items.get(i).isEmpty()) {
                inv.items.set(i, rest);
                return stack;
            }
        }
        player.drop(rest, false);
        return stack;
    }

    private StackUtil() {}
}
