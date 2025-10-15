package com.ben.csp.client.render.entity;

import com.ben.csp.CSPModClient;
import com.ben.csp.client.render.model.entity.BrigadierEntityModel;
import com.ben.csp.entity.BrigadierEntity;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class BrigadierEntityRenderer extends BipedEntityRenderer<BrigadierEntity, BrigadierEntityModel<BrigadierEntity>> {
    public BrigadierEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new BrigadierEntityModel<BrigadierEntity>(context.getPart(CSPModClient.MODEL_BRIGADIER_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(BrigadierEntity entity) {
        return new Identifier("minecraft", "textures/entity/villager/villager.png");
    }
}