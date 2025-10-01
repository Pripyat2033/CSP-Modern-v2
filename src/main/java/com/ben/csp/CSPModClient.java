package com.ben.csp;

import com.ben.csp.entity.ModEntities;
import com.ben.csp.entity.render.BargeEntityRenderer;
// import com.ben.csp.entity.render.GeodezistEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class CSPModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BARGE, BargeEntityRenderer::new);
        // EntityRendererRegistry.register(ModEntities.GEODEZIST, GeodezistEntityRenderer::new); // To be restored
    }
}