package com.ben.csp.client.render.entity;

import com.ben.csp.CSPModClient;
import com.ben.csp.client.render.model.entity.GeodezistEntityModel;
import com.ben.csp.entity.GeodezistEntity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class GeodezistEntityRenderer extends MobEntityRenderer<GeodezistEntity, GeodezistEntityModel<GeodezistEntity>> {
    public GeodezistEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new GeodezistEntityModel<>(context.getPart(CSPModClient.MODEL_GEODEZIST_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(GeodezistEntity entity) {
        return new Identifier("minecraft", "textures/entity/villager/villager.png");
    }
}