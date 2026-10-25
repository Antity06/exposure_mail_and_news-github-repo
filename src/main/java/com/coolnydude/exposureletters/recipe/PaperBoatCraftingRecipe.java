package com.coolnydude.exposureletters.recipe;

import com.coolnydude.exposureletters.component.NoteContent;
import com.coolnydude.exposureletters.item.NoteItem;
import com.coolnydude.exposureletters.item.PaperBoatItem;
import com.coolnydude.exposureletters.registry.ModRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class PaperBoatCraftingRecipe extends CustomRecipe {
    public PaperBoatCraftingRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static boolean isPaperIngredient(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ModRegistry.NOTE.get())
                || stack.is(Items.PAPER)
                || stack.is(ModRegistry.NEWSPAPERS);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() < 2) return false;
        for (int startRow = 0; startRow <= input.height() - 2; startRow++) {
            if (matchesAtRow(input, startRow)) return true;
        }
        return false;
    }

    private static boolean matchesAtRow(CraftingInput input, int r0) {
        for (int row = 0; row < input.height(); row++) {
            for (int col = 0; col < 3; col++) {
                ItemStack stack = input.getItem(col, row);
                boolean expect;
                if (row == r0) {
                    expect = (col == 0 || col == 2);
                } else if (row == r0 + 1) {
                    expect = true;
                } else {
                    expect = false;
                }
                if (expect) {
                    if (!isPaperIngredient(stack)) return false;
                } else {
                    if (!stack.isEmpty()) return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        if (!matches(input, null)) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(ModRegistry.PAPER_BOAT.get());
        NoteContent content = NoteContent.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.is(ModRegistry.NOTE.get())) {
                NoteContent c = NoteItem.getContent(s);
                if (!c.isEmpty()) {
                    content = c;
                    break;
                }
            }
        }
        if (!content.isEmpty()) {
            PaperBoatItem.setContent(result, content);
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.PAPER_BOAT_CRAFTING.get();
    }
}
