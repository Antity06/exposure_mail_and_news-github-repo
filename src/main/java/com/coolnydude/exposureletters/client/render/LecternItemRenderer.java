package com.coolnydude.exposureletters.client.render;

import com.coolnydude.exposureletters.ExposureLetters;
import com.coolnydude.exposureletters.item.NoteItem;
import com.coolnydude.exposureletters.network.Payloads;
import com.coolnydude.exposureletters.registry.ModRegistry;
import com.coolnydude.exposureletters.util.LecternBookAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders custom 3D voxel models for a Note (layered 3D note pad with tilted top sheet, curled lower flap,
 * dog-eared corner, and wooden/brass top clip) and a Newspaper (multi-layered fanned 3D newsprint stack
 * with thick broadsheet wings, raised center fold ridge, and folded corner) on a Lectern.
 */
public final class LecternItemRenderer {
    private static final ResourceLocation NOTE_WRITTEN = ExposureLetters.id("textures/entity/lectern_note.png");
    private static final ResourceLocation NOTE_BLANK = ExposureLetters.id("textures/entity/lectern_note_blank.png");
    private static final ResourceLocation NEWSPAPER = ExposureLetters.id("textures/entity/lectern_newspaper.png");

    private static final Map<BlockPos, ItemStack> CLIENT_BOOKS = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> LAST_SYNC_REQUEST = new ConcurrentHashMap<>();

    public static void setClientBook(BlockPos pos, ItemStack stack) {
        if (pos == null) return;
        BlockPos key = pos.immutable();
        if (stack == null || stack.isEmpty()) {
            CLIENT_BOOKS.remove(key);
        } else {
            CLIENT_BOOKS.put(key, stack.copyWithCount(1));
        }
    }

