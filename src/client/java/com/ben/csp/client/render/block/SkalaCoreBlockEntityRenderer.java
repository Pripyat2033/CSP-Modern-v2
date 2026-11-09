package com.ben.csp.client.render.block;

import com.ben.csp.skala.SkalaCoreBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class SkalaCoreBlockEntityRenderer implements BlockEntityRenderer<SkalaCoreBlockEntity> {

    public SkalaCoreBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(SkalaCoreBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // Rendering logic to follow.
    }
}
