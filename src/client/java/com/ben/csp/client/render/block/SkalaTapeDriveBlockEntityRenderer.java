package com.ben.csp.client.render.block;

import com.ben.csp.skala.SkalaTapeDriveBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class SkalaTapeDriveBlockEntityRenderer implements BlockEntityRenderer<SkalaTapeDriveBlockEntity> {

    public SkalaTapeDriveBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(SkalaTapeDriveBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // Rendering logic to follow.
    }
}
