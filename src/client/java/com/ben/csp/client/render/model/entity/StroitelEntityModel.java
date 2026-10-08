package com.ben.csp.client.render.model.entity;
import com.ben.csp.entity.*;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.entity.LivingEntity;

public class StroitelEntityModel<T extends LivingEntity> extends VillagerResemblingModel<T> {
    public StroitelEntityModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        // Use the standard VillagerResemblingModel data. This is the correct approach.
        ModelData modelData = VillagerResemblingModel.getModelData();
        return TexturedModelData.of(modelData, 64, 64);
    }
}