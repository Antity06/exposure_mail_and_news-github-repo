package com.coolnydude.exposureletters.client.render;

import com.coolnydude.exposureletters.ExposureLetters;
import com.coolnydude.exposureletters.entity.PaperPlane;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Folded plane made of three triangles (two wings and a keel), nose along +X like the arrow model. */
public class PaperPlaneRenderer extends EntityRenderer<PaperPlane> {
    public static final ResourceLocation TEXTURE = ExposureLetters.id("textures/entity/paper_plane.png");

    public PaperPlaneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PaperPlane plane, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, plane.yRotO, plane.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, plane.xRotO, plane.getXRot())));
        // small wobble while gliding
        if (!plane.isNoGravity() && plane.getDeltaMovement().lengthSqr() > 0.01)
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin((plane.tickCount + partialTick) * 0.4F) * 6.0F));

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose p = pose.last();
        float nx = 0.35F, tx = -0.25F;
        // left wing
        tri(vc, p, light, nx, 0, 0, 1, 0.5F, tx, 0.04F, 0.22F, 0, 0, tx, 0, 0, 0, 0.5F, 0, 1, 0);
        // right wing
        tri(vc, p, light, nx, 0, 0, 1, 0.5F, tx, 0, 0, 0, 0.5F, tx, 0.04F, -0.22F, 0, 1, 0, 1, 0);
        // keel
        tri(vc, p, light, nx, 0, 0, 1, 0.5F, tx, 0, 0, 0, 0.5F, tx, -0.08F, 0, 0, 0.75F, 0, 0, 1);
        pose.popPose();
        super.render(plane, yaw, partialTick, pose, buffer, light);
    }

    private static void tri(VertexConsumer vc, PoseStack.Pose p, int light,
                            float x1, float y1, float z1, float u1, float v1,
                            float x2, float y2, float z2, float u2, float v2,
                            float x3, float y3, float z3, float u3, float v3,
                            float nx, float ny, float nz) {
        vertex(vc, p, light, x1, y1, z1, u1, v1, nx, ny, nz);
        vertex(vc, p, light, x2, y2, z2, u2, v2, nx, ny, nz);
        vertex(vc, p, light, x3, y3, z3, u3, v3, nx, ny, nz);
        vertex(vc, p, light, x3, y3, z3, u3, v3, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, int light, float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(PaperPlane plane) {
        return TEXTURE;
    }
}
