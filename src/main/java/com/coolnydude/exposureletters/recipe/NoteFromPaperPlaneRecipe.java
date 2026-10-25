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

public class NoteFromPaperPlaneRecipe extends CustomRecipe {
    public NoteFromPaperPlaneRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static ItemStack findPlane(CraftingInput input) {
        ItemStack found = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if ((s.is(ModRegistry.PAPER_PLANE.get()) || s.is(ModRegistry.PAPER_BOAT.get())) && found.isEmpty()) {
                found = s;
            } else {
                return ItemStack.EMPTY;
            }
        }
        return found;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !findPlane(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack plane = findPlane(input);
        if (plane.isEmpty()) return ItemStack.EMPTY;
        ItemStack note = new ItemStack(ModRegistry.NOTE.get());
        NoteContent content = PaperPlaneItem.getContent(plane);
        if (!content.isEmpty()) {
            NoteItem.setContent(note, content);
        }
        return note;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.NOTE_FROM_PAPER_PLANE.get();
    }
}
