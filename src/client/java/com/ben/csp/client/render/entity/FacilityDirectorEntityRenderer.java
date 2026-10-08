package com.ben.csp.client.render.entity;

import com.ben.csp.CSPMod;
import com.ben.csp.client.render.model.ModModelLayers;
import com.ben.csp.client.render.model.entity.FacilityDirectorEntityModel;
import com.ben.csp.entity.FacilityDirectorEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class FacilityDirectorEntityRenderer extends MobEntityRenderer<FacilityDirectorEntity, FacilityDirectorEntityModel<FacilityDirectorEntity>> {
    private static final Identifier TEXTURE = new Identifier(CSPMod.MOD_ID, "textures/entity/facility_director.png");

    public FacilityDirectorEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new FacilityDirectorEntityModel<>(context.getPart(ModModelLayers.FACILITY_DIRECTOR)), 0.5f);
    }

    @Override
    public Identifier getTexture(FacilityDirectorEntity entity) {
        return TEXTURE;
    }
}