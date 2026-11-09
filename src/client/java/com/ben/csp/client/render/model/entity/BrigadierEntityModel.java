package com.ben.csp.client.render.model.entity;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;

/**
 * Biped (humanoid) model for the brigadier worker. Replaces the villager model so
 * the role can wear a proper 1986 Soviet work uniform skin.
 */
public class BrigadierEntityModel<T extends LivingEntity> extends BipedEntityModel<T> {

    public BrigadierEntityModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
        return TexturedModelData.of(modelData, 64, 64);
    }
}