    public static boolean isExposureLecternItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(ModRegistry.NOTE.get())
                || stack.is(ModRegistry.PRINTED_NEWSPAPER.get())
                || stack.is(ModRegistry.NEWSPAPER.get())
                || stack.is(ModRegistry.NEWSPAPER_DRAFT.get());
    }

    public static boolean isNewspaperItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(ModRegistry.PRINTED_NEWSPAPER.get())
                || stack.is(ModRegistry.NEWSPAPER.get())
                || stack.is(ModRegistry.NEWSPAPER_DRAFT.get());
    }

    public static boolean render(LecternBlockEntity lectern, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = lectern.getBlockState();
        if (!state.getValue(LecternBlock.HAS_BOOK)) {
            return false;
        }
        BlockPos pos = lectern.getBlockPos();

        ItemStack book = ItemStack.EMPTY;
        if (lectern instanceof LecternBookAccess access) {
            ItemStack synced = access.exposureLetters$getRenderBook();
            if (synced != null && !synced.isEmpty()) {
                book = synced;
            }
        }
        if (book.isEmpty()) {
            ItemStack direct = lectern.getBook();
            if (direct != null && !direct.isEmpty()) {
                book = direct;
            }
        }
        if (book.isEmpty() && pos != null) {
            ItemStack cached = CLIENT_BOOKS.get(pos);
            if (cached != null && !cached.isEmpty()) {
                book = cached;
            }
        }
        if (book.isEmpty() && pos != null) {
            Minecraft mc = Minecraft.getInstance();
            IntegratedServer server = mc != null ? mc.getSingleplayerServer() : null;
            if (server != null && lectern.getLevel() != null) {
                ServerLevel sLevel = server.getLevel(lectern.getLevel().dimension());
                if (sLevel != null && sLevel.getBlockEntity(pos) instanceof LecternBlockEntity sLectern) {
                    ItemStack sBook = sLectern.getBook();
                    if ((sBook == null || sBook.isEmpty()) && sLectern instanceof LecternBookAccess sAccess) {
                        sBook = sAccess.exposureLetters$getRenderBook();
                    }
                    if (sBook != null && !sBook.isEmpty()) {
                        book = sBook.copyWithCount(1);
                        CLIENT_BOOKS.put(pos.immutable(), book);
                        if (lectern instanceof LecternBookAccess cAccess) {
                            cAccess.exposureLetters$setClientBook(book);
                        }
                    }
                }
            }
        }
        if (book.isEmpty() && pos != null) {
            Level level = lectern.getLevel();
            long now = level != null ? level.getGameTime() : System.currentTimeMillis() / 50L;
            Long lastReq = LAST_SYNC_REQUEST.get(pos);
            if (lastReq == null || Math.abs(now - lastReq) >= 10L) {
                LAST_SYNC_REQUEST.put(pos.immutable(), now);
                PacketDistributor.sendToServer(new Payloads.RequestLecternSync(pos));
            }
            return false;
        }

        boolean isNote = book.is(ModRegistry.NOTE.get());
        boolean isNewspaper = isNewspaperItem(book);
        if (!isNote && !isNewspaper) return false;

        if (pos != null) {
            CLIENT_BOOKS.put(pos.immutable(), book.copyWithCount(1));
        }

        Direction facing = state.getValue(LecternBlock.FACING);
        pose.pushPose();
        pose.translate(0.5F, 1.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(0.0F, 0.0F, 0.105F);
        pose.mulPose(Axis.XP.rotationDegrees(-67.5F));

        if (isNote) {
            boolean written = !NoteItem.getContent(book).isEmpty();
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(written ? NOTE_WRITTEN : NOTE_BLANK));
            renderNote3D(pose, vc, light, overlay);
        } else {
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(NEWSPAPER));
            renderNewspaper3D(pose, vc, light, overlay);
        }

        pose.popPose();
        return true;
    }

    private static void renderNote3D(PoseStack pose, VertexConsumer vc, int light, int overlay) {
        float w = 0.23F;
        float yTop = 0.29F;
        float yMid = -0.13F;
        float yBot = -0.29F;

        // 1. Backing 3D parchment/note layer slightly skewed (+3 deg) for true 3D depth on the lectern
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(3.0F));
        box(vc, pose.last(), light, overlay,
                0xFFEAE2D0, 0xFFD2C8B4, 0xFFB8AE9A, 0xFF9E9482,
                -w + 0.01F, yBot + 0.015F, 0.005F,
                 w + 0.015F, yTop - 0.01F, 0.032F,
                0.0F, 0.0F, 1.0F, 1.0F,
                0.0F, 0.0F, 0.06F, 0.06F);
        pose.popPose();

        // 2. Main tilted 3D voxel note sheet (-4.5 deg, 0.042F voxel thickness)
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(-4.5F));
        PoseStack.Pose last = pose.last();

        float zBase = 0.033F;
        float thick = 0.042F;

        // Upper 3D slab of the note
        box(vc, last, light, overlay,
                0xFFFFFFFF, 0xFFDCD3C0, 0xFFC2B8A4, 0xFFA69C88,
                -w, yMid, zBase,
                 w, yTop, zBase + thick,
                0.0F, 0.0F, 1.0F, 0.72F,
                0.0F, 0.0F, 0.06F, 0.06F);

        // 3. Lifted 3D curled lower flap rising off the lectern bottom lip
        float zCurlLeft = zBase + 0.042F;
        float zCurlRight = zBase + 0.024F;
        // Top surface of curled lower flap
        quad(vc, last, light, overlay, 0xFFF7F2E6, 0.0F, -0.22F, 0.97F,
                -w, yMid, zBase + thick,       0.0F, 0.72F,
                -w, yBot, zCurlLeft + thick,   0.0F, 1.0F,
                 w, yBot, zCurlRight + thick,  1.0F, 1.0F,
                 w, yMid, zBase + thick,       1.0F, 0.72F);
        // Bottom surface of curled lower flap
        quad(vc, last, light, overlay, 0xFFA69C88, 0.0F, 0.22F, -0.97F,
                -w, yBot, zCurlLeft,   0.0F, 1.0F,
                -w, yMid, zBase,       0.0F, 0.72F,
                 w, yMid, zBase,       1.0F, 0.72F,
                 w, yBot, zCurlRight,  1.0F, 1.0F);
        // Left 3D wall of curled flap
        quad(vc, last, light, overlay, 0xFFC2B8A4, -1.0F, 0.0F, 0.0F,
                -w, yMid, zBase + thick,      0.0F, 0.72F,
                -w, yMid, zBase,              0.06F, 0.72F,
                -w, yBot, zCurlLeft,          0.06F, 1.0F,
                -w, yBot, zCurlLeft + thick,  0.0F, 1.0F);
        // Right 3D wall of curled flap
        quad(vc, last, light, overlay, 0xFFC2B8A4, 1.0F, 0.0F, 0.0F,
                 w, yBot, zCurlRight + thick, 0.94F, 1.0F,
                 w, yBot, zCurlRight,         1.0F, 1.0F,
                 w, yMid, zBase,              1.0F, 0.72F,
                 w, yMid, zBase + thick,      0.94F, 0.72F);
        // Bottom front 3D wall of curled flap
        quad(vc, last, light, overlay, 0xFFDCD3C0, 0.0F, -1.0F, 0.0F,
                -w, yBot, zCurlLeft + thick,  0.0F, 0.94F,
                -w, yBot, zCurlLeft,          0.0F, 1.0F,
                 w, yBot, zCurlRight,         1.0F, 1.0F,
                 w, yBot, zCurlRight + thick, 1.0F, 0.94F);

        // 5. 3D Wooden & Brass top clip pinning the note to the lectern
        box(vc, last, light, overlay,
                0xFFD8A858, 0xFFB8883C, 0xFF986C28, 0xFF78521A,
                -0.055F, yTop - 0.045F, zBase + thick - 0.004F,
                 0.055F, yTop + 0.020F, zBase + thick + 0.032F,
                0.0F, 0.0F, 0.16F, 0.06F,
                0.0F, 0.0F, 0.08F, 0.04F);

        pose.popPose();
    }

    private static void renderNewspaper3D(PoseStack pose, VertexConsumer vc, int light, int overlay) {
        float w = 0.43F;
        float h = 0.30F;

        // 1. Bottom skewed 3D folded newsprint stack (-4.5 deg)
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(-4.5F));
        box(vc, pose.last(), light, overlay,
                0xFFE4DDD0, 0xFFCCC4B4, 0xFFB4AC9C, 0xFF989080,
                -w, -h, 0.005F,
                 w,  h, 0.040F,
                0.0F, 0.0F, 1.0F, 1.0F,
                0.0F, 0.0F, 0.04F, 1.0F);
        pose.popPose();

        // 2. Middle skewed 3D folded newsprint stack (+3.0 deg)
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(3.0F));
        box(vc, pose.last(), light, overlay,
                0xFFEEE8DC, 0xFFD6CEC0, 0xFFBEB6A6, 0xFFA29A8A,
                -w, -h, 0.041F,
                 w,  h, 0.075F,
                0.0F, 0.0F, 1.0F, 1.0F,
                0.0F, 0.0F, 0.04F, 1.0F);
        pose.popPose();

        PoseStack.Pose last = pose.last();

        // 3. Upper 3D angled broadsheet spread (thick 3D voxel left & right wings rising from the center spine)
        float zSpine = 0.076F;
        float zOuter = 0.118F;
        float wingThick = 0.038F;

        // Left 3D broadsheet wing (-w .. 0)
        quad(vc, last, light, overlay, 0xFFFFFFFF, 0.09F, 0.0F, 0.995F,
                -w,    h, zOuter + wingThick, 0.0F, 0.0F,
                -w,   -h, zOuter + wingThick, 0.0F, 1.0F,
                 0.0F,-h, zSpine + wingThick, 0.5F, 1.0F,
                 0.0F, h, zSpine + wingThick, 0.5F, 0.0F);
        quad(vc, last, light, overlay, 0xFFA49C8C, -0.09F, 0.0F, -0.995F,
                -w,   -h, zOuter, 0.0F, 1.0F,
                -w,    h, zOuter, 0.0F, 0.0F,
                 0.0F, h, zSpine, 0.5F, 0.0F,
                 0.0F,-h, zSpine, 0.5F, 1.0F);
        quad(vc, last, light, overlay, 0xFFBEB6A6, -1.0F, 0.0F, 0.0F,
                -w,  h, zOuter + wingThick, 0.0F, 0.0F,
                -w,  h, zOuter,             0.03F, 0.0F,
                -w, -h, zOuter,             0.03F, 1.0F,
                -w, -h, zOuter + wingThick, 0.0F, 1.0F);
        quad(vc, last, light, overlay, 0.0F, 1.0F, 0.0F,
                -w,    h, zOuter,             0.0F, 0.03F,
                -w,    h, zOuter + wingThick, 0.0F, 0.0F,
                 0.0F, h, zSpine + wingThick, 0.5F, 0.0F,
                 0.0F, h, zSpine,             0.5F, 0.03F);
        quad(vc, last, light, overlay, 0xFFD8D0C2, 0.0F, -1.0F, 0.0F,
                -w,   -h, zOuter + wingThick, 0.0F, 0.97F,
                -w,   -h, zOuter,             0.0F, 1.0F,
                 0.0F,-h, zSpine,             0.5F, 1.0F,
                 0.0F,-h, zSpine + wingThick, 0.5F, 0.97F);

        // Right 3D broadsheet wing (0 .. +w)
        quad(vc, last, light, overlay, 0xFFF8F4EA, -0.09F, 0.0F, 0.995F,
                 0.0F, h, zSpine + wingThick, 0.5F, 0.0F,
                 0.0F,-h, zSpine + wingThick, 0.5F, 1.0F,
                 w,   -h, zOuter + wingThick, 1.0F, 1.0F,
                 w,    h, zOuter + wingThick, 1.0F, 0.0F);
        quad(vc, last, light, overlay, 0xFFA49C8C, 0.09F, 0.0F, -0.995F,
                 0.0F,-h, zSpine, 0.5F, 1.0F,
                 0.0F, h, zSpine, 0.5F, 0.0F,
                 w,    h, zOuter, 1.0F, 0.0F,
                 w,   -h, zOuter, 1.0F, 1.0F);
        quad(vc, last, light, overlay, 0xFFBEB6A6, 1.0F, 0.0F, 0.0F,
                 w, -h, zOuter + wingThick, 0.97F, 1.0F,
                 w, -h, zOuter,             1.0F, 1.0F,
                 w,  h, zOuter,             1.0F, 0.0F,
                 w,  h, zOuter + wingThick, 0.97F, 0.0F);
        quad(vc, last, light, overlay, 0xFFD8D0C2, 0.0F, 1.0F, 0.0F,
                 0.0F, h, zSpine,             0.5F, 0.03F,
                 0.0F, h, zSpine + wingThick, 0.5F, 0.0F,
                 w,    h, zOuter + wingThick, 1.0F, 0.0F,
                 w,    h, zOuter,             1.0F, 0.03F);
        quad(vc, last, light, overlay, 0xFFD8D0C2, 0.0F, -1.0F, 0.0F,
                 0.0F,-h, zSpine + wingThick, 0.5F, 0.97F,
                 0.0F,-h, zSpine,             0.5F, 1.0F,
                 w,   -h, zOuter,             1.0F, 1.0F,
                 w,   -h, zOuter + wingThick, 1.0F, 0.97F);

        // 4. Central 3D raised fold spine ridge
        box(vc, last, light, overlay,
                0xFFEAE3D4, 0xFFD0C8B8, 0xFFB8B0A0, 0xFF9C9484,
                -0.022F, -h - 0.005F, zSpine - 0.004F,
                 0.022F,  h + 0.005F, zSpine + wingThick + 0.012F,
                0.48F, 0.0F, 0.52F, 1.0F,
                0.48F, 0.0F, 0.52F, 0.04F);

    }

    private static void box(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay,
                            int colTop, int colNS, int colEW, int colBot,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float u0, float v0, float u1, float v1,
                            float eu0, float ev0, float eu1, float ev1) {
        // Top (+Z)
        quad(vc, pose, light, overlay, colTop, 0.0F, 0.0F, 1.0F,
                x0, y1, z1, u0, v0,
                x0, y0, z1, u0, v1,
                x1, y0, z1, u1, v1,
                x1, y1, z1, u1, v0);
        // Bottom (-Z)
        quad(vc, pose, light, overlay, colBot, 0.0F, 0.0F, -1.0F,
                x0, y0, z0, u0, v1,
                x0, y1, z0, u0, v0,
                x1, y1, z0, u1, v0,
                x1, y0, z0, u1, v1);
        // North (+Y)
        quad(vc, pose, light, overlay, colNS, 0.0F, 1.0F, 0.0F,
                x0, y1, z0, eu0, ev1,
                x0, y1, z1, eu0, ev0,
                x1, y1, z1, eu1, ev0,
                x1, y1, z0, eu1, ev1);
        // South (-Y)
        quad(vc, pose, light, overlay, colNS, 0.0F, -1.0F, 0.0F,
                x0, y0, z1, eu0, ev0,
                x0, y0, z0, eu0, ev1,
                x1, y0, z0, eu1, ev1,
                x1, y0, z1, eu1, ev0);
        // West (-X)
        quad(vc, pose, light, overlay, colEW, -1.0F, 0.0F, 0.0F,
                x0, y1, z1, eu0, ev0,
                x0, y1, z0, eu1, ev0,
                x0, y0, z0, eu1, ev1,
                x0, y0, z1, eu0, ev1);
        // East (+X)
        quad(vc, pose, light, overlay, colEW, 1.0F, 0.0F, 0.0F,
                x1, y0, z1, eu0, ev1,
                x1, y0, z0, eu1, ev1,
                x1, y1, z0, eu1, ev0,
                x1, y1, z1, eu0, ev0);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay,
                             float nx, float ny, float nz,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3) {
        quad(vc, pose, light, overlay, 0xFFD8D0C2, nx, ny, nz,
                x0, y0, z0, u0, v0, x1, y1, z1, u1, v1, x2, y2, z2, u2, v2, x3, y3, z3, u3, v3);
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

    private LecternItemRenderer() {}
}
