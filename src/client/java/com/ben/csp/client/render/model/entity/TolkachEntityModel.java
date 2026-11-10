package com.ben.csp.client.render.model.entity;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;

/**
 * Biped (humanoid) model for the tolkach labourer. Without it the render dispatcher
 * returns nothing for a spawned tolkach and the client crashes, so this is not
 * decoration — it is what keeps the labourers on screen.
 */
public class TolkachEntityModel<T extends LivingEntity> extends BipedEntityModel<T> {

    public TolkachEntityModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
        return TexturedModelData.of(modelData, 64, 64);
    }
}
