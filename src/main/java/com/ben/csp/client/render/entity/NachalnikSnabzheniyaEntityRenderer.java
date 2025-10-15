package com.ben.csp.client.render.entity;

import com.ben.csp.CSPModClient;
import com.ben.csp.client.render.model.entity.NachalnikSnabzheniyaEntityModel;
import com.ben.csp.entity.NachalnikSnabzheniyaEntity;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class NachalnikSnabzheniyaEntityRenderer extends BipedEntityRenderer<NachalnikSnabzheniyaEntity, NachalnikSnabzheniyaEntityModel<NachalnikSnabzheniyaEntity>> {
    public NachalnikSnabzheniyaEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new NachalnikSnabzheniyaEntityModel<>(context.getPart(CSPModClient.MODEL_NACHALNIK_SNABZHENIYA_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(NachalnikSnabzheniyaEntity entity) {
        return new Identifier("minecraft", "textures/entity/villager/villager.png");
    }
}