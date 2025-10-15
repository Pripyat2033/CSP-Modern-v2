package com.ben.csp;

import com.ben.csp.block.ModBlocks;
import com.ben.csp.client.render.block.ArchiveFilingCabinetBlockEntityRenderer;
import com.ben.csp.client.render.block.VertushkaBlockEntityRenderer;
import com.ben.csp.client.render.entity.BargeEntityRenderer;
import com.ben.csp.client.render.entity.BrigadierEntityRenderer;
import com.ben.csp.client.render.entity.GeodezistEntityRenderer;
import com.ben.csp.client.render.entity.MasterEntityRenderer;
import com.ben.csp.client.render.entity.NachalnikSnabzheniyaEntityRenderer;
import com.ben.csp.client.render.entity.ProrabEntityRenderer;
import com.ben.csp.client.render.entity.StroitelEntityRenderer;
import com.ben.csp.client.render.entity.UchetchikEntityRenderer;
import com.ben.csp.client.render.model.block.ArchiveFilingCabinetBlockEntityModel;
import com.ben.csp.client.render.model.block.VertushkaBlockEntityModel;
import com.ben.csp.client.render.model.entity.BargeEntityModel;
import com.ben.csp.client.render.model.entity.BrigadierEntityModel;
import com.ben.csp.client.render.model.entity.GeodezistEntityModel;
import com.ben.csp.client.render.model.entity.MasterEntityModel;
import com.ben.csp.client.render.model.entity.NachalnikSnabzheniyaEntityModel;
import com.ben.csp.client.render.model.entity.ProrabEntityModel;
import com.ben.csp.client.render.model.entity.StroitelEntityModel;
import com.ben.csp.client.render.model.entity.UchetchikEntityModel;
import com.ben.csp.entity.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
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
    @SuppressWarnings("deprecation")
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
        EntityModelLayerRegistry.registerModelLayer(MODEL_GEODEZIST_LAYER, GeodezistEntityModel::getTexturedModelData); // Custom model
        EntityModelLayerRegistry.registerModelLayer(MODEL_PRORAB_LAYER, ProrabEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(MODEL_MASTER_LAYER, MasterEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(MODEL_STROITEL_LAYER, StroitelEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(MODEL_BRIGADIER_LAYER, BrigadierEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(MODEL_NACHALNIK_SNABZHENIYA_LAYER, NachalnikSnabzheniyaEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(MODEL_UCHETCHIK_LAYER, UchetchikEntityModel::getTexturedModelData);

        // Block Entity Renderers
        BlockEntityRendererRegistry.register(ModBlocks.VERTUSHKA_BLOCK_ENTITY, VertushkaBlockEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(MODEL_VERTUSHKA_LAYER, VertushkaBlockEntityModel::getTexturedModelData);

        BlockEntityRendererRegistry.register(ModBlocks.ARCHIVE_FILING_CABINET_BLOCK_ENTITY, ArchiveFilingCabinetBlockEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(MODEL_ARCHIVE_FILING_CABINET_LAYER, ArchiveFilingCabinetBlockEntityModel::getTexturedModelData);
    }
}