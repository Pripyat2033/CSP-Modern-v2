package com.ben.csp.client.render.block;

import com.ben.csp.block.entity.ArchiveFilingCabinetBlockEntity;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class ArchiveFilingCabinetBlockEntityRenderer implements BlockEntityRenderer<ArchiveFilingCabinetBlockEntity> {
    public ArchiveFilingCabinetBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(ArchiveFilingCabinetBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // Placeholder for rendering logic
    }
}