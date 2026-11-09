package com.ben.csp.client.render.block;

import com.ben.csp.block.entity.VertushkaBlockEntity;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class VertushkaBlockEntityRenderer implements BlockEntityRenderer<VertushkaBlockEntity> {
    public VertushkaBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(VertushkaBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // Placeholder for rendering logic
    }
}