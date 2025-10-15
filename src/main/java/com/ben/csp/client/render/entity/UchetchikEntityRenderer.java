package com.ben.csp.client.render.entity;

import com.ben.csp.CSPModClient;
import com.ben.csp.client.render.model.entity.UchetchikEntityModel;
import com.ben.csp.entity.UchetchikEntity;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class UchetchikEntityRenderer extends BipedEntityRenderer<UchetchikEntity, UchetchikEntityModel<UchetchikEntity>> {
    public UchetchikEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new UchetchikEntityModel<>(context.getPart(CSPModClient.MODEL_UCHETCHIK_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(UchetchikEntity entity) {
        return new Identifier("minecraft", "textures/entity/villager/villager.png");
    }
}