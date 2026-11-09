package com.ben.csp.client.render.entity;

import com.ben.csp.CSPMod;
import com.ben.csp.client.render.model.ModModelLayers;
import com.ben.csp.client.render.model.entity.KommutatorOperatorEntityModel;
import com.ben.csp.entity.KommutatorOperatorEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class KommutatorOperatorEntityRenderer
        extends MobEntityRenderer<KommutatorOperatorEntity, KommutatorOperatorEntityModel> {

    private static final Identifier TEXTURE = new Identifier(CSPMod.MOD_ID, "textures/entity/kommutator.png");

    public KommutatorOperatorEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new KommutatorOperatorEntityModel(context.getPart(ModModelLayers.KOMMUTATOR)), 0.5f);
    }

    @Override
    public Identifier getTexture(KommutatorOperatorEntity entity) {
        return TEXTURE;
    }
}
