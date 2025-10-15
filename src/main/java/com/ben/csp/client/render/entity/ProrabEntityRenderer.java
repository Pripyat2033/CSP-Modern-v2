package com.ben.csp.client.render.entity;

import com.ben.csp.CSPModClient;
import com.ben.csp.client.render.model.entity.ProrabEntityModel;
import com.ben.csp.entity.ProrabEntity;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class ProrabEntityRenderer extends BipedEntityRenderer<ProrabEntity, ProrabEntityModel<ProrabEntity>> {
    public ProrabEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new ProrabEntityModel<>(context.getPart(CSPModClient.MODEL_PRORAB_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(ProrabEntity entity) {
        return new Identifier("minecraft", "textures/entity/villager/villager.png");
    }
}