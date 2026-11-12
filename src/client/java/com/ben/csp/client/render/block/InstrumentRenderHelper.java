package com.ben.csp.client.render.block;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Draws a tinted cuboid for the plant instruments. A one-pixel white texture is
 * uploaded once and every face is tinted with the instrument's colour, so the
 * valve and the meter need no art assets — the shape and colour carry the state.
 */
final class InstrumentRenderHelper {

    private static final Identifier WHITE = new Identifier("csp-modern", "instrument_white");
    private static boolean whiteReady;

    private InstrumentRenderHelper() {}

    private static void ensureWhite() {
        if (whiteReady) {
            return;
        }
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, 1, 1, false);
        img.setColor(0, 0, 0xFFFFFFFF); // ABGR white
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
        MinecraftClient.getInstance().getTextureManager().registerTexture(WHITE, tex);
        whiteReady = true;
    }

    /**
     * Draw an axis-aligned cuboid in block-local coordinates (0..1), tinted
     * {@code (r,g,b,a)} in 0..1, lit by {@code light}.
     */
    static void box(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                    float x0, float y0, float z0, float x1, float y1, float z1,
                    float r, float g, float b, float a) {
        ensureWhite();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(WHITE));

        // +Y top
        face(vc, m, light, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a, 0, 1, 0);
        // -Y bottom
        face(vc, m, light, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, r, g, b, a, 0, -1, 0);
        // +Z
        face(vc, m, light, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a, 0, 0, 1);
        // -Z
        face(vc, m, light, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r, g, b, a, 0, 0, -1);
        // +X
        face(vc, m, light, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r, g, b, a, 1, 0, 0);
        // -X
        face(vc, m, light, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a, -1, 0, 0);
    }

    private static void face(VertexConsumer vc, Matrix4f m, int light,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float r, float g, float b, float a, float nx, float ny, float nz) {
        put(vc, m, light, ax, ay, az, r, g, b, a, nx, ny, nz);
        put(vc, m, light, bx, by, bz, r, g, b, a, nx, ny, nz);
        put(vc, m, light, cx, cy, cz, r, g, b, a, nx, ny, nz);
        put(vc, m, light, dx, dy, dz, r, g, b, a, nx, ny, nz);
    }

    private static void put(VertexConsumer vc, Matrix4f m, int light,
                            float x, float y, float z, float r, float g, float b, float a,
                            float nx, float ny, float nz) {
        vc.vertex(m, x, y, z).color(r, g, b, a).texture(0.5f, 0.5f)
            .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV).light(light)
            .normal(nx, ny, nz).next();
    }
}
