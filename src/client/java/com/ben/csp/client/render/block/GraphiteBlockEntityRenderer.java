package com.ben.csp.client.render.block;

import com.ben.csp.block.entity.GraphiteBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class GraphiteBlockEntityRenderer implements BlockEntityRenderer<GraphiteBlockEntity> {

    public GraphiteBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(GraphiteBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // Rendering logic to follow.
    }
}
