package com.ben.csp.client.render.entity;

import com.ben.csp.client.render.model.entity.BargeEntityModel;
import com.ben.csp.client.render.model.ModModelLayers;
import com.ben.csp.entity.BargeEntity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class BargeEntityRenderer extends MobEntityRenderer<BargeEntity, BargeEntityModel<BargeEntity>> {
    public BargeEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new BargeEntityModel<>(context.getPart(ModModelLayers.BARGE)), 2.0f);
    }

    @Override
    public Identifier getTexture(BargeEntity entity) {
        return new Identifier("minecraft", "textures/block/oak_planks.png");
    }
}