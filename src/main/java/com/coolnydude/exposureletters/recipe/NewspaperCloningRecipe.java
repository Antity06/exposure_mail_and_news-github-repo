package com.coolnydude.exposureletters.recipe;

import com.coolnydude.exposureletters.component.NewspaperMeta;
import com.coolnydude.exposureletters.item.PrintedNewspaperItem;
import com.coolnydude.exposureletters.registry.ModRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Printed newspaper + N blank newspapers = N copies. The source stays in the grid (like book cloning). */
public class NewspaperCloningRecipe extends CustomRecipe {
    public NewspaperCloningRecipe(CraftingBookCategory category) {
        super(category);
    }

    private record Found(ItemStack source, int blanks) {}

    private static Found find(CraftingInput input) {
        ItemStack source = ItemStack.EMPTY;
        int blanks = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(ModRegistry.PRINTED_NEWSPAPER.get())) {
                if (!source.isEmpty()) return null;
                source = stack;
            } else if (stack.is(ModRegistry.NEWSPAPER.get())) {
                blanks++;
            } else {
                return null;
            }
        }
        if (source.isEmpty() || blanks == 0) return null;
        if (!PrintedNewspaperItem.getMeta(source).canBeCopied()) return null;
        return new Found(source, blanks);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Found found = find(input);
        if (found == null) return ItemStack.EMPTY;
        ItemStack copy = found.source().copyWithCount(found.blanks());
        NewspaperMeta meta = PrintedNewspaperItem.getMeta(found.source());
        copy.set(ModRegistry.NEWSPAPER_META.get(), meta.copy());
        return copy;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> list = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.hasCraftingRemainingItem()) {
                list.set(i, stack.getCraftingRemainingItem());
            } else if (stack.is(ModRegistry.PRINTED_NEWSPAPER.get())) {
                list.set(i, stack.copyWithCount(1));
                break;
            }
        }
        return list;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.NEWSPAPER_CLONING.get();
    }
}
