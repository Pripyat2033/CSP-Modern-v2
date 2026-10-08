package com.ben.csp.client.render.entity;

import com.ben.csp.client.render.model.ModModelLayers;
import com.ben.csp.client.render.model.entity.StroitelEntityModel;
import com.ben.csp.entity.StroitelEntity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class StroitelEntityRenderer extends MobEntityRenderer<StroitelEntity, StroitelEntityModel<StroitelEntity>> {
    public StroitelEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new StroitelEntityModel<>(context.getPart(ModModelLayers.STROITEL)), 0.5f);
    }

    @Override
    public Identifier getTexture(StroitelEntity entity) {
        return new Identifier("minecraft", "textures/entity/villager/villager.png");
    }
}