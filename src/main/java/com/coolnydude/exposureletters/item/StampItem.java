package com.coolnydude.exposureletters.item;

import com.coolnydude.exposureletters.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class StampItem extends Item {
    public static final List<String> DESIGNS = List.of(
            "generic", "flower", "creeper", "skull", "mojang", "globe", "piglin", "flow", "guster"
    );

    public StampItem(Item.Properties properties) {
        super(properties);
    }

    public static String designOf(BannerPatternItem pattern) {
        String id = BuiltInRegistries.ITEM.getKey(pattern).getPath().replace("_banner_pattern", "");
        return DESIGNS.contains(id) ? id : "generic";
    }

    public static String getDesign(ItemStack stack) {
        String id = stack.getOrDefault(ModRegistry.STAMP.get(), "generic");
        return DESIGNS.contains(id) ? id : "generic";
    }

    public static MutableComponent designName(String design) {
        return Component.translatable("stamp.exposure_letters.design." + design);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(designName(getDesign(stack)).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.exposure_letters.stamp.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
