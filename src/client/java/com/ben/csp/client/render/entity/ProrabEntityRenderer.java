package com.ben.csp.client.render.entity;

import com.ben.csp.client.render.model.ModModelLayers;
import com.ben.csp.client.render.model.entity.ProrabEntityModel;
import com.ben.csp.entity.ProrabEntity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class ProrabEntityRenderer extends MobEntityRenderer<ProrabEntity, ProrabEntityModel<ProrabEntity>> {
    public ProrabEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new ProrabEntityModel<>(context.getPart(ModModelLayers.PRORAB)), 0.5f);
    }

    @Override
    public Identifier getTexture(ProrabEntity entity) {
        return new Identifier("minecraft", "textures/entity/villager/villager.png");
    }
}