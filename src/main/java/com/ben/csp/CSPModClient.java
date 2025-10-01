package com.ben.csp;

import com.ben.csp.entity.ModEntities;
import com.ben.csp.entity.model.GeodezistEntityModel;
import com.ben.csp.entity.render.BargeEntityRenderer;
import com.ben.csp.entity.render.GeodezistEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class CSPModClient implements ClientModInitializer {
    public static final EntityModelLayer MODEL_GEODEZIST_LAYER = new EntityModelLayer(new Identifier(CSPMod.MOD_ID, "geodezist"), "main");

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BARGE, BargeEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GEODEZIST, GeodezistEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(MODEL_GEODEZIST_LAYER, GeodezistEntityModel::getTexturedModelData);
    }
}