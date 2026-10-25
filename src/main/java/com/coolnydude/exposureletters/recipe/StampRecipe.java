package com.coolnydude.exposureletters.recipe;

import com.coolnydude.exposureletters.item.StampItem;
import com.coolnydude.exposureletters.registry.ModRegistry;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class StampRecipe extends CustomRecipe {
    public StampRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static BannerPatternItem pattern(CraftingInput input) {
        BannerPatternItem pat = null;
        int photos = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof PhotographItem) {
                photos++;
            } else if (s.getItem() instanceof BannerPatternItem bp && pat == null) {
                pat = bp;
            } else {
                return null;
            }
        }
        return photos == 1 ? pat : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return pattern(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        BannerPatternItem pat = pattern(input);
        if (pat == null) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(ModRegistry.STAMP_ITEM.get(), 4);
        out.set(ModRegistry.STAMP.get(), StampItem.designOf(pat));
        return out;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> rem = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < rem.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.getItem() instanceof BannerPatternItem) {
                rem.set(i, s.copyWithCount(1));
            } else if (s.hasCraftingRemainingItem()) {
                rem.set(i, s.getCraftingRemainingItem());
            }
        }
        return rem;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.STAMP_RECIPE.get();
    }
}
