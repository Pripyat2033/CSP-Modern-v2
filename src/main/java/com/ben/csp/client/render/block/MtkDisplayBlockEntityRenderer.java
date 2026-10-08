package com.ben.csp.client.render.block;

import com.ben.csp.block.entity.MtkDisplayBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class MtkDisplayBlockEntityRenderer implements BlockEntityRenderer<MtkDisplayBlockEntity> {

    public MtkDisplayBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(MtkDisplayBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // Rendering logic for the MTK display will go here.
    }
}