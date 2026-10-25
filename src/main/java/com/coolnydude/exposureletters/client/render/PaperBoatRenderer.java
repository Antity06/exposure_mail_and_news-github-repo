package com.coolnydude.exposureletters.client.render;

import com.coolnydude.exposureletters.ExposureLetters;
import com.coolnydude.exposureletters.block.PaperBoatBlock;
import com.coolnydude.exposureletters.block.PaperBoatBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the placed Paper Boat on water as a smooth connected-polygon 3D origami paper boat
 * matching the inventory item icon.
 */
public class PaperBoatRenderer implements BlockEntityRenderer<PaperBoatBlockEntity> {
    public static final ResourceLocation TEXTURE = ExposureLetters.id("textures/entity/paper_boat.png");

    public PaperBoatRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(PaperBoatBlockEntity boat, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = boat.getBlockState();
        Direction facing = state.getBlock() instanceof PaperBoatBlock
                ? state.getValue(PaperBoatBlock.FACING)
                : Direction.NORTH;

        pose.pushPose();
        pose.translate(0.5F, 0.012F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose last = pose.last();

        // Connected-polygon 3D origami paper boat vertices:
        // Boat length runs along Z (-Z = front bow, +Z = rear stern), width along X, height along Y.
        float yBot = 0.0F;
        float yRim = 0.165F;
        float yTip = 0.215F;
        float yFloor = 0.055F;
        float yPeak = 0.375F;

        float wBot = 0.105F;
        float wRim = 0.195F;
        float zKeel = 0.22F;
        float zProwBot = 0.30F;
        float zMid = 0.18F;
        float zTip = 0.41F;

        float wSail = 0.125F;
        float zSail = 0.165F;

        int colBright = 0xFFFCFAF4;
        int colLight  = 0xFFF2EDE1;
        int colWarm   = 0xFFE0D9C8;
        int colMid    = 0xFFCAC1AE;
        int colShade  = 0xFFB2A894;
        int colInner  = 0xFF9E9482;

        // 1. Outer Left Hull (-X):
        // Front-left bow triangle/quad: BOW(0, yTip, -zTip) -> FL(-wRim, yRim, -zMid) -> BL(-wBot, yBot, -zKeel) -> PROW_BOT(0, yBot, -zProwBot)
        quad(vc, last, light, overlay, colBright, -0.82F, 0.35F, -0.45F,
                 0.0F,  yTip, -zTip,     0.0F, 0.0F,
                -wRim,  yRim, -zMid,     0.5F, 0.2F,
                -wBot,  yBot, -zKeel,    0.5F, 0.9F,
                 0.0F,  yBot, -zProwBot, 0.0F, 0.9F);
        // Mid-left hull wall: FL(-wRim, yRim, -zMid) -> RL(-wRim, yRim, +zMid) -> RBL(-wBot, yBot, +zKeel) -> BL(-wBot, yBot, -zKeel)
        quad(vc, last, light, overlay, colLight, -0.90F, 0.42F, 0.0F,
                -wRim, yRim, -zMid,  0.2F, 0.2F,
                -wRim, yRim,  zMid,  0.8F, 0.2F,
                -wBot, yBot,  zKeel, 0.8F, 0.9F,
                -wBot, yBot, -zKeel, 0.2F, 0.9F);
        // Rear-left stern quad: RL(-wRim, yRim, +zMid) -> STERN(0, yTip, +zTip) -> STERN_BOT(0, yBot, +zProwBot) -> RBL(-wBot, yBot, +zKeel)
        quad(vc, last, light, overlay, colWarm, -0.82F, 0.35F, 0.45F,
                -wRim, yRim,  zMid,     0.5F, 0.2F,
                 0.0F, yTip,  zTip,     1.0F, 0.0F,
                 0.0F, yBot,  zProwBot, 1.0F, 0.9F,
                -wBot, yBot,  zKeel,    0.5F, 0.9F);

        // 2. Outer Right Hull (+X):
        // Front-right bow quad: FR(+wRim, yRim, -zMid) -> BOW(0, yTip, -zTip) -> PROW_BOT(0, yBot, -zProwBot) -> BR(+wBot, yBot, -zKeel)
        quad(vc, last, light, overlay, colLight, 0.82F, 0.35F, -0.45F,
                 wRim, yRim, -zMid,     0.5F, 0.2F,
                 0.0F, yTip, -zTip,     0.0F, 0.0F,
                 0.0F, yBot, -zProwBot, 0.0F, 0.9F,
                 wBot, yBot, -zKeel,    0.5F, 0.9F);
        // Mid-right hull wall: RR(+wRim, yRim, +zMid) -> FR(+wRim, yRim, -zMid) -> BR(+wBot, yBot, -zKeel) -> RBR(+wBot, yBot, +zKeel)
        quad(vc, last, light, overlay, colWarm, 0.90F, 0.42F, 0.0F,
                 wRim, yRim,  zMid,  0.8F, 0.2F,
                 wRim, yRim, -zMid,  0.2F, 0.2F,
                 wBot, yBot, -zKeel, 0.2F, 0.9F,
                 wBot, yBot,  zKeel, 0.8F, 0.9F);
        // Rear-right stern quad: STERN(0, yTip, +zTip) -> RR(+wRim, yRim, +zMid) -> RBR(+wBot, yBot, +zKeel) -> STERN_BOT(0, yBot, +zProwBot)
        quad(vc, last, light, overlay, colMid, 0.82F, 0.35F, 0.45F,
                 0.0F, yTip,  zTip,     1.0F, 0.0F,
                 wRim, yRim,  zMid,     0.5F, 0.2F,
                 wBot, yBot,  zKeel,    0.5F, 0.9F,
                 0.0F, yBot,  zProwBot, 1.0F, 0.9F);

        // 3. Flat Bottom Hull Base (closing the underside on the water):
        quad(vc, last, light, overlay, colShade, 0.0F, -1.0F, 0.0F,
                -wBot, yBot,  zKeel, 0.2F, 0.8F,
                -wBot, yBot, -zKeel, 0.2F, 0.2F,
                 wBot, yBot, -zKeel, 0.8F, 0.2F,
                 wBot, yBot,  zKeel, 0.8F, 0.8F);
        tri(vc, last, light, overlay, colShade, 0.0F, -1.0F, 0.0F,
                 0.0F, yBot, -zProwBot, 0.5F, 0.0F,
                 wBot, yBot, -zKeel,    0.8F, 0.2F,
                -wBot, yBot, -zKeel,    0.2F, 0.2F);
        tri(vc, last, light, overlay, colShade, 0.0F, -1.0F, 0.0F,
                 0.0F, yBot,  zProwBot, 0.5F, 1.0F,
                -wBot, yBot,  zKeel,    0.2F, 0.8F,
                 wBot, yBot,  zKeel,    0.8F, 0.8F);

        // 4. Folded Fore & Aft Triangular Deck Flaps (covering the pointed bow and stern tips):
        tri(vc, last, light, overlay, colBright, 0.0F, 0.96F, 0.25F,
                 0.0F, yTip, -zTip, 0.5F, 0.0F,
                -wRim, yRim, -zMid, 0.1F, 0.4F,
                 wRim, yRim, -zMid, 0.9F, 0.4F);
        tri(vc, last, light, overlay, colLight, 0.0F, 0.96F, -0.25F,
                 0.0F, yTip,  zTip, 0.5F, 1.0F,
                 wRim, yRim,  zMid, 0.9F, 0.6F,
                -wRim, yRim,  zMid, 0.1F, 0.6F);

        // 5. Inner Pocket Floor & Sloped Inner Hull Walls (visible inside the boat around the central sail):
        quad(vc, last, light, overlay, colInner, 0.0F, 1.0F, 0.0F,
                -wRim, yRim,   -zMid,  0.1F, 0.2F,
                -wBot, yFloor, -zMid,  0.3F, 0.4F,
                -wBot, yFloor,  zMid,  0.3F, 0.6F,
                -wRim, yRim,    zMid,  0.1F, 0.8F);
        quad(vc, last, light, overlay, colInner, 0.0F, 1.0F, 0.0F,
                 wRim, yRim,    zMid,  0.9F, 0.8F,
                 wBot, yFloor,  zMid,  0.7F, 0.6F,
                 wBot, yFloor, -zMid,  0.7F, 0.4F,
                 wRim, yRim,   -zMid,  0.9F, 0.2F);
        quad(vc, last, light, overlay, colShade, 0.0F, 1.0F, 0.0F,
                -wBot, yFloor, -zMid, 0.3F, 0.3F,
                -wBot, yFloor,  zMid, 0.3F, 0.7F,
                 wBot, yFloor,  zMid, 0.7F, 0.7F,
                 wBot, yFloor, -zMid, 0.7F, 0.3F);

        // 6. Central Connected-Polygon 4-Sided Origami Sail Pyramid rising to PEAK(0, yPeak, 0):
        // Front-left sail face
        tri(vc, last, light, overlay, colBright, -0.65F, 0.45F, -0.60F,
                 0.0F,  yPeak,  0.0F,   0.5F, 0.0F,
                 0.0F,  yFloor, -zSail, 0.5F, 0.8F,
                -wSail, yFloor,  0.0F,  0.1F, 0.8F);
        // Front-right sail face
        tri(vc, last, light, overlay, colLight, 0.65F, 0.45F, -0.60F,
                 0.0F,  yPeak,  0.0F,   0.5F, 0.0F,
                 wSail, yFloor,  0.0F,  0.9F, 0.8F,
                 0.0F,  yFloor, -zSail, 0.5F, 0.8F);
        // Rear-left sail face
        tri(vc, last, light, overlay, colWarm, -0.65F, 0.45F, 0.60F,
                 0.0F,  yPeak,  0.0F,  0.5F, 0.0F,
                -wSail, yFloor, 0.0F,  0.1F, 0.8F,
                 0.0F,  yFloor, zSail, 0.5F, 0.8F);
        // Rear-right sail face
        tri(vc, last, light, overlay, colMid, 0.65F, 0.45F, 0.60F,
                 0.0F,  yPeak,  0.0F,  0.5F, 0.0F,
                 0.0F,  yFloor, zSail, 0.5F, 0.8F,
                 wSail, yFloor, 0.0F,  0.9F, 0.8F);

        pose.popPose();
    }

    private static void tri(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, int color,
                            float nx, float ny, float nz,
                            float x0, float y0, float z0, float u0, float v0,
                            float x1, float y1, float z1, float u1, float v1,
                            float x2, float y2, float z2, float u2, float v2) {
        quad(vc, pose, light, overlay, color, nx, ny, nz,
                x0, y0, z0, u0, v0,
                x1, y1, z1, u1, v1,
                x2, y2, z2, u2, v2,
                x2, y2, z2, u2, v2);
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
