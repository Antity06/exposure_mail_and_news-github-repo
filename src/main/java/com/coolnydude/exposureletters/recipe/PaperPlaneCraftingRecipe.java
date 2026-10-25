package com.coolnydude.exposureletters.recipe;

import com.coolnydude.exposureletters.component.NoteContent;
import com.coolnydude.exposureletters.item.NoteItem;
import com.coolnydude.exposureletters.item.PaperPlaneItem;
import com.coolnydude.exposureletters.registry.ModRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class PaperPlaneCraftingRecipe extends CustomRecipe {
    private static final boolean[][] PATTERN = {
            {false, true, false},
            {false, true, false},
            {true, true, true}
    };

    public PaperPlaneCraftingRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                ItemStack stack = input.getItem(col, row);
                boolean expectNote = PATTERN[row][col];
                if (expectNote) {
                    if (!stack.is(ModRegistry.NOTE.get())) return false;
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
        ItemStack result = new ItemStack(ModRegistry.PAPER_PLANE.get());
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
            PaperPlaneItem.setContent(result, content);
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.PAPER_PLANE_CRAFTING.get();
    }
}
