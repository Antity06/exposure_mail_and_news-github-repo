package com.coolnydude.exposureletters.entity;

import com.coolnydude.exposureletters.registry.ModRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Glides with low gravity and strong air drag. Deals no damage. Lands like an arrow and can be picked up again.
 */
public class PaperPlane extends AbstractArrow {
    private static final double AIR_DRAG = 0.975;

    public PaperPlane(EntityType<? extends PaperPlane> type, Level level) {
        super(type, level);
        this.setBaseDamage(0.0);
    }

    public PaperPlane(Level level, LivingEntity owner, ItemStack stack) {
        super(ModRegistry.PAPER_PLANE_ENTITY.get(), owner, level, stack, null);
        this.setBaseDamage(0.0);
        this.pickup = Pickup.ALLOWED;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.inGround && !this.isInWater()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(AIR_DRAG));
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.012;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // Bonk. No damage: just bounce off and fall.
        this.setDeltaMovement(this.getDeltaMovement().scale(-0.1));
        this.playSound(SoundEvents.BOOK_PAGE_TURN, 0.6F, 1.5F);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModRegistry.PAPER_PLANE.get());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.BOOK_PUT;
    }
}
