package com.ben.csp.client.render.block;

import com.ben.csp.block.entity.PlanshetBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class PlanshetBlockEntityRenderer implements BlockEntityRenderer<PlanshetBlockEntity> {

    public PlanshetBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(PlanshetBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // Rendering logic to follow.
    }
}
