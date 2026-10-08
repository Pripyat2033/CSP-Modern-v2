package com.ben.csp.client.render.entity;

import com.ben.csp.CSPMod;
import com.ben.csp.client.render.model.ModModelLayers;
import com.ben.csp.client.render.model.entity.SekretarEntityModel;
import com.ben.csp.entity.SekretarEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class SekretarEntityRenderer extends MobEntityRenderer<SekretarEntity, SekretarEntityModel> {
    private static final Identifier TEXTURE = new Identifier(CSPMod.MOD_ID, "textures/entity/sekretar.png");

    public SekretarEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new SekretarEntityModel(context.getPart(ModModelLayers.SEKRETAR)), 0.5f);
    }

    @Override
    public Identifier getTexture(SekretarEntity entity) {
        return TEXTURE;
    }
}