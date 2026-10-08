package com.ben.csp.client.render.model.entity;
import com.ben.csp.entity.*;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;

public class BargeEntityModel<T extends Entity> extends SinglePartEntityModel<T> {
    private final ModelPart root;

    public BargeEntityModel(ModelPart root) {
        this.root = root;
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        modelData.getRoot().addChild("main", ModelPartBuilder.create().uv(0, 0).cuboid(-16.0F, -4.0F, -32.0F, 32.0F, 8.0F, 64.0F), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 128, 64);
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }
}