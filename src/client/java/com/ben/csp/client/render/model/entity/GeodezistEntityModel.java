package com.ben.csp.client.render.model.entity;
import com.ben.csp.entity.*;
import com.ben.csp.entity.ModModelLayers;
import com.ben.csp.entity.GeodezistEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;

/**
 * Represents the model for a Geodezist entity.
 * This class is a standard Biped model, as the Geodezist is a humanoid NPC.
 */
public class GeodezistEntityModel extends BipedEntityModel<GeodezistEntity> {

    public static final EntityModelLayer LAYER_LOCATION = ModModelLayers.GEODEZIST;

    public GeodezistEntityModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
        return TexturedModelData.of(modelData, 64, 64);
    }
}