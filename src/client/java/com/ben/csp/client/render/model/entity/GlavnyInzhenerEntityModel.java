package com.ben.csp.client.render.model.entity;
import com.ben.csp.entity.*;
import com.ben.csp.entity.GlavnyInzhenerEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;

public class GlavnyInzhenerEntityModel extends VillagerResemblingModel<GlavnyInzhenerEntity> {
    public GlavnyInzhenerEntityModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = VillagerResemblingModel.getModelData();
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(GlavnyInzhenerEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
    }
}