package com.ben.csp.client.render.entity;

import com.ben.csp.CSPModClient;
import com.ben.csp.client.render.model.entity.MasterEntityModel;
import com.ben.csp.entity.MasterEntity;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class MasterEntityRenderer extends BipedEntityRenderer<MasterEntity, MasterEntityModel<MasterEntity>> {
    public MasterEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new MasterEntityModel<>(context.getPart(CSPModClient.MODEL_MASTER_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(MasterEntity entity) {
        return new Identifier("minecraft", "textures/entity/villager/villager.png");
    }
}