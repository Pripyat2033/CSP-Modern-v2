package com.ben.csp.block.entity;

import com.ben.csp.skala.SkalaCoreBlockEntity;
import com.ben.csp.skala.SkalaRequestDeviceBlockEntity;
import com.ben.csp.skala.SkalaTapeDriveBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Central registry of every block entity type in the mod.
 */
public class ModBlockEntities {

    // Aliases so the client initialiser can refer to each type by a stable name.
    public static final BlockEntityType<ChiseledBlockEntity> CHISELED_BLOCK_ENTITY =
        ChiseledBlockEntity.CHISELED_BLOCK_ENTITY_TYPE;
    public static final BlockEntityType<GraphiteBlockEntity> GRAPHITE_BLOCK_ENTITY =
        GraphiteBlockEntity.GRAPHITE_BLOCK_ENTITY_TYPE;
    public static final BlockEntityType<VertushkaBlockEntity> VERTUSHKA_BLOCK_ENTITY =
        VertushkaBlockEntity.VERTUSHKA_BLOCK_ENTITY_TYPE;
    public static final BlockEntityType<IndustrialControlBlockEntity> INDUSTRIAL_CONTROL_BLOCK_ENTITY =
        IndustrialControlBlockEntity.INDUSTRIAL_CONTROL_BLOCK_ENTITY_TYPE;
    public static final BlockEntityType<MtkDisplayBlockEntity> MTK_DISPLAY_BLOCK_ENTITY =
        MtkDisplayBlockEntity.MTK_DISPLAY_BLOCK_ENTITY_TYPE;
    public static final BlockEntityType<IcmsBlockEntity> ICMS_BLOCK_ENTITY =
        IcmsBlockEntity.ICMS_BLOCK_ENTITY_TYPE;
    public static final BlockEntityType<IcmsPanelBlockEntity> ICMS_PANEL_BLOCK_ENTITY =
        IcmsPanelBlockEntity.ICMSPANEL_BLOCK_ENTITY_TYPE;

    public static final BlockEntityType<PlanshetBlockEntity> PLANSHET_BLOCK_ENTITY =
        PlanshetBlockEntity.PLANSHET_BLOCK_ENTITY_TYPE;
    public static final BlockEntityType<ArchiveFilingCabinetBlockEntity> ARCHIVE_FILING_CABINET_BLOCK_ENTITY =
        ArchiveFilingCabinetBlockEntity.ARCHIVE_FILING_CABINET_BLOCK_ENTITY_TYPE;
    public static final BlockEntityType<SkalaCoreBlockEntity> SKALA_CORE_BLOCK_ENTITY =
        SkalaCoreBlockEntity.SKALA_CORE_BLOCK_ENTITY_TYPE;
    public static final BlockEntityType<SkalaTapeDriveBlockEntity> SKALA_TAPE_DRIVE_BLOCK_ENTITY =
        SkalaTapeDriveBlockEntity.SKALA_TAPE_DRIVE_BLOCK_ENTITY_TYPE;
    public static final BlockEntityType<SkalaRequestDeviceBlockEntity> SKALA_REQUEST_DEVICE_BLOCK_ENTITY =
        SkalaRequestDeviceBlockEntity.SKALA_REQUEST_DEVICE_BLOCK_ENTITY_TYPE;

    public static void registerAll() {
        reg("chiseled_block_entity", CHISELED_BLOCK_ENTITY);
        reg("graphite_block_entity", GRAPHITE_BLOCK_ENTITY);
        reg("vertushka_block_entity", VERTUSHKA_BLOCK_ENTITY);
        reg("industrial_control_block_entity", INDUSTRIAL_CONTROL_BLOCK_ENTITY);
        reg("mtk_display_block_entity", MTK_DISPLAY_BLOCK_ENTITY);
        reg("icms_block_entity", ICMS_BLOCK_ENTITY);
        reg("icms_panel_block_entity", ICMS_PANEL_BLOCK_ENTITY);
        reg("planshet_block_entity", PLANSHET_BLOCK_ENTITY);
        reg("archive_filing_cabinet_block_entity", ARCHIVE_FILING_CABINET_BLOCK_ENTITY);
        reg("skala_core_block_entity", SKALA_CORE_BLOCK_ENTITY);
        reg("skala_tape_drive_block_entity", SKALA_TAPE_DRIVE_BLOCK_ENTITY);
        reg("skala_request_device_block_entity", SKALA_REQUEST_DEVICE_BLOCK_ENTITY);
    }

    private static void reg(String name, BlockEntityType<?> type) {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("csp-modern", name), type);
    }
}
