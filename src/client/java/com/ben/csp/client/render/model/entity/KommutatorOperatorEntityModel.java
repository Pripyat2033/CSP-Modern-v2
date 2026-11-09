package com.ben.csp.client.render.model.entity;

import com.ben.csp.client.render.model.ModModelLayers;
import com.ben.csp.entity.KommutatorOperatorEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;

/**
 * Biped model for the Kommutator (switchboard operator).
 */
public class KommutatorOperatorEntityModel extends BipedEntityModel<KommutatorOperatorEntity> {

    public KommutatorOperatorEntityModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
        return TexturedModelData.of(modelData, 64, 64);
    }
}
