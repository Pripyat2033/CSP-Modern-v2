package com.ben.csp.client.render.entity;

import com.ben.csp.client.render.model.ModModelLayers;
import com.ben.csp.client.render.model.entity.MasterEntityModel;
import com.ben.csp.entity.MasterEntity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class MasterEntityRenderer extends MobEntityRenderer<MasterEntity, MasterEntityModel<MasterEntity>> {
    public MasterEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new MasterEntityModel<>(context.getPart(ModModelLayers.MASTER)), 0.5f);
    }

    @Override
    public Identifier getTexture(MasterEntity entity) {
        return new Identifier("minecraft", "textures/entity/villager/villager.png");
    }
}