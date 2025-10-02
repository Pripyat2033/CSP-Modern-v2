package com.ben.csp;

import com.ben.csp.block.ModBlocks;
import com.ben.csp.entity.model.BargeEntityModel;
import com.ben.csp.block.model.ArchiveFilingCabinetBlockEntityModel;
import com.ben.csp.block.render.ArchiveFilingCabinetBlockEntityRenderer;
import com.ben.csp.block.model.VertushkaBlockEntityModel;
import com.ben.csp.block.render.VertushkaBlockEntityRenderer;
import com.ben.csp.entity.ModEntities;
import com.ben.csp.entity.model.MasterEntityModel;
import com.ben.csp.entity.model.UchetchikEntityModel;
import com.ben.csp.entity.model.NachalnikSnabzheniyaEntityModel;
import com.ben.csp.entity.model.BrigadierEntityModel;
import com.ben.csp.entity.model.StroitelEntityModel;
import com.ben.csp.entity.model.ProrabEntityModel;
import com.ben.csp.entity.model.GeodezistEntityModel;
import com.ben.csp.entity.render.BargeEntityRenderer;
import com.ben.csp.entity.render.UchetchikEntityRenderer;
import com.ben.csp.entity.render.BrigadierEntityRenderer;
import com.ben.csp.entity.render.StroitelEntityRenderer;
import com.ben.csp.entity.render.MasterEntityRenderer;
import com.ben.csp.entity.render.ProrabEntityRenderer;
import com.ben.csp.entity.render.NachalnikSnabzheniyaEntityRenderer;
import com.ben.csp.entity.render.GeodezistEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class CSPModClient implements ClientModInitializer {
    // Entity Model Layers
    public static final EntityModelLayer MODEL_GEODEZIST_LAYER = new EntityModelLayer(new Identifier(CSPMod.MOD_ID, "geodezist"), "main");
    public static final EntityModelLayer MODEL_BARGE_LAYER = new EntityModelLayer(new Identifier(CSPMod.MOD_ID, "barge"), "main");
    public static final EntityModelLayer MODEL_PRORAB_LAYER = new EntityModelLayer(new Identifier(CSPMod.MOD_ID, "prorab"), "main");
    public static final EntityModelLayer MODEL_MASTER_LAYER = new EntityModelLayer(new Identifier(CSPMod.MOD_ID, "master"), "main");
    public static final EntityModelLayer MODEL_STROITEL_LAYER = new EntityModelLayer(new Identifier(CSPMod.MOD_ID, "stroitel"), "main");
    public static final EntityModelLayer MODEL_BRIGADIER_LAYER = new EntityModelLayer(new Identifier(CSPMod.MOD_ID, "brigadier"), "main");
    public static final EntityModelLayer MODEL_NACHALNIK_SNABZHENIYA_LAYER = new EntityModelLayer(new Identifier(CSPMod.MOD_ID, "nachalnik_snabzheniya"), "main");
    public static final EntityModelLayer MODEL_UCHETCHIK_LAYER = new EntityModelLayer(new Identifier(CSPMod.MOD_ID, "uchetchik"), "main");

    // Block Entity Model Layers
    public static final EntityModelLayer MODEL_VERTUSHKA_LAYER = new EntityModelLayer(new Identifier(CSPMod.MOD_ID, "vertushka"), "main");
    public static final EntityModelLayer MODEL_ARCHIVE_FILING_CABINET_LAYER = new EntityModelLayer(new Identifier(CSPMod.MOD_ID, "archive_filing_cabinet"), "main");

    @Override
    public void onInitializeClient() {
        // Entity Renderers
        EntityRendererRegistry.register(ModEntities.BARGE, BargeEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GEODEZIST, GeodezistEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.PRORAB, ProrabEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MASTER, MasterEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.STROITEL, StroitelEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BRIGADIER, BrigadierEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NACHALNIK_SNABZHENIYA, NachalnikSnabzheniyaEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.UCHETCHIK, UchetchikEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(MODEL_BARGE_LAYER, BargeEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(MODEL_GEODEZIST_LAYER, GeodezistEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(MODEL_PRORAB_LAYER, BipedEntityModel::getModelData);
        EntityModelLayerRegistry.registerModelLayer(MODEL_MASTER_LAYER, BipedEntityModel::getModelData);
        EntityModelLayerRegistry.registerModelLayer(MODEL_STROITEL_LAYER, BipedEntityModel::getModelData);
        EntityModelLayerRegistry.registerModelLayer(MODEL_BRIGADIER_LAYER, BipedEntityModel::getModelData);
        EntityModelLayerRegistry.registerModelLayer(MODEL_NACHALNIK_SNABZHENIYA_LAYER, BipedEntityModel::getModelData);
        EntityModelLayerRegistry.registerModelLayer(MODEL_UCHETCHIK_LAYER, BipedEntityModel::getModelData);

        // Block Entity Renderers
        BlockEntityRendererRegistry.register(ModBlocks.VERTUSHKA_BLOCK_ENTITY, VertushkaBlockEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(MODEL_VERTUSHKA_LAYER, VertushkaBlockEntityModel::getTexturedModelData);

        BlockEntityRendererRegistry.register(ModBlocks.ARCHIVE_FILING_CABINET_BLOCK_ENTITY, ArchiveFilingCabinetBlockEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(MODEL_ARCHIVE_FILING_CABINET_LAYER, ArchiveFilingCabinetBlockEntityModel::getTexturedModelData);
    }
}