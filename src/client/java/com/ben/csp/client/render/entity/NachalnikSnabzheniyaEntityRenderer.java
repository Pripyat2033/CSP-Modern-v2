package com.ben.csp.client.render.entity;

import com.ben.csp.client.render.model.ModModelLayers;
import com.ben.csp.client.render.model.entity.NachalnikSnabzheniyaEntityModel;
import com.ben.csp.entity.NachalnikSnabzheniyaEntity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class NachalnikSnabzheniyaEntityRenderer extends MobEntityRenderer<NachalnikSnabzheniyaEntity, NachalnikSnabzheniyaEntityModel<NachalnikSnabzheniyaEntity>> {
    public NachalnikSnabzheniyaEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new NachalnikSnabzheniyaEntityModel<>(context.getPart(ModModelLayers.NACHALNIK_SNABZHENIYA)), 0.5f);
    }

    @Override
    public Identifier getTexture(NachalnikSnabzheniyaEntity entity) {
        return new Identifier("minecraft", "textures/entity/villager/villager.png");
    }
}