package com.ben.csp.client.render.block;

import com.ben.csp.block.entity.PlantMeterBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Draws the ball flow meter: a vertical glass tube with a float whose height is
 * the channel flow reading, green when the channel is flowing and red when it is
 * starving — the one reading the RBMK operator watched channel by channel.
 */
public class PlantMeterBlockRenderer implements BlockEntityRenderer<PlantMeterBlockEntity> {

    public PlantMeterBlockRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(PlantMeterBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // glass tube
        InstrumentRenderHelper.box(matrices, vertexConsumers, light,
            0.30f, 0.10f, 0.30f, 0.70f, 0.92f, 0.70f,
            0.75f, 0.85f, 0.90f, 0.55f);

        // float: height follows the reading; colour follows the state
        float pos = entity.live() ? (float) entity.reading() : 0f;
        float fy = 0.12f + pos * 0.72f;
        boolean starve = entity.live() && entity.starving();
        boolean dead = !entity.live();
        float r = dead ? 0.35f : (starve ? 0.90f : 0.20f);
        float g = dead ? 0.35f : (starve ? 0.15f : 0.85f);
        InstrumentRenderHelper.box(matrices, vertexConsumers, light,
            0.34f, fy, 0.34f, 0.66f, fy + 0.06f, 0.66f,
            r, g, 0.20f, 1f);
    }
}
