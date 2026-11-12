package com.ben.csp.client.render.block;

import com.ben.csp.block.entity.PlantValveBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Draws the valve's state: a body across the block and a handwheel whose height
 * follows the valve position, from shut (wheel down) to open (wheel up). With no
 * plant running the valve reads shut, so a dead plant shows a closed valve.
 */
public class PlantValveBlockRenderer implements BlockEntityRenderer<PlantValveBlockEntity> {

    public PlantValveBlockRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(PlantValveBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        boolean open = entity.live() && entity.open();
        float pos = entity.live() ? (float) entity.reading() : 0f;

        // valve body, along x
        InstrumentRenderHelper.box(matrices, vertexConsumers, light,
            0.10f, 0.30f, 0.35f, 0.90f, 0.70f, 0.65f,
            0.72f, 0.55f, 0.25f, 1f);

        // stem + handwheel: the wheel rises as the valve opens
        float wheelY = 0.72f + pos * 0.20f;
        InstrumentRenderHelper.box(matrices, vertexConsumers, light,
            0.46f, 0.70f, 0.46f, 0.54f, wheelY, 0.54f,
            0.55f, 0.55f, 0.58f, 1f);
        float wr = 0.16f;
        float c = 0.5f;
        float gc = open ? 0.30f : 0.55f;
        InstrumentRenderHelper.box(matrices, vertexConsumers, light,
            c - wr, wheelY, c - wr, c + wr, wheelY + 0.06f, c + wr,
            gc, open ? 0.85f : 0.30f, 0.30f, 1f);
    }
}
