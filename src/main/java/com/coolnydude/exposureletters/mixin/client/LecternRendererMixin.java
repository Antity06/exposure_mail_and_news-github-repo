package com.coolnydude.exposureletters.mixin.client;

import com.coolnydude.exposureletters.client.render.ExposureLecternRenderer;
import com.coolnydude.exposureletters.client.render.LecternItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternRenderer.class)
public abstract class LecternRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void exposureLetters$render(LecternBlockEntity lectern, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        if (!((Object) this instanceof ExposureLecternRenderer)
                && LecternItemRenderer.render(lectern, pose, buffer, light, overlay)) {
            ci.cancel();
        }
    }
}
