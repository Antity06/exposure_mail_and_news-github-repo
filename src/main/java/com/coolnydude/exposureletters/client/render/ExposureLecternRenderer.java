package com.coolnydude.exposureletters.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

public class ExposureLecternRenderer extends LecternRenderer {
    public ExposureLecternRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LecternBlockEntity lectern, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (LecternItemRenderer.render(lectern, pose, buffer, light, overlay)) {
            return;
        }
        super.render(lectern, partialTick, pose, buffer, light, overlay);
    }
}
