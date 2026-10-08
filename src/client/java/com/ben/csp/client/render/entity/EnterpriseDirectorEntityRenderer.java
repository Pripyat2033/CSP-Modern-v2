package com.ben.csp.client.render.entity;

import com.ben.csp.CSPMod;
import com.ben.csp.client.render.model.ModModelLayers;
import com.ben.csp.client.render.model.entity.EnterpriseDirectorEntityModel;
import com.ben.csp.entity.EnterpriseDirectorEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class EnterpriseDirectorEntityRenderer extends MobEntityRenderer<EnterpriseDirectorEntity, EnterpriseDirectorEntityModel<EnterpriseDirectorEntity>> {
    private static final Identifier TEXTURE = new Identifier(CSPMod.MOD_ID, "textures/entity/enterprise_director.png");

    public EnterpriseDirectorEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new EnterpriseDirectorEntityModel<>(context.getPart(ModModelLayers.ENTERPRISE_DIRECTOR)), 0.5f);
    }

    @Override
    public Identifier getTexture(EnterpriseDirectorEntity entity) {
        return TEXTURE;
    }
}