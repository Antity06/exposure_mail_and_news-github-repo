package com.coolnydude.exposureletters.block;

import com.coolnydude.exposureletters.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PaperBoatBlockEntity extends BlockEntity {
    public PaperBoatBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.PAPER_BOAT_BE.get(), pos, state);
    }
}
