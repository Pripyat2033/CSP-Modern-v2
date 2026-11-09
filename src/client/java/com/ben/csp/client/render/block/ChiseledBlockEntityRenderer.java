package com.ben.csp.client.render.block;

import com.ben.csp.block.entity.ChiseledBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class ChiseledBlockEntityRenderer implements BlockEntityRenderer<ChiseledBlockEntity> {

    public ChiseledBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(ChiseledBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // Rendering logic to follow.
    }
}
