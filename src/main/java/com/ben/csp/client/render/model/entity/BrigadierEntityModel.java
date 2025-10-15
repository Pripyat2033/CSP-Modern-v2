package com.ben.csp.client.render.model.entity;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;

public class BrigadierEntityModel<T extends LivingEntity> extends BipedEntityModel<T> {
    public BrigadierEntityModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(new net.minecraft.client.model.Dilation(0.0F), 0.0F);
		modelData.getRoot();
        return TexturedModelData.of(modelData, 64, 64);
    }
}