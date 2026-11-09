package com.ben.csp.client.render.block;

import com.ben.csp.block.entity.GenplanBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Paints the генплан's drawing onto its block faces. The drawing is generated at
 * runtime from the plan (not a fixed texture) and uploaded once per panel; each
 * block shows the part that falls on its cell, so a wall built from many blocks
 * shows one continuous plan.
 */
public class GenplanBlockEntityRenderer implements BlockEntityRenderer<GenplanBlockEntity> {

    private static final Identifier TEX = new Identifier("csp-modern", "genplan_panel");

    private static int[] uploaded;
    private static NativeImageBackedTexture texture;
    private static int texW;
    private static int texH;

    public GenplanBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(GenplanBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!entity.hasRaster()) {
            return;
        }
        ensureTexture(entity.raster(), entity.rasterW(), entity.rasterH());
        if (texture == null) {
            return;
        }

        Direction facing = entity.getCachedState().get(net.minecraft.state.property.Properties.HORIZONTAL_FACING);

        matrices.push();
        // move to the face and orient so the drawing faces outward
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
        matrices.translate(0, 0, 0.501);

        // this block shows only its own cell of the panel's drawing
        int cols = Math.max(1, entity.cols());
        int rows = Math.max(1, entity.rows());
        float u0 = entity.col() / (float) cols;
        float u1 = (entity.col() + 1) / (float) cols;
        // row 0 is the top of the panel; texture v runs top(0) to bottom(1)
        float v0 = entity.row() / (float) rows;
        float v1 = (entity.row() + 1) / (float) rows;

        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEX));
        float s = 0.5f; // half-extent of the face, in blocks
        vc.vertex(m, -s, -s, 0).color(255, 255, 255, 255).texture(u0, v1)
            .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(0, 0, 1).next();
        vc.vertex(m, s, -s, 0).color(255, 255, 255, 255).texture(u1, v1)
            .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(0, 0, 1).next();
        vc.vertex(m, s, s, 0).color(255, 255, 255, 255).texture(u1, v0)
            .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(0, 0, 1).next();
        vc.vertex(m, -s, s, 0).color(255, 255, 255, 255).texture(u0, v0)
            .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(0, 0, 1).next();

        matrices.pop();
    }

    /** Pack a colour into the ABGR int NativeImage expects. */
    private static int abgr(int r, int g, int b, int a) {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /** Upload the drawing as a texture, once per distinct raster. */
    private static void ensureTexture(int[] raster, int w, int h) {
        if (uploaded != null && java.util.Arrays.equals(uploaded, raster)) {
            return;
        }
        if (texture != null) {
            texture.close();
        }
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = raster[y * w + x];
                int packed;
                if ((argb >>> 24) == 0) {
                    // paper: a pale plan sheet where nothing is drawn
                    packed = abgr(0xF4, 0xEF, 0xE2, 0xFF);
                } else {
                    packed = abgr((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF,
                                  (argb >>> 24) & 0xFF);
                }
                img.setColor(x, y, packed);
            }
        }
        texture = new NativeImageBackedTexture(img);
        MinecraftClient.getInstance().getTextureManager().registerTexture(TEX, texture);
        uploaded = raster.clone();
        texW = w;
        texH = h;
    }
}
