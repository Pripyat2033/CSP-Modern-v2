package com.ben.csp.client.render.entity;

import com.ben.csp.client.render.model.ModModelLayers;
import com.ben.csp.client.render.model.entity.TolkachEntityModel;
import com.ben.csp.entity.TolkachEntity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class TolkachEntityRenderer extends MobEntityRenderer<TolkachEntity, TolkachEntityModel<TolkachEntity>> {
    public TolkachEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new TolkachEntityModel<>(context.getPart(ModModelLayers.TOLKACH)), 0.5f);
    }

    @Override
    public Identifier getTexture(TolkachEntity entity) {
        return new Identifier("csp-modern", "textures/entity/tolkach.png");
    }
}
