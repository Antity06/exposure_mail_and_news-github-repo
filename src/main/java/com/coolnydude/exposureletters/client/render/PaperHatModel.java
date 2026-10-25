package com.coolnydude.exposureletters.client.render;

import com.coolnydude.exposureletters.ExposureLetters;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Seamless polygonal folded paper hat model that matches the inventory icon (item/paper_hat.png)
 * without separate blocky pieces.
 */
public class PaperHatModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ExposureLetters.id("paper_hat"), "main");

    public PaperHatModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        if (!this.head.visible) return;

        poseStack.pushPose();
        if (this.young) {
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.translate(0.0F, 1.0F, 0.0F);
        }
        this.head.translateAndRotate(poseStack);
        PoseStack.Pose last = poseStack.last();

        int colBright = shade(color, 1.00F);
        int colMid    = shade(color, 0.93F);
        int colSide   = shade(color, 0.86F);
        int colUnder  = shade(color, 0.76F);

        // Head-local coordinates (player head is x: -0.25..+0.25, y: -0.50..0.0 with -Y up, z: -0.25..+0.25 with -Z front)
        float yBrimBot  = -0.345F; // -5.52 / 16
        float yBrimTop  = -0.495F; // -7.92 / 16
        float yPeak     = -0.845F; // -13.52 / 16 (pointed triangular crown peak)

        // Brim outer perimeter (slightly wider than head + hat layer, with subtle folded crease at x = 0)
        float bxBot = 0.305F;
        float bzBot = 0.295F;
        float bzFrontMidBot = -0.312F;
        float bzBackMidBot  =  0.312F;

        float bxTop = 0.292F;
        float bzTop = 0.285F;
        float bzFrontMidTop = -0.302F;
        float bzBackMidTop  =  0.302F;

        // 1. Continuous polygonal folded brim band (UV v: 0.625 .. 1.0)
        // Front-Left brim
        quad(buffer, last, packedLight, packedOverlay, colBright, -0.10F, -0.05F, -0.99F,
                -bxTop, yBrimTop, -bzTop,        0.00F, 0.625F,
                -bxBot, yBrimBot, -bzBot,        0.00F, 1.000F,
                 0.00F, yBrimBot, bzFrontMidBot, 0.25F, 1.000F,
                 0.00F, yBrimTop, bzFrontMidTop, 0.25F, 0.625F);
        // Front-Right brim
        quad(buffer, last, packedLight, packedOverlay, colMid, 0.10F, -0.05F, -0.99F,
                 0.00F, yBrimTop, bzFrontMidTop, 0.25F, 0.625F,
                 0.00F, yBrimBot, bzFrontMidBot, 0.25F, 1.000F,
                 bxBot, yBrimBot, -bzBot,        0.50F, 1.000F,
                 bxTop, yBrimTop, -bzTop,        0.50F, 0.625F);

        // Back-Right brim
        quad(buffer, last, packedLight, packedOverlay, colMid, 0.10F, -0.05F, 0.99F,
                 bxTop, yBrimTop,  bzTop,       0.00F, 0.625F,
                 bxBot, yBrimBot,  bzBot,       0.00F, 1.000F,
                 0.00F, yBrimBot, bzBackMidBot, 0.25F, 1.000F,
                 0.00F, yBrimTop, bzBackMidTop, 0.25F, 0.625F);
        // Back-Left brim
        quad(buffer, last, packedLight, packedOverlay, colBright, -0.10F, -0.05F, 0.99F,
                 0.00F, yBrimTop, bzBackMidTop, 0.25F, 0.625F,
                 0.00F, yBrimBot, bzBackMidBot, 0.25F, 1.000F,
                -bxBot, yBrimBot,  bzBot,       0.50F, 1.000F,
                -bxTop, yBrimTop,  bzTop,       0.50F, 0.625F);

        // Left side brim (with pointed boat-fold tip at z = 0)
        float bxTipBot = 0.325F;
        float bxTipTop = 0.312F;
        quad(buffer, last, packedLight, packedOverlay, colSide, -0.99F, -0.05F, -0.10F,
                -bxTipTop, yBrimTop,  0.00F, 0.00F, 0.625F,
                -bxTipBot, yBrimBot,  0.00F, 0.00F, 1.000F,
                -bxBot,    yBrimBot, -bzBot, 0.25F, 1.000F,
                -bxTop,    yBrimTop, -bzTop, 0.25F, 0.625F);
        quad(buffer, last, packedLight, packedOverlay, colSide, -0.99F, -0.05F, 0.10F,
                -bxTop,    yBrimTop,  bzTop, 0.25F, 0.625F,
                -bxBot,    yBrimBot,  bzBot, 0.25F, 1.000F,
                -bxTipBot, yBrimBot,  0.00F, 0.50F, 1.000F,
                -bxTipTop, yBrimTop,  0.00F, 0.50F, 0.625F);

        // Right side brim (with pointed boat-fold tip at z = 0)
        quad(buffer, last, packedLight, packedOverlay, colSide, 0.99F, -0.05F, -0.10F,
                 bxTop,    yBrimTop, -bzTop, 0.00F, 0.625F,
                 bxBot,    yBrimBot, -bzBot, 0.00F, 1.000F,
                 bxTipBot, yBrimBot,  0.00F, 0.25F, 1.000F,
                 bxTipTop, yBrimTop,  0.00F, 0.25F, 0.625F);
        quad(buffer, last, packedLight, packedOverlay, colSide, 0.99F, -0.05F, 0.10F,
                 bxTipTop, yBrimTop,  0.00F, 0.25F, 0.625F,
                 bxTipBot, yBrimBot,  0.00F, 0.25F, 1.000F,
                 bxBot,    yBrimBot,  bzBot, 0.50F, 1.000F,
                 bxTop,    yBrimTop,  bzTop, 0.50F, 0.625F);

        // 2. Seamless polygonal triangular paper crown (UV v: 0.0 .. 0.625)
        // Base of crown meets the top of the brim band seamlessly along every edge,
        // tapering smoothly to the pointed triangular peak at (x = 0.0F, y = yPeak, z = -0.045F .. +0.045F)
        float zPeakFront = -0.045F;
        float zPeakBack  =  0.045F;

        // Front-Left triangular crown slope
        quad(buffer, last, packedLight, packedOverlay, colBright, -0.35F, -0.60F, -0.72F,
                 0.00F, yPeak,    zPeakFront,    0.25F, 0.000F,
                -bxTop, yBrimTop, -bzTop,        0.00F, 0.625F,
                 0.00F, yBrimTop, bzFrontMidTop, 0.25F, 0.625F,
                 0.00F, yPeak,    zPeakFront,    0.25F, 0.000F);

        // Front-Right triangular crown slope
        quad(buffer, last, packedLight, packedOverlay, colMid, 0.35F, -0.60F, -0.72F,
                 0.00F, yPeak,    zPeakFront,    0.25F, 0.000F,
                 0.00F, yBrimTop, bzFrontMidTop, 0.25F, 0.625F,
                 bxTop, yBrimTop, -bzTop,        0.50F, 0.625F,
                 0.00F, yPeak,    zPeakFront,    0.25F, 0.000F);

        // Back-Right triangular crown slope
        quad(buffer, last, packedLight, packedOverlay, colMid, 0.35F, -0.60F, 0.72F,
                 0.00F, yPeak,    zPeakBack,    0.25F, 0.000F,
                 bxTop, yBrimTop,  bzTop,       0.00F, 0.625F,
                 0.00F, yBrimTop, bzBackMidTop, 0.25F, 0.625F,
                 0.00F, yPeak,    zPeakBack,    0.25F, 0.000F);

        // Back-Left triangular crown slope
        quad(buffer, last, packedLight, packedOverlay, colBright, -0.35F, -0.60F, 0.72F,
                 0.00F, yPeak,    zPeakBack,    0.25F, 0.000F,
                 0.00F, yBrimTop, bzBackMidTop, 0.25F, 0.625F,
                -bxTop, yBrimTop,  bzTop,       0.50F, 0.625F,
                 0.00F, yPeak,    zPeakBack,    0.25F, 0.000F);

        // Left side crown facets (meeting the left boat-tip at z = 0)
        quad(buffer, last, packedLight, packedOverlay, colSide, -0.75F, -0.62F, -0.22F,
                 0.00F,     yPeak,    zPeakFront, 0.25F, 0.000F,
                -bxTipTop,  yBrimTop,  0.00F,     0.00F, 0.625F,
                -bxTop,     yBrimTop, -bzTop,     0.25F, 0.625F,
                 0.00F,     yPeak,    zPeakFront, 0.25F, 0.000F);
        quad(buffer, last, packedLight, packedOverlay, colSide, -0.78F, -0.62F, 0.00F,
                 0.00F,     yPeak,    zPeakBack,  0.25F, 0.000F,
                -bxTipTop,  yBrimTop,  0.00F,     0.25F, 0.625F,
                 0.00F,     yPeak,    zPeakFront, 0.25F, 0.000F,
                 0.00F,     yPeak,    zPeakBack,  0.25F, 0.000F);
        quad(buffer, last, packedLight, packedOverlay, colSide, -0.75F, -0.62F, 0.22F,
                 0.00F,     yPeak,    zPeakBack,  0.25F, 0.000F,
                -bxTop,     yBrimTop,  bzTop,     0.25F, 0.625F,
                -bxTipTop,  yBrimTop,  0.00F,     0.50F, 0.625F,
                 0.00F,     yPeak,    zPeakBack,  0.25F, 0.000F);

        // Right side crown facets (meeting the right boat-tip at z = 0)
        quad(buffer, last, packedLight, packedOverlay, colSide, 0.75F, -0.62F, -0.22F,
                 0.00F,     yPeak,    zPeakFront, 0.25F, 0.000F,
                 bxTop,     yBrimTop, -bzTop,     0.25F, 0.625F,
                 bxTipTop,  yBrimTop,  0.00F,     0.50F, 0.625F,
                 0.00F,     yPeak,    zPeakFront, 0.25F, 0.000F);
        quad(buffer, last, packedLight, packedOverlay, colSide, 0.78F, -0.62F, 0.00F,
                 0.00F,     yPeak,    zPeakFront, 0.25F, 0.000F,
                 bxTipTop,  yBrimTop,  0.00F,     0.25F, 0.625F,
                 0.00F,     yPeak,    zPeakBack,  0.25F, 0.000F,
                 0.00F,     yPeak,    zPeakFront, 0.25F, 0.000F);
        quad(buffer, last, packedLight, packedOverlay, colSide, 0.75F, -0.62F, 0.22F,
                 0.00F,     yPeak,    zPeakBack,  0.25F, 0.000F,
                 bxTipTop,  yBrimTop,  0.00F,     0.00F, 0.625F,
                 bxTop,     yBrimTop,  bzTop,     0.25F, 0.625F,
                 0.00F,     yPeak,    zPeakBack,  0.25F, 0.000F);

        // 3. Subtle underside rim of the brim so looking up from below shows folded paper thickness
        float ix = 0.245F;
        float iz = 0.245F;
        quad(buffer, last, packedLight, packedOverlay, colUnder, 0.0F, 1.0F, 0.0F,
                -bxBot, yBrimBot, -bzBot, 0.00F, 0.95F,
                -ix,    yBrimBot, -iz,    0.00F, 1.00F,
                 ix,    yBrimBot, -iz,    0.50F, 1.00F,
                 bxBot, yBrimBot, -bzBot, 0.50F, 0.95F);
        quad(buffer, last, packedLight, packedOverlay, colUnder, 0.0F, 1.0F, 0.0F,
                 bxBot, yBrimBot,  bzBot, 0.00F, 0.95F,
                 ix,    yBrimBot,  iz,    0.00F, 1.00F,
                -ix,    yBrimBot,  iz,    0.50F, 1.00F,
                -bxBot, yBrimBot,  bzBot, 0.50F, 0.95F);

        poseStack.popPose();
    }

    private static int shade(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        if (a == 0) a = 0xFF;
        int r = Math.min(255, Math.max(0, (int) (((argb >>> 16) & 0xFF) * factor)));
        int g = Math.min(255, Math.max(0, (int) (((argb >>> 8) & 0xFF) * factor)));
        int b = Math.min(255, Math.max(0, (int) ((argb & 0xFF) * factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, int color,
                             float nx, float ny, float nz,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3) {
        v(vc, pose, light, overlay, color, nx, ny, nz, x0, y0, z0, u0, v0);
        v(vc, pose, light, overlay, color, nx, ny, nz, x1, y1, z1, u1, v1);
        v(vc, pose, light, overlay, color, nx, ny, nz, x2, y2, z2, u2, v2);
        v(vc, pose, light, overlay, color, nx, ny, nz, x3, y3, z3, u3, v3);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, int color,
                          float nx, float ny, float nz,
                          float x, float y, float z, float u, float v) {
        vc.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
