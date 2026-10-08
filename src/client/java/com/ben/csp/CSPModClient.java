package com.ben.csp;

import com.ben.csp.client.render.model.block.VertushkaBlockModel;
import com.ben.csp.client.render.model.block.ArchiveFilingCabinetBlockEntityModel;
import com.ben.csp.client.render.block.GraphiteBlockEntityRenderer;
import com.ben.csp.client.render.model.block.SkalaTapeDriveBlockEntityModel;
import com.ben.csp.client.render.block.*;

import com.ben.csp.block.entity.ModBlockEntities;
import com.ben.csp.client.render.entity.*;
import com.ben.csp.client.render.model.entity.*;
import com.ben.csp.entity.*;
import com.ben.csp.networking.ModMessages;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class CSPModClient implements ClientModInitializer {

    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        // Entity Renderers
        EntityRendererRegistry.register(ModEntities.BARGE, BargeEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BRIGADIER, BrigadierEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GEODEZIST, GeodezistEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MASTER, MasterEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.PRORAB, ProrabEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.STROITEL, StroitelEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NACHALNIK_SNABZHENIYA, NachalnikSnabzheniyaEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.UCHETCHIK, UchetchikEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ENTERPRISE_DIRECTOR, EnterpriseDirectorEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.FACILITY_DIRECTOR, FacilityDirectorEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SEKRETAR, SekretarEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GLAVNY_INZHENER, GlavnyInzhenerEntityRenderer::new);

        // Entity Model Layers - This connects the model definition to the renderer.
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.GEODEZIST, GeodezistEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.BARGE, BargeEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.BRIGADIER, BrigadierEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.MASTER, MasterEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PRORAB, ProrabEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.STROITEL, StroitelEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.NACHALNIK_SNABZHENIYA, NachalnikSnabzheniyaEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.UCHETCHIK, UchetchikEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.ENTERPRISE_DIRECTOR, EnterpriseDirectorEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SEKRETAR, SekretarEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.FACILITY_DIRECTOR, FacilityDirectorEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.GLAVNY_INZHENER, GlavnyInzhenerEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.VERTUSHKA, VertushkaBlockModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.ARCHIVE_FILING_CABINET, ArchiveFilingCabinetBlockEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SKALA_TAPE_DRIVE, SkalaTapeDriveBlockEntityModel::getTexturedModelData);

        registerBlockEntityRenderers();
        
        ModMessages.registerS2CPackets();
    }

    private void registerBlockEntityRenderers() {
        BlockEntityRendererFactories.register(ModBlockEntities.PLANSHET_BLOCK_ENTITY, PlanshetBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.VERTUSHKA_BLOCK_ENTITY, VertushkaBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.ARCHIVE_FILING_CABINET_BLOCK_ENTITY, ArchiveFilingCabinetBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.CHISELED_BLOCK_ENTITY, ChiseledBlockEntityRenderer::new);
        // Register the renderer for our new MTK display block.
        BlockEntityRendererFactories.register(ModBlockEntities.MTK_DISPLAY_BLOCK_ENTITY, MtkDisplayBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.SKALA_CORE_BLOCK_ENTITY, SkalaCoreBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.SKALA_TAPE_DRIVE_BLOCK_ENTITY, SkalaTapeDriveBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.GRAPHITE_BLOCK_ENTITY, GraphiteBlockEntityRenderer::new);
    }
}