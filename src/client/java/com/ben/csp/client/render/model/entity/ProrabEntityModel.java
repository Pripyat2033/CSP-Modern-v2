package com.ben.csp.client.render.model.entity;
import com.ben.csp.entity.*;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import com.ben.csp.entity.ProrabEntity;

public class ProrabEntityModel<T extends ProrabEntity> extends VillagerResemblingModel<T> {
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart arms;

    public ProrabEntityModel(ModelPart root) {
        super(root);
        // Get references to the private parts from the root model part
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.arms = root.getChild("arms");
    }



    public static TexturedModelData getTexturedModelData() {
        // Use the standard VillagerResemblingModel data. This is the correct approach.
        ModelData modelData = VillagerResemblingModel.getModelData();
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);

        if (entity.isSitting()) {
            // Apply sitting pose
            this.getHead().pitch = 0.0F; // Keep head level
            this.rightLeg.pitch = -1.5707964F; // -90 degrees
            this.rightLeg.yaw = 0.31415927F; // 18 degrees
            this.leftLeg.pitch = -1.5707964F; // -90 degrees
            this.leftLeg.yaw = -0.31415927F; // -18 degrees
            this.arms.pitch = -0.8F;
        }
    }
}