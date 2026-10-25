package com.coolnydude.exposureletters.item;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Purely cosmetic hat folded from newspapers. Gets soggy in the rain. */
public class PaperHatItem extends ArmorItem {
    public PaperHatItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living) || level.getGameTime() % 100 != 0)
            return;
        if (living.getItemBySlot(EquipmentSlot.HEAD) != stack)
            return;
        if (level.isRainingAt(living.blockPosition().above()) || living.isUnderWater())
            stack.hurtAndBreak(1, living, EquipmentSlot.HEAD);
    }
}
